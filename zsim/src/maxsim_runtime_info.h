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

#ifndef SRC_MAXSIM_RUNTIME_INFO_H
#define SRC_MAXSIM_RUNTIME_INFO_H

#include "zsim.h"
#include "maxsim_interface_c.h"
#include "maxsim_interface_helpers.h"

#ifdef MAXSIM_ENABLED

#define NPRINT_REGDEREG_GLOB_MEM_RANGES

// MaxSim Maxine runtime information
//
class MaxSimRuntimeInfo {

    friend class MaxSimMediator;

    public:
        // Maxine Address spaces.
        //
        // Address ranges within the same address space should not intersect, but address ranges form different spaces
        // can intersect.
        //
        typedef enum MaxineAddressSpace_t {
            // Global address space containing the following address ranges: TLS, stack, heap, code cache, native
            Global,
            // Heap address subspace containing array critical (see JNI GetPrimitiveArrayCritical) address ranges
            HeapArrayCritical,
            // Code address subspace containing allocation frontier (functions called at allocation sites) code address ranges
            CodeAllocationFrontier,
            // Number of address spaces
            AddressSpacesNum
        } MaxineAddressSpace_t;

        // Get Maxine address space by an address range type
        //
        MaxineAddressSpace_t getMaxineAddressSpaceByAddressRangeType(AddressRangeType type);

        // Get a registered address range for a given address and an address space
        //
        AddressRange_t getRegisteredAddressRange(Address address, MaxSimRuntimeInfo::MaxineAddressSpace_t space);

        // Adjust tag and offset
        //
        void adjustTagAndOffset(PointerTag_t & tag, MAOffset_t & offset, Address address);

        // Get Maxine hub offset
        //
        MAOffset_t getMaxineHubOffset() {
            if (!isMaxineHubOffsetSet) {
                panic("Use of undefined hubOffset");
            }
            return MaxineHubOffset;
        }

        // Get Maxine hub type offset
        //
        MAOffset_t getMaxineHubTypeOffset() {
            if (!isMaxineHubTypeOffsetSet) {
                panic("Use of undefined hubTypeOffset");
            }
            return MaxineHubTypeOffset;
        }

        // Get Maxine array first element offset
        //
        MAOffset_t getMaxineArrayFirstElementOffset() {
            if (!isMaxineArrayFirstElementOffsetSet) {
                panic("Use of undefined hubTypeOffset");
            }
            return MaxineArrayFirstElementOffset;
        }

    private:
        // Vector of address ranges
        typedef std::vector<AddressRange_t> VectorOfAddressRanges_t;

        // Set Maxine hub offset
        //
        void setMaxineHubOffset(MAOffset_t offset) {
            MaxineHubOffset = offset;
            isMaxineHubOffsetSet = true;
        }

        // Set Maxine hub type offset
        //
        void setMaxineHubTypeOffset(MAOffset_t offset) {
            MaxineHubTypeOffset = offset;
            isMaxineHubTypeOffsetSet = true;
        }

        // Set Maxine array first element offset
        //
        void setMaxineArrayFirstElementOffset(MAOffset_t offset) {
            MaxineArrayFirstElementOffset = offset;
            isMaxineArrayFirstElementOffsetSet = true;
        }

        // Register address range
        //
        void registerAddressRange(AddressRange_t * addressRange);

        // Deregister address range
        //
        void deregisterAddressRange(AddressRange_t * addressRange);

        MAOffset_t MaxineHubOffset;
        bool isMaxineHubOffsetSet;

        MAOffset_t MaxineHubTypeOffset;
        bool isMaxineHubTypeOffsetSet;

        MAOffset_t MaxineArrayFirstElementOffset;
        bool isMaxineArrayFirstElementOffsetSet;

        PAD();
        lock_t registeredAddressRangesLock;
        PAD();

        // Array of sorted vectors of address ranges for each Maxine address space
        VectorOfAddressRanges_t disjointAddressRanges[MaxineAddressSpace_t::AddressSpacesNum];

    // Singleton part
    public:
        // Get instance
        //
        static MaxSimRuntimeInfo &getInst() {
            static MaxSimRuntimeInfo maxsimStatsDB;

            return maxsimStatsDB;
        }

        // Delete copy and move constructors and assign operators
        MaxSimRuntimeInfo(MaxSimRuntimeInfo const &) = delete;
        MaxSimRuntimeInfo(MaxSimRuntimeInfo &&) = delete;
        MaxSimRuntimeInfo &operator=(MaxSimRuntimeInfo const &) = delete;
        MaxSimRuntimeInfo &operator=(MaxSimRuntimeInfo &&) = delete;

    private:
        // Privatize constructor and destructor
        MaxSimRuntimeInfo() : isMaxineHubOffsetSet(false), isMaxineHubTypeOffsetSet(false), isMaxineArrayFirstElementOffsetSet(false), registeredAddressRangesLock(0) {}
        ~MaxSimRuntimeInfo() {}
};

#endif // MAXSIM_ENABLED

#endif //SRC_MAXSIM_RUNTIME_INFO_H
