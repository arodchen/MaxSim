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

#include "maxsim/maxsim_profiling.h"

#if defined(MA_PROF_ENABLED) && defined(MAXSIM_ENABLED)

#include "stack_trace_estimation.h"
#include "zsim.h"
#include <fstream>

void MaxSimProfiling::addMemoryAccess(PointerTag_t tag, MAOffset_t offset, Address bblIP, bool isWrite) {
    if (!isProfileCollectionEnabled()) {
        return;
    }

    // lookup class entry and add access info
    futex_lock(& classEntryProfilingLock);
    ClassProf *classEntry = classIdOffsetPairToClassEntryMap[tag];
    if (classEntry == nullptr) {

        futex_lock(& zsimProfDBProfilingLock);
        classEntry = zsimProfDB.add_classprof();
        futex_unlock(& zsimProfDBProfilingLock);

        classEntry->set_id(tag);
        classEntry->set_memallsize(0);
        classEntry->set_memallcount(0);
        classIdOffsetPairToClassEntryMap[tag] = classEntry;
    }
    classEntry->set_memacccount(classEntry->memacccount() + 1);
    futex_unlock(& classEntryProfilingLock);

    // lookup field entry and add access info
    futex_lock(& fieldEntryProfilingLock);
    ClassIdOffsetPair_t fieldKey(tag, offset);
    FieldProf *fieldEntry = classIdOffsetPairToFieldEntryMap[fieldKey];
    if (fieldEntry == nullptr) {
        fieldEntry = classEntry->add_fieldprof();
        fieldEntry->set_offset(offset);
        fieldEntry->set_readcount(0);
        fieldEntry->set_writecount(0);

        for (MAProfCacheGroupId_t i = 0; i < getMAProfCacheRWGroupNum(); i++) {
            fieldEntry->add_cacherwgroupmisscount(0);
        }

        classIdOffsetPairToFieldEntryMap[fieldKey] = fieldEntry;
    }
    if (isWrite) {
        fieldEntry->set_writecount(fieldEntry->writecount() + 1);
    } else {
        fieldEntry->set_readcount(fieldEntry->readcount() + 1);
    }
    futex_unlock(& fieldEntryProfilingLock);

}

void MaxSimProfiling::addCacheMiss(PointerTag_t tag, int offset, uint64_t bblIP, bool isWrite, MAProfCacheGroupId_t MAProfcacheGroupId, int missCount) {
    if (!isProfileCollectionEnabled() ||
        (MAProfcacheGroupId == UNDEF_CACHE_ID)) {
        return;
    }

    // lookup field entry
    ClassIdOffsetPair_t fieldKey(tag, offset);
    ClassIdCacheAccessIPTriplet_t p(ClassIdCacheRWIdPair_t(tag, getMAProfCacheRWGroupIdForCacheGroupIdAndRW(MAProfcacheGroupId, isWrite)), bblIP);
    FieldProf *fieldEntry;

    futex_lock(& fieldEntryProfilingLock);
    fieldEntry = classIdOffsetPairToFieldEntryMap[fieldKey];
    futex_unlock(& fieldEntryProfilingLock);

    registerCacheMiss(fieldEntry, p, offset, missCount);
}

void MaxSimProfiling::serializeToFile() {
    std::fstream zsimProfDBStream(profFileName, std::ios::out | std::ios::binary);

    futex_lock(& classEntryProfilingLock);
    futex_lock(& fieldEntryProfilingLock);
    futex_lock(& memoryAllocationEntryProfilingLock);
    futex_lock(& allocationSiteEntryProfilingLock);
    futex_lock(& cacheMissEntryProfilingLock);
    futex_lock(& zsimProfDBProfilingLock);

    zsimProfDB.SerializeToOstream(& zsimProfDBStream);

    futex_unlock(& zsimProfDBProfilingLock);
    futex_unlock(& cacheMissEntryProfilingLock);
    futex_unlock(& allocationSiteEntryProfilingLock);
    futex_unlock(& memoryAllocationEntryProfilingLock);
    futex_unlock(& fieldEntryProfilingLock);
    futex_unlock(& classEntryProfilingLock);

}

