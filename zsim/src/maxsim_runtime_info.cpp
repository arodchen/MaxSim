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

#include "maxsim_runtime_info.h"

#ifdef MAXSIM_ENABLED

MaxSimRuntimeInfo::MaxineAddressSpace_t MaxSimRuntimeInfo::getMaxineAddressSpaceByAddressRangeType(AddressRangeType type) {
    switch (type) {
        default:
            panic("Cannot get Maxine address space for address range type %d", type);
        case STACK_ADDRESS_RANGE:
        case TLS_ADDRESS_RANGE:
        case HEAP_ADDRESS_RANGE:
        case CODE_ADDRESS_RANGE:
        case NATIVE_ADDRESS_RANGE:
            return MaxineAddressSpace_t::Global;
        case ARRAY_CRITICAL_ADDRESS_RANGE:
            return MaxineAddressSpace_t::HeapArrayCritical;

    }
}

void MaxSimRuntimeInfo::registerAddressRange(AddressRange_t * addressRange) {
    VectorOfAddressRanges_t::iterator it;
    MaxineAddressSpace_t space = getMaxineAddressSpaceByAddressRangeType(addressRange->type);

#ifndef NPRINT_REGDEREG_GLOB_MEM_RANGES
    if (space == MaxineAddressSpace_t::Global) {
        info("Registering range l:0x%lx h:0x%lx t:%d", addressRange->lo, addressRange->hi, addressRange->type);
    }
#endif

    futex_lock(& registeredAddressRangesLock);

    it = std::lower_bound(disjointAddressRanges[space].begin(), disjointAddressRanges[space].end(), *addressRange);

    if ((it != disjointAddressRanges[space].end()) && addressRange->equalTo(*it)) {
        assert(space == MaxineAddressSpace_t::HeapArrayCritical);
        (*it).counter++;
        futex_unlock(& registeredAddressRangesLock);
        return;
    }
    addressRange->counter = 0;
    it = disjointAddressRanges[space].insert(it, *addressRange);

    ++it;
    while (it != disjointAddressRanges[space].end() && (addressRange->hi > it->lo)) {
        it = disjointAddressRanges[space].erase(it);
        warn("Address range intersection detected: deregistering intersecting ranges.");
    }

    --it;
    if (it != disjointAddressRanges[space].begin()) {
        --it;
        while (it != disjointAddressRanges[space].begin() && (it->hi > addressRange->lo)) {
            it = disjointAddressRanges[space].erase(it);
            warn("Address range intersection detected: deregistering intersecting ranges.");
        }
    }

    futex_unlock(& registeredAddressRangesLock);
}

void  MaxSimRuntimeInfo::deregisterAddressRange(AddressRange_t * addressRange) {
    VectorOfAddressRanges_t::iterator it;
    MaxineAddressSpace_t space = getMaxineAddressSpaceByAddressRangeType(addressRange->type);

    futex_lock(& registeredAddressRangesLock);

    it = std::lower_bound(disjointAddressRanges[space].begin(), disjointAddressRanges[space].end(), *addressRange);

    if ((it != disjointAddressRanges[space].end()) && addressRange->equalTo(*it)) {
        if ((*it).counter) {
            assert(space == MaxineAddressSpace_t::HeapArrayCritical);
            (*it).counter--;
        } else {
            disjointAddressRanges[space].erase(it);
        }
        futex_unlock(& registeredAddressRangesLock);
#ifndef NPRINT_REGDEREG_GLOB_MEM_RANGES
        if (space == MaxineAddressSpace_t::Global) {
            info("Deregistering range l:0x%lx h:0x%lx", addressRange->lo, addressRange->hi);
        }
#endif
        return;
    }

    futex_unlock(& registeredAddressRangesLock);
    warn("Deregistering non-existing address range!");
}

AddressRange_t MaxSimRuntimeInfo::getRegisteredAddressRange(uint64_t address, MaxSimRuntimeInfo::MaxineAddressSpace_t space) {
    VectorOfAddressRanges_t::iterator it;
    AddressRange_t addressRange = {address, address, UNDEFINED_ADDRESS_RANGE};

    futex_lock(& registeredAddressRangesLock);

    it = std::lower_bound(disjointAddressRanges[space].begin(), disjointAddressRanges[space].end(), addressRange);
    if (it != disjointAddressRanges[space].begin()) {
        --it;
        if (address < it->hi) {
            addressRange = *it;
        }
    }

    futex_unlock(& registeredAddressRangesLock);

    return addressRange;
}

#endif // MAXSIM_ENABLED
