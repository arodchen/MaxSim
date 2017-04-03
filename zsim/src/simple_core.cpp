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

#include "simple_core.h"
#include "filter_cache.h"
#include "zsim.h"
#include "pointer_tagging.h"

#ifdef MAXSIM_ENABLED
#include "maxsim/maxsim_profiling.h"
#include "maxsim/maxsim_address_space_morphing.h"
#endif // MAXSIM_ENABLED

SimpleCore::SimpleCore(FilterCache* _l1i, FilterCache* _l1d, g_string& _name) : Core(_name), l1i(_l1i), l1d(_l1d), instrs(0), curCycle(0), haltedCycles(0), isCondBrunch(false), doSimulateBbl(true), maNum(0) {
#ifdef MA_PROF_ENABLED
    curBblAddr = UNDEF_VIRTUAL_ADDRESS;
#endif
}

void SimpleCore::initStats(AggregateStat* parentStat) {
    AggregateStat* coreStat = new AggregateStat();
    coreStat->init(name.c_str(), "Core stats");
    auto x = [this]() -> uint64_t { assert(curCycle >= haltedCycles); return curCycle - haltedCycles; };
    auto cyclesStat = makeLambdaStat(x);
    cyclesStat->init("cycles", "Simulated cycles");
    ProxyStat* instrsStat = new ProxyStat();
    instrsStat->init("instrs", "Simulated instructions", &instrs);
    coreStat->append(cyclesStat);
    coreStat->append(instrsStat);
    parentStat->append(coreStat);
}

uint64_t SimpleCore::getPhaseCycles() const {
    return curCycle % zinfo->phaseLength;
}

void SimpleCore::load(Address addr, MASize_t size, Address base) {
    assert(maNum < MA_NUM_MAX);
    if (maNum == MA_NUM_MAX) panic("Memory access storage is out of bounds!");
    maAddr[maNum] = addr;
    maSize[maNum] = size;
    maBase[maNum] = base;
    maIsLoad[maNum++] = true;
}

void SimpleCore::loadSim(Address addr, MASize_t size, Address base) {
#ifdef MA_PROF_ENABLED
#   ifdef POINTER_TAGGING_ENABLED
    PointerTag_t tag = getPointerTag(base);
    if (isTagNative(tag)) {
        tag = UNDEF_TAG;
    }
#   else
    PointerTag_t tag = UNDEF_TAG;
#   endif
    MAOffset_t offset = addr - base;

    addr = getUntaggedPointerSE(addr);
    base = getUntaggedPointerSE(base);
#   ifdef MAXSIM_ENABLED
    MaxSimRuntimeInfo::getInst().adjustTagAndOffset(tag, offset, addr);
    addr = MaxSimAddressSpaceMorphing::getInst().processMAAddressAndRemap(addr, base, offset, tag);
    MaxSimProfiling::getInst().addMemoryAccess(tag, offset, curBblAddr, false);
#   else
    UNUSED_VAR(tag); UNUSED_VAR(offset); UNUSED_VAR(curBblAddr);
#   endif
#endif // MA_PROF_ENABLED
    curCycle = l1d->load(addr, curCycle
#ifdef CLU_STATS_ENABLED
                         , size, LoadData
#endif
#ifdef MA_PROF_ENABLED
                         , tag, offset, curBblAddr
#endif
                         );
}

void SimpleCore::store(Address addr, MASize_t size, Address base) {
    assert(maNum < MA_NUM_MAX);
    if (maNum == MA_NUM_MAX) panic("Memory access storage is out of bounds!");
    maAddr[maNum] = addr;
    maSize[maNum] = size;
    maBase[maNum] = base;
    maIsLoad[maNum++] = false;
}

void SimpleCore::storeSim(Address addr, MASize_t size, Address base) {
#ifdef MA_PROF_ENABLED
#   ifdef POINTER_TAGGING_ENABLED
    PointerTag_t tag = getPointerTag(base);
    if (isTagNative(tag)) {
        tag = UNDEF_TAG;
    }
#   else
    PointerTag_t tag = UNDEF_TAG;
#   endif
    MAOffset_t offset = addr - base;

    addr = getUntaggedPointerSE(addr);
    base = getUntaggedPointerSE(base);
#   ifdef MAXSIM_ENABLED
    MaxSimRuntimeInfo::getInst().adjustTagAndOffset(tag, offset, addr);
    addr = MaxSimAddressSpaceMorphing::getInst().processMAAddressAndRemap(addr, base, offset, tag);
    MaxSimProfiling::getInst().addMemoryAccess(tag, offset, curBblAddr, true);
#   else
    UNUSED_VAR(tag); UNUSED_VAR(offset); UNUSED_VAR(curBblAddr);
#   endif
#endif // MA_PROF_ENABLED
    curCycle = l1d->store(addr, curCycle
#ifdef CLU_STATS_ENABLED
                          , size
#endif
#ifdef MA_PROF_ENABLED
                          , tag, offset, curBblAddr
#endif
                          );
}