void MaxSimProfiling::resetProfileCollection() {

    futex_lock(& classEntryProfilingLock);
    futex_lock(& fieldEntryProfilingLock);
    futex_lock(& memoryAllocationEntryProfilingLock);
    futex_lock(& allocationSiteEntryProfilingLock);
    futex_lock(& cacheMissEntryProfilingLock);
    futex_lock(& zsimProfDBProfilingLock);
    classIdOffsetPairToFieldEntryMap.clear();
    classIdOffsetPairToClassEntryMap.clear();
    classIdSizePairToMemoryAllocationEntryMap.clear();
    classIdCacheAccessIPTripletToCacheMissEntryMap.clear();
    allocationSiteIPClassIDPairToAllocationEntryMap.clear();
    zsimProfDB.Clear();
    profFileName.assign(defaultProfFileName);
    zsimProfDB.set_maxallocsiteprofid(MaxSimInterface::TAG_GP_LO - 1);
    for (MAProfCacheRWGroupId_t i = 0; i < getMAProfCacheRWGroupNum(); i++) {
        CacheRWGroupInfo * cacheMissProfCategory = zsimProfDB.add_cacherwgroupinfo();
        cacheMissProfCategory->set_cacherwgroupid(i);
        cacheMissProfCategory->set_cachegroupid(getMAProfCacheGroupIdForCacheRWGroupId(i));
        cacheMissProfCategory->set_cachegroupname(getMAProfCacheRWGroupName(i));
        cacheMissProfCategory->set_iswrite(isMAProfCacheRWGroupIdWrite(i));
        zsimProfDB.add_cacherwgroupmissprof();
    }
    futex_unlock(& zsimProfDBProfilingLock);
    futex_unlock(& cacheMissEntryProfilingLock);
    futex_unlock(& allocationSiteEntryProfilingLock);
    futex_unlock(& memoryAllocationEntryProfilingLock);
    futex_unlock(& fieldEntryProfilingLock);
    futex_unlock(& classEntryProfilingLock);

}

void MaxSimProfiling::profileObjectAllocation(PointerTag_t tag, uint16_t tagType, MASize_t size, ThreadId_t tid) {
    if (!isProfileCollectionEnabled()) {
        return;
    }

    futex_lock(& classEntryProfilingLock);
    ClassProf *classEntry = classIdOffsetPairToClassEntryMap[tag];
    if (classEntry == nullptr) {

        futex_lock(& zsimProfDBProfilingLock);
        classEntry = zsimProfDB.add_classprof();
        futex_unlock(& zsimProfDBProfilingLock);

        classEntry->set_id(tag);
        classIdOffsetPairToClassEntryMap[tag] = classEntry;
        classEntry->set_memacccount(0);
    }
    classEntry->set_memallcount(classEntry->memallcount() + 1);
    classEntry->set_memallsize(classEntry->memallsize() + (uint64_t)size);
    futex_unlock(& classEntryProfilingLock);

    // lookup memory allocation entry info
    futex_lock(& memoryAllocationEntryProfilingLock);
    ClassIdSizePair_t memoryAllocationKey(tag, size);
    AllocProf *memoryAllocationEntry = classIdSizePairToMemoryAllocationEntryMap[memoryAllocationKey];
    if (memoryAllocationEntry == nullptr) {
        memoryAllocationEntry = classEntry->add_allocprof();
        memoryAllocationEntry->set_size(size);
        classIdSizePairToMemoryAllocationEntryMap[memoryAllocationKey] = memoryAllocationEntry;
    }
    memoryAllocationEntry->set_count(memoryAllocationEntry->count() + 1);
    futex_unlock(& memoryAllocationEntryProfilingLock);

    // lookup allocation site info
    if (tagType == CLASS_ID_TAGGING) {
        registerAndRetrieveAllocationSiteEntry(tag, tid);
    }
}

PointerTag_t MaxSimProfiling::getAllocationSiteEstimationID(PointerTag_t tag, ThreadId_t tid) {
    AllocSiteProf * allocationSiteEntry = registerAndRetrieveAllocationSiteEntry(tag, tid);
    return (PointerTag_t) allocationSiteEntry->id();
}

