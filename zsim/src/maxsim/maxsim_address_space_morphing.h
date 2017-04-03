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

#ifndef SRC_MAXINE_SIMULATION_ASSIST_H
#define SRC_MAXINE_SIMULATION_ASSIST_H

#include "zsim.h"

#ifdef MAXSIM_ENABLED

#include "constants.h"
#include "maxsim/maxsim_interface_c.h"
#include "maxsim/maxsim_runtime_info.h"

class MaxSimAddressSpaceMorphing {
  public:
    // Processes basic block; returns true if basic block should be simulated and false if it should be filtered.
    //
    // Precondition: passed addresses should be untagged.
    //
    bool processBBlAndDoSimulate(ThreadId_t tid, Address addressBbl, bool isCondBranch);

    // Processes memory access address and remap; returns re-mapped address.
    //
    // Precondition: passed addresses should be untagged.
    //
    Address processMAAddressAndRemap(Address addr, Address base, MAOffset_t offset, PointerTag_t tag);

    // Begins loop filtering.
    //
    // Precondition: passed addresses should be untagged.
    //
    void beginLoopFiltering(ThreadId_t tid, Address addr) {
        AddressRange_t addressRange = MaxSimRuntimeInfo::getInst().getRegisteredAddressRange(addr,
            MaxSimRuntimeInfo::MaxineAddressSpace_t::Global);
        if (addressRange.type == HEAP_ADDRESS_RANGE) {
            maxineSimulationState[tid] = MaxineSimulationState_t::EstimateFilteredLoopHead;
        }
    }

    // Ends loop filtering.
    //
    void endLoopFiltering(ThreadId_t tid) {
        maxineSimulationState[tid] = MaxineSimulationState_t::Normal;
    }

    // Activates data transformation via address space morhping.
    //
    void activateDataTransformation(AddressRange_t * dataTransInfoMessage);

  private:
    const int LAYOUT_SCALE_FACTOR = MaxSimConfig::default_instance().layoutscalefactor();
    const int LAYOUT_SCALE_REF_FACTOR = MaxSimConfig::default_instance().layoutscalereffactor();
    const int LAYOUT_SCALE_FACTOR_ONE = 1;

    const int FILTERED_LOOP_PHASE_SIMULATE = 0;
    const int FILTERED_LOOP_PHASE_INITIATE = FILTERED_LOOP_PHASES_NUM - 1;
    const int FILTERED_LOOP_PHASES_NUM = LAYOUT_SCALE_FACTOR;

    typedef enum {
        Normal = 0,
        EstimateFilteredLoopHead,
        InitiateFilteredLoop,
        FilterLoopIterations
    } MaxineSimulationState_t;

    MaxineSimulationState_t maxineSimulationState[MAX_THREADS];

    Address filteredLoopHead[MAX_THREADS];

    int filteredLoopPhase[MAX_THREADS];

    PAD();
    lock_t classIdToFieldOffsetRemapMapLock;
    PAD();

    std::map<PointerTag_t, std::map<MAOffset_t, MAOffset_t>> classIdToFieldOffsetRemapMap;

  // Singleton part
  public:
    // Get instance
    static MaxSimAddressSpaceMorphing &getInst() {
        static MaxSimAddressSpaceMorphing maxsimAddressSpaceMorphing;

        return maxsimAddressSpaceMorphing;
    }

    // Delete copy and move constructors and assign operators
    MaxSimAddressSpaceMorphing(MaxSimAddressSpaceMorphing const &) = delete;
    MaxSimAddressSpaceMorphing(MaxSimAddressSpaceMorphing &&) = delete;
    MaxSimAddressSpaceMorphing &operator=(MaxSimAddressSpaceMorphing const &) = delete;
    MaxSimAddressSpaceMorphing &operator=(MaxSimAddressSpaceMorphing &&) = delete;

  private:
    // Privatize constructor and destructor
    MaxSimAddressSpaceMorphing();
    ~MaxSimAddressSpaceMorphing() {}
};

#endif // MAXSIM_ENABLED

#endif // SRC_MAXINE_SIMULATION_ASSIST_H
