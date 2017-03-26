/** $lic$
 * Copyright (C) 2012-2015 by Massachusetts Institute of Technology
 * Copyright (C) 2010-2013 by The Board of Trustees of Stanford University
 *
 * This file is part of zsim.
 *
 * zsim is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * If you use this software in your research, we request that you reference
 * the zsim paper ("ZSim: Fast and Accurate Microarchitectural Simulation of
 * Thousand-Core Systems", Sanchez and Kozyrakis, ISCA-40, June 2013) as the
 * source of the simulator in any publications that use this software, and that
 * you send us a citation of your work.
 *
 * zsim is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef FILTER_CACHE_H_
#define FILTER_CACHE_H_

#include "bithacks.h"
#include "cache.h"
#include "galloc.h"
#include "zsim.h"
#include "clu_stats.h"
#include "ma_prof.h"

/* Extends Cache with an L0 direct-mapped cache, optimized to hell for hits
 *
 * L1 lookups are dominated by several kinds of overhead (grab the cache locks,
 * several virtual functions for the replacement policy, etc.). This
 * specialization of Cache solves these issues by having a filter array that
 * holds the most recently used line in each set. Accesses check the filter array,
 * and then go through the normal access path. Because there is one line per set,
 * it is fine to do this without grabbing a lock.
 */

class FilterCache : public Cache {
    private:
        struct FilterEntry {
            volatile Address rdAddr;
            volatile Address wrAddr;
            volatile uint64_t availCycle;

#ifdef CLU_STATS_ENABLED
            volatile CacheLineAccessMask_t accessMask;
#endif
            void clear() {
                wrAddr = UNDEF_CACHE_LINE_ADDRESS; rdAddr = UNDEF_CACHE_LINE_ADDRESS; availCycle = 0;
#ifdef CLU_STATS_ENABLED
                accessMask = CLU_STATS_ZERO_MASK;
#endif
            }
        };

        //Replicates the most accessed line of each set in the cache
        FilterEntry* filterArray;
        Address setMask;
        uint32_t numSets;
        uint32_t srcId; //should match the core
        uint32_t reqFlags;

        lock_t filterLock;
        uint64_t fGETSHit, fGETXHit;
#ifdef CLU_STATS_ENABLED
        uint64_t fCLEI;
        uint64_t fUCLC;
#endif

    public:
        FilterCache(uint32_t _numSets, uint32_t _numLines, CC* _cc, CacheArray* _array,
                ReplPolicy* _rp, uint32_t _accLat, uint32_t _invLat, g_string& _name)
            : Cache(_numLines, _cc, _array, _rp, _accLat, _invLat, _name)
        {
            numSets = _numSets;
            setMask = numSets - 1;
            filterArray = gm_memalign<FilterEntry>(CACHE_LINE_BYTES, numSets);
            for (uint32_t i = 0; i < numSets; i++) filterArray[i].clear();
            futex_init(&filterLock);
            fGETSHit = fGETXHit = 0;
#ifdef CLU_STATS_ENABLED
            fCLEI = 0;
            fUCLC = 0;
#endif
            srcId = -1;
            reqFlags = 0;
        }

        void setSourceId(uint32_t id) {
            srcId = id;
        }

        void setFlags(uint32_t flags) {
            reqFlags = flags;
        }

        void initStats(AggregateStat* parentStat) {
            AggregateStat* cacheStat = new AggregateStat();
            cacheStat->init(name.c_str(), "Filter cache stats");

            ProxyStat* fgetsStat = new ProxyStat();
            fgetsStat->init("fhGETS", "Filtered GETS hits", &fGETSHit);
            ProxyStat* fgetxStat = new ProxyStat();
            fgetxStat->init("fhGETX", "Filtered GETX hits", &fGETXHit);
#ifdef CLU_STATS_ENABLED
            ProxyStat* fCLEIStat = new ProxyStat();
            fCLEIStat->init("fCLEI", "Filtered cache line evictions and invalidations", &fCLEI);
            ProxyStat*fUCLCStat = new ProxyStat();
            fUCLCStat->init("fUCLC", "Filtered utilized cache line chunks", &fUCLC);
#endif
            cacheStat->append(fgetsStat);
            cacheStat->append(fgetxStat);
#ifdef CLU_STATS_ENABLED
            cacheStat->append(fCLEIStat);
            cacheStat->append(fUCLCStat);
#endif

            initCacheStats(cacheStat);
            parentStat->append(cacheStat);
        }

#ifdef CLU_STATS_ENABLED
        inline void processAccessCLUStats(Address vAddr, MASize_t size, MemReqStatType_t memReqStatType, uint32_t lineIndex) {
            filterArray[lineIndex].accessMask |= cluStatsGetUtilizationMask(vAddr, size, memReqStatType);
        }
#endif

