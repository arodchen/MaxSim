/** $lic$
 * Copyright (C) 2017 by Andrey Rodchenko, School of Computer Science,
 * The University of Manchester
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

#ifndef MAXINE_STATISTICS_H_
#define MAXINE_STATISTICS_H_

#include "zsim.h"
#include "ma_prof.h"

#if defined(MA_PROF_ENABLED) && defined(MAXSIM_ENABLED)

#include "maxsim/maxsim_runtime_info.h"
#include "maxsim/maxsim_interface_helpers.h"
#include "maxsim/maxsim_interface_c.h"
#include "pad.h"
#include "locks.h"

class MaxSimProfiling {
    public:
        // Adds memory access
        //
        void addMemoryAccess(PointerTag_t tag, MAOffset_t offset, Address bblIP, bool isWrite);

        // Adds cache miss
        //
        void addCacheMiss(PointerTag_t tag, MAOffset_t offset, Address bblIP, bool isWrite, MAProfCacheGroupId_t cacheGroupId, int missCount);

        // Serializes to file
        //
        void serializeToFile();

        // Indicates where DB is empty
        //
        bool isEmpty() {
            return zsimProfDB.classprof_size() == 0;
        }

        // Indicates whether profile collection is enabled
        //
        bool isProfileCollectionEnabled() {
            return isProfileCollectionEnabledIndicator;
        }

        // Enables profile collection
        //
        void enableProfileCollection(int b) {
            zsimProfDB.set_dumpeventualstatsbeg(b);
            isProfileCollectionEnabledIndicator = true;
        }

        // Disables profile collection
        //
        void disableProfileCollection(int e) {
            zsimProfDB.set_dumpeventualstatsend(e);
            isProfileCollectionEnabledIndicator = false;
        }

        // Resets profile collection
        //
        void resetProfileCollection();

        // Profiles object allocation
        //
        void profileObjectAllocation(PointerTag_t tag, uint16_t tagType, MASize_t size, ThreadId_t id);

        // Gets allocation site id estimation
        //
        PointerTag_t getAllocationSiteEstimationID(PointerTag_t tag, ThreadId_t tid);

        // Sets profile file name
        //
        void setProfilingFileName(char * profileFileName) {
            profFileName.assign(profileFileName);
        }

    private:
        typedef std::pair<PointerTag_t, MASize_t> ClassIdSizePair_t;

        typedef std::pair<PointerTag_t, MAOffset_t> ClassIdOffsetPair_t;

        typedef std::pair<PointerTag_t, MAProfCacheRWGroupId_t> ClassIdCacheRWIdPair_t;

        typedef std::pair<Address, PointerTag_t> AllocationSiteIPClassIdPair_t;

        typedef std::pair<ClassIdCacheRWIdPair_t, Address> ClassIdCacheAccessIPTriplet_t;

        typedef std::map<ClassIdOffsetPair_t, FieldProf *> ClassIdOffsetPairToFieldEntryMap_t;

        typedef std::map<PointerTag_t, ClassProf *> ClassIdOffsetPairToClassEntryMap_t;

        typedef std::map<ClassIdSizePair_t, AllocProf *> ClassIdSizePairToMemoryAllocationEntryMap_t;

        typedef std::map<ClassIdCacheAccessIPTriplet_t, CacheMissProf *> ClassIdCacheAccessIPTripletToCacheMissEntryMap_t;

        typedef std::map<AllocationSiteIPClassIdPair_t, AllocSiteProf *> AllocationSiteIPClassIDPairToAllocationEntryMap_t;

        // Map to support unique field entries in serializable statistics data base
        //
        ClassIdOffsetPairToFieldEntryMap_t classIdOffsetPairToFieldEntryMap;

        // Map to support unique class entries in serializable statistics data base
        //
        ClassIdOffsetPairToClassEntryMap_t classIdOffsetPairToClassEntryMap;

        // Map to support unique memory allocation entries in serializable statistics data base
        //
        ClassIdSizePairToMemoryAllocationEntryMap_t classIdSizePairToMemoryAllocationEntryMap;

        // Map to support unique cache miss entries in serializable statistics data base
        //
        ClassIdCacheAccessIPTripletToCacheMissEntryMap_t classIdCacheAccessIPTripletToCacheMissEntryMap;

        // Map to support unique allocation site entries in serializable statistics data base
        //
        AllocationSiteIPClassIDPairToAllocationEntryMap_t allocationSiteIPClassIDPairToAllocationEntryMap;

        // Serializable ZSim profile data base
        //
        ZSimProfDB zsimProfDB;

        // Indicates whether collection is enabled
        //
        bool isProfileCollectionEnabledIndicator;

        // Profiling file name reported by Maxine.
        //
        std::string profFileName;

        // Default profiling file name.
        //
        static constexpr char const * defaultProfFileName = "zsim-prof.db";

        // Registers and retrieves allocation site entry
        //
        AllocSiteProf *registerAndRetrieveAllocationSiteEntry(PointerTag_t tag, ThreadId_t tid);

        // Registers cache miss
        //
        void registerCacheMiss(FieldProf * e, ClassIdCacheAccessIPTriplet_t & p, MAOffset_t offset, int missCount);

        PAD();
        lock_t classEntryProfilingLock;
        PAD();

        PAD();
        lock_t fieldEntryProfilingLock;
        PAD();

        PAD();
        lock_t memoryAllocationEntryProfilingLock;
        PAD();

        PAD();
        lock_t allocationSiteEntryProfilingLock;
        PAD();

        PAD();
        lock_t cacheMissEntryProfilingLock;
        PAD();

        PAD();
        lock_t zsimProfDBProfilingLock;
        PAD();

    // Singleton part
    public:
        // Get instance
        static MaxSimProfiling & getInst() {
            static MaxSimProfiling maxsimStatsDB;

            return maxsimStatsDB;
        }

        // Delete copy and move constructors and assign operators
        MaxSimProfiling(MaxSimProfiling const&) = delete;
        MaxSimProfiling(MaxSimProfiling&&) = delete;
        MaxSimProfiling& operator=(MaxSimProfiling const&) = delete;
        MaxSimProfiling& operator=(MaxSimProfiling &&) = delete;

    private:
        // Privatize constructor and destructor
        MaxSimProfiling();
        ~MaxSimProfiling() {}
};

#endif // MA_PROF_ENABLED && MAXSIM_ENABLED

#endif // MAXINE_STATISTICS_H_
