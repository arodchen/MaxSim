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

#include "maxsim_address_space_morphing.h"

#ifdef MAXSIM_ENABLED

#include "maxsim_stats.h"
#include "pointer_tagging.h"

bool MaxSimAddressSpaceMorphing::processBBlAndDoSimulate(uint16_t tid, Address addressBbl, bool isCondBranch) {
    switch (maxineSimulationState[tid]) {
        case MaxineSimulationState_t::Normal:
            return true;
        case MaxineSimulationState_t::EstimateFilteredLoopHead:
            if (isCondBranch) {
                maxineSimulationState[tid] = MaxineSimulationState_t::InitiateFilteredLoop;
            }
            return true;
        case MaxineSimulationState_t::InitiateFilteredLoop:
            filteredLoopPhase[tid] = FILTERED_LOOP_PHASE_INITIATE;
            filteredLoopHead[tid] = addressBbl;
            maxineSimulationState[tid] = FilterLoopIterations;
            // fall through
        case MaxineSimulationState_t::FilterLoopIterations:
            if (addressBbl == filteredLoopHead[tid]) {
                filteredLoopPhase[tid] = (filteredLoopPhase[tid] + 1) % FILTERED_LOOP_PHASES_NUM;
            }
            return (filteredLoopPhase[tid] == FILTERED_LOOP_PHASE_SIMULATE);
        default:
            panic("Thread %d is in the unknown simulation state %d!", tid, maxineSimulationState[tid]);
    }
    return true;
}

Address MaxSimAddressSpaceMorphing::processMAAddressAndRemap(Address addr, Address base, int32_t offset, uint16_t tag) {
    if ((addr == UNDEF_VIRTUAL_ADDRESS) || (addr == NOP_VIRTUAL_ADDRESS)) {
        return addr;
    }
    if (LAYOUT_SCALE_FACTOR != 1) {
        AddressRange_t addressRange = MaxSimRuntimeInfo::getInst().getRegisteredAddressRange(addr, MaxSimRuntimeInfo::MaxineAddressSpace_t::Global);
        if (addressRange.type == HEAP_ADDRESS_RANGE) {
            unsigned char hubType = HUB_TYPE_TUPLE;
            AddressRange_t arrayCriticalAddressRange = MaxSimRuntimeInfo::getInst().getRegisteredAddressRange(addr, MaxSimRuntimeInfo::MaxineAddressSpace_t::HeapArrayCritical);

            if (arrayCriticalAddressRange.type == ARRAY_CRITICAL_ADDRESS_RANGE) {
                base = arrayCriticalAddressRange.lo - MaxSimRuntimeInfo::getInst().getMaxineArrayFirstElementOffset();
                offset = addr - base;
                hubType = HUB_TYPE_ARRAY_OF_PRIMITIVES;
            } else {
                if (offset > MaxSimRuntimeInfo::getInst().getMaxineArrayFirstElementOffset()) {
                    Address hubAddress = *((Address *) base);
                    if (hubAddress != NULL_VIRTUAL_ADDRESS) {
                        hubType = * ((char *) (getUntaggedPointerSE(hubAddress) + MaxSimRuntimeInfo::getInst().getMaxineHubTypeOffset()));
                    }
                    if (hubType >= HUB_TYPE_UNDEF) {
                        panic("Object hubByte should be less than %d!", HUB_TYPE_UNDEF);
                    }
                }
            }

            if ((hubType == HUB_TYPE_TUPLE) || (offset <= MaxSimRuntimeInfo::getInst().getMaxineArrayFirstElementOffset()) ||
                ((hubType == HUB_TYPE_ARRAY_OF_REFERENCES) && (LAYOUT_SCALE_REF_FACTOR == LAYOUT_SCALE_FACTOR_ONE))) {
                addr = ((addressRange.hi - addressRange.lo) / LAYOUT_SCALE_FACTOR) +
                                  ((addressRange.lo + addr) / LAYOUT_SCALE_FACTOR);
            } else {
                int offsetScaleFactor = (hubType == HUB_TYPE_ARRAY_OF_REFERENCES) ?
                     LAYOUT_SCALE_REF_FACTOR : LAYOUT_SCALE_FACTOR;
                addr = ((addressRange.hi - addressRange.lo) / LAYOUT_SCALE_FACTOR) +
                                  ((addressRange.lo + base + offsetScaleFactor * offset -
                                    MaxSimRuntimeInfo::getInst().getMaxineArrayFirstElementOffset()) / LAYOUT_SCALE_FACTOR);

            }
        }
    }
    return addr;
}

#endif // MAXSIM_ENABLED