AllocSiteProf * MaxSimProfiling::registerAndRetrieveAllocationSiteEntry(PointerTag_t tag, ThreadId_t tid) {
    // Predicate to check if an address belongs to an allocation frontier
    auto allocationFrontierPred = [] (uint64_t address) {
        if (MaxSimRuntimeInfo::getInst().getRegisteredAddressRange(address, MaxSimRuntimeInfo::MaxineAddressSpace_t::CodeAllocationFrontier).type == ALLOCATION_FRONTIER_ADDRESS_RANGE) {
            return true;
        }
        return false;
    };

    int allocationFunctionFrameNo = StackTraceEstimation::getInst().findFrameNoIf(tid, allocationFrontierPred);
    uint64_t allocationSiteIPApprox = StackTraceEstimation::getInst().topNthReturnAddress(tid,
        allocationFunctionFrameNo == StackTraceEstimation::UNDEF_FRAME_NO ? 0 : allocationFunctionFrameNo + 1);
    AllocationSiteIPClassIdPair_t allocationSiteIPAndClassKey(allocationSiteIPApprox, tag);

    futex_lock(& allocationSiteEntryProfilingLock);
    AllocationSiteIPClassIDPairToAllocationEntryMap_t::iterator it = allocationSiteIPClassIDPairToAllocationEntryMap.find(allocationSiteIPAndClassKey);
    AllocSiteProf * allocationSiteEntry = nullptr;
    if (it == allocationSiteIPClassIDPairToAllocationEntryMap.end()) {
        int maxallocsiteprofid;

        futex_lock(& zsimProfDBProfilingLock);
        maxallocsiteprofid = zsimProfDB.maxallocsiteprofid();
        futex_unlock(& zsimProfDBProfilingLock);

        if ((maxallocsiteprofid == MaxSimInterface::TAG_GP_HI) ||
            MaxSimInterfaceHelpers::isUndefinedGeneralPurposeTag(tag)) {
            allocationSiteEntry = allocationSiteIPClassIDPairToAllocationEntryMap[{UNDEF_VIRTUAL_ADDRESS, MaxSimInterface::TAG_UNDEFINED_GP}];
            if (allocationSiteEntry == nullptr) {

                futex_lock(& zsimProfDBProfilingLock);
                allocationSiteEntry = zsimProfDB.add_allocsiteprof();
                futex_unlock(& zsimProfDBProfilingLock);

                allocationSiteEntry->set_id(MaxSimInterface::TAG_UNDEFINED_GP);
                allocationSiteEntry->set_ip(UNDEF_VIRTUAL_ADDRESS);
                allocationSiteEntry->set_classid(MaxSimInterface::TAG_UNDEFINED_GP);
                allocationSiteEntry->set_count(0);
                allocationSiteIPClassIDPairToAllocationEntryMap[{UNDEF_VIRTUAL_ADDRESS, MaxSimInterface::TAG_UNDEFINED_GP}] = allocationSiteEntry;
            }
        } else {

            futex_lock(& zsimProfDBProfilingLock);
            zsimProfDB.set_maxallocsiteprofid(zsimProfDB.maxallocsiteprofid() + 1);
            allocationSiteEntry = zsimProfDB.add_allocsiteprof();
            allocationSiteEntry->set_id(zsimProfDB.maxallocsiteprofid());
            futex_unlock(& zsimProfDBProfilingLock);

            allocationSiteEntry->set_ip(allocationSiteIPApprox);
            allocationSiteEntry->set_classid(tag);
            allocationSiteEntry->set_count(0);
            allocationSiteIPClassIDPairToAllocationEntryMap[allocationSiteIPAndClassKey] = allocationSiteEntry;
        }
    } else {
        allocationSiteEntry = (*it).second;
    }
    if (isProfileCollectionEnabled()) {
        allocationSiteEntry->set_count(allocationSiteEntry->count() + 1);
    }
    futex_unlock(& allocationSiteEntryProfilingLock);

    return allocationSiteEntry;
}

void MaxSimProfiling::registerCacheMiss(FieldProf * e, ClassIdCacheAccessIPTriplet_t & p, int offset, int missCount) {
    CacheMissProf * cacheMissEntry;
    PointerTag_t tag = p.first.first;
    MAProfCacheRWGroupId_t cacheRWId = p.first.second;

    futex_lock(& cacheMissEntryProfilingLock);
    e->set_cacherwgroupmisscount(cacheRWId, e->cacherwgroupmisscount(cacheRWId) + missCount);
    cacheMissEntry = classIdCacheAccessIPTripletToCacheMissEntryMap[p];
    if (cacheMissEntry == nullptr) {

        futex_lock(& zsimProfDBProfilingLock);
        cacheMissEntry = zsimProfDB.mutable_cacherwgroupmissprof(cacheRWId)->add_cachemissprof();
        futex_unlock(& zsimProfDBProfilingLock);

        cacheMissEntry->set_count(missCount);
        cacheMissEntry->set_ip(p.second);
        cacheMissEntry->set_classid(tag);
        cacheMissEntry->set_offsetlo(offset);
        cacheMissEntry->set_offsethi(offset);
        classIdCacheAccessIPTripletToCacheMissEntryMap[p] = cacheMissEntry;
    } else {
        cacheMissEntry->set_count(cacheMissEntry->count() + missCount);
        if (offset < cacheMissEntry->offsetlo()) {
            cacheMissEntry->set_offsetlo(offset);
        } else if (offset > cacheMissEntry->offsethi()) {
            cacheMissEntry->set_offsethi(offset);
        }
    }
    futex_unlock(& cacheMissEntryProfilingLock);

}

MaxSimProfiling::MaxSimProfiling() : classEntryProfilingLock(0), fieldEntryProfilingLock(0),
                                     memoryAllocationEntryProfilingLock(0), allocationSiteEntryProfilingLock(0),
                                     cacheMissEntryProfilingLock(0), zsimProfDBProfilingLock(0) {
    resetProfileCollection();
}

#endif // MA_PROF_ENABLED && MAXSIM_ENABLED