void SimpleCore::bbl(THREADID tid, Address bblAddr, BblInfo* bblInfo) {
#ifdef MAXSIM_ENABLED
    doSimulateBbl = MaxSimAddressSpaceMorphing::getInst().processBBlAndDoSimulate(tid, bblAddr, isCondBrunch);
    if (!doSimulateBbl) {
        maNum = 0;
        isCondBrunch = false;
        return;
    }
#endif // MAXSIM_ENABLED

    for (int i = 0; i < maNum; i++) {
        if (maIsLoad[i]) {
            loadSim(maAddr[i], maSize[i], maBase[i]);
        } else {
            storeSim(maAddr[i], maSize[i], maBase[i]);
        }
    }
    //info("BBL %s %p", name.c_str(), bblInfo);
    //info("%d %d", bblInfo->instrs, bblInfo->bytes);
    instrs += bblInfo->instrs;
    curCycle += bblInfo->instrs;
#ifdef MA_PROF_ENABLED
    curBblAddr = bblAddr;
#endif
    maNum = 0;
    isCondBrunch = false;

    Address endBblAddr = bblAddr + bblInfo->bytes;
    for (Address fetchAddr = bblAddr; fetchAddr < endBblAddr; fetchAddr+=(1 << lineBits)) {
#ifdef MAXSIM_ENABLED
        MaxSimProfiling::getInst().addMemoryAccess(FETCH_TAG, UNDEF_OFFSET, bblAddr, false);
#endif
        curCycle = l1i->load(fetchAddr, curCycle
#ifdef CLU_STATS_ENABLED
                             , (1 << lineBits), FetchRightPath
#endif
#ifdef MA_PROF_ENABLED
                             , FETCH_TAG, UNDEF_OFFSET, curBblAddr
#endif
                             );
    }
}

void SimpleCore::contextSwitch(int32_t gid) {
    if (gid == -1) {
#ifdef MA_PROF_ENABLED
        curBblAddr = UNDEF_VIRTUAL_ADDRESS;
#endif
        l1i->contextSwitch();
        l1d->contextSwitch();
    }
}

void SimpleCore::join() {
    //info("[%s] Joining, curCycle %ld phaseEnd %ld haltedCycles %ld", name.c_str(), curCycle, phaseEndCycle, haltedCycles);
    if (curCycle < zinfo->globPhaseCycles) { //carry up to the beginning of the phase
        haltedCycles += (zinfo->globPhaseCycles - curCycle);
        curCycle = zinfo->globPhaseCycles;
    }
    phaseEndCycle = zinfo->globPhaseCycles + zinfo->phaseLength;
    //note that with long events, curCycle can be arbitrarily larger than phaseEndCycle; however, it must be aligned in current phase
    //info("[%s] Joined, curCycle %ld phaseEnd %ld haltedCycles %ld", name.c_str(), curCycle, phaseEndCycle, haltedCycles);
}


//Static class functions: Function pointers and trampolines

InstrFuncPtrs SimpleCore::GetFuncPtrs() {
    return {LoadFunc, StoreFunc, BblFunc, BranchFunc, PredLoadFunc, PredStoreFunc, FPTR_ANALYSIS, {0}};
}

void SimpleCore::LoadFunc(THREADID tid, ADDRINT addr, UINT32 size, ADDRINT base) {
    assert(size < 256);
    static_cast<SimpleCore*>(cores[tid])->load(addr, (MASize_t)size, base);
}

void SimpleCore::StoreFunc(THREADID tid, ADDRINT addr, UINT32 size, ADDRINT base) {
    assert(size < 256);
    static_cast<SimpleCore*>(cores[tid])->store(addr, (MASize_t)size, base);
}

void SimpleCore::PredLoadFunc(THREADID tid, ADDRINT addr, UINT32 size, ADDRINT base, BOOL pred) {
    assert(size < 256);
    if (pred) static_cast<SimpleCore*>(cores[tid])->load(addr, (MASize_t)size, base);
}

void SimpleCore::PredStoreFunc(THREADID tid, ADDRINT addr, UINT32 size, ADDRINT base, BOOL pred) {
    assert(size < 256);
    if (pred) static_cast<SimpleCore*>(cores[tid])->store(addr, (MASize_t)size, base);
}

void SimpleCore::BblFunc(THREADID tid, ADDRINT bblAddr, BblInfo* bblInfo) {
    SimpleCore* core = static_cast<SimpleCore*>(cores[tid]);
    core->bbl(tid, bblAddr, bblInfo);

    while (core->curCycle > core->phaseEndCycle) {
        assert(core->phaseEndCycle == zinfo->globPhaseCycles + zinfo->phaseLength);
        core->phaseEndCycle += zinfo->phaseLength;

        uint32_t cid = getCid(tid);
        //NOTE: TakeBarrier may take ownership of the core, and so it will be used by some other thread. If TakeBarrier context-switches us,
        //the *only* safe option is to return inmmediately after we detect this, or we can race and corrupt core state. If newCid == cid,
        //we're not at risk of racing, even if we were switched out and then switched in.
        uint32_t newCid = TakeBarrier(tid, cid);
        if (newCid != cid) break; /*context-switch*/
    }
}