        inline uint64_t load(Address vAddr, uint64_t curCycle
#ifdef CLU_STATS_ENABLED
                             , MASize_t size, MemReqStatType_t memReqStatType
#endif
#ifdef MA_PROF_ENABLED
                             , PointerTag_t tag, MAOffset_t offset, Address bblIP
#endif
                             ) {
            Address vLineAddr = vAddr >> lineBits;
            uint32_t idx = vLineAddr & setMask;
            uint64_t availCycle = filterArray[idx].availCycle; //read before, careful with ordering to avoid timing races
            if (vLineAddr == filterArray[idx].rdAddr) {
#ifdef CLU_STATS_ENABLED
                processAccessCLUStats(vAddr, size, memReqStatType, idx);
#endif
                fGETSHit++;
                return MAX(curCycle, availCycle);
            } else {
                return replace(vLineAddr, idx, true, curCycle, vAddr
#ifdef CLU_STATS_ENABLED
                               , size, memReqStatType
#endif
#ifdef MA_PROF_ENABLED
                               , tag, offset, bblIP
#endif
                               );
            }
        }

        inline uint64_t store(Address vAddr, uint64_t curCycle
#ifdef CLU_STATS_ENABLED
                              , MASize_t size
#endif
#ifdef MA_PROF_ENABLED
                              , PointerTag_t tag, MAOffset_t offset, Address bblIP
#endif
                              ) {
            Address vLineAddr = vAddr >> lineBits;
            uint32_t idx = vLineAddr & setMask;
            uint64_t availCycle = filterArray[idx].availCycle; //read before, careful with ordering to avoid timing races
            if (vLineAddr == filterArray[idx].wrAddr) {
#ifdef CLU_STATS_ENABLED
                processAccessCLUStats(vAddr, size, StoreData, idx);
#endif
                fGETXHit++;
                //NOTE: Stores don't modify availCycle; we'll catch matches in the core
                //filterArray[idx].availCycle = curCycle; //do optimistic store-load forwarding
                return MAX(curCycle, availCycle);
            } else {
                return replace(vLineAddr, idx, false, curCycle, vAddr
#ifdef CLU_STATS_ENABLED
                               , size, StoreData
#endif
#ifdef MA_PROF_ENABLED
                               , tag, offset, bblIP
#endif
                               );
            }
        }

        uint64_t replace(Address vLineAddr, uint32_t idx, bool isLoad, uint64_t curCycle, Address vAddr
#ifdef CLU_STATS_ENABLED
                         , MASize_t size, MemReqStatType_t memReqStatType
#endif
#ifdef MA_PROF_ENABLED
                         , PointerTag_t tag, MAOffset_t offset, Address bblIP
#endif
                         ) {
            Address pLineAddr = procMask | vLineAddr;
            MESIState dummyState = MESIState::I;
            futex_lock(&filterLock);
            MemReq req = {pLineAddr, isLoad? GETS : GETX, 0, &dummyState, curCycle, &filterLock, dummyState, srcId, reqFlags
#ifdef CLU_STATS_ENABLED
                    , {vAddr, size, memReqStatType, filterArray[idx].rdAddr, filterArray[idx].accessMask}
#endif
#ifdef MA_PROF_ENABLED
                    , {tag, offset, bblIP}
#endif
                };
            uint64_t respCycle  = access(req);

            //Due to the way we do the locking, at this point the old address might be invalidated, but we have the new address guaranteed until we release the lock

            //Careful with this order
            Address oldAddr = filterArray[idx].rdAddr;
            filterArray[idx].wrAddr = isLoad? -1L : vLineAddr;
            filterArray[idx].rdAddr = vLineAddr;
#ifdef CLU_STATS_ENABLED
            if (oldAddr != UNDEF_CACHE_LINE_ADDRESS) {
                fCLEI++;
                fUCLC += __builtin_popcount(filterArray[idx].accessMask);
                filterArray[idx].accessMask = CLU_STATS_ZERO_MASK;
            }
            processAccessCLUStats(vAddr, size, memReqStatType, idx);
#endif

            //For LSU simulation purposes, loads bypass stores even to the same line if there is no conflict,
            //(e.g., st to x, ld from x+8) and we implement store-load forwarding at the core.
            //So if this is a load, it always sets availCycle; if it is a store hit, it doesn't
            if (oldAddr != vLineAddr) filterArray[idx].availCycle = respCycle;

            futex_unlock(&filterLock);
            return respCycle;
        }

        uint64_t invalidate(const InvReq& req) {
            Cache::startInvalidate();  // grabs cache's downLock
            futex_lock(&filterLock);
            uint32_t idx = req.lineAddr & setMask; //works because of how virtual<->physical is done...
            if ((filterArray[idx].rdAddr | procMask) == req.lineAddr) { //FIXME: If another process calls invalidate(), procMask will not match even though we may be doing a capacity-induced invalidation!
#ifdef CLU_STATS_ENABLED
                assert(filterArray[idx].rdAddr != UNDEF_CACHE_LINE_ADDRESS);
#endif
                filterArray[idx].wrAddr = -1L;
                filterArray[idx].rdAddr = -1L;
#ifdef CLU_STATS_ENABLED
                fCLEI++;
                fUCLC += __builtin_popcount(filterArray[idx].accessMask);
                filterArray[idx].accessMask = CLU_STATS_ZERO_MASK;
#endif
            }
            uint64_t respCycle = Cache::finishInvalidate(req); // releases cache's downLock
            futex_unlock(&filterLock);
            return respCycle;
        }

        void contextSwitch() {
            futex_lock(&filterLock);
            for (uint32_t i = 0; i < numSets; i++) filterArray[i].clear();
            futex_unlock(&filterLock);
        }
};

#endif  // FILTER_CACHE_H_
