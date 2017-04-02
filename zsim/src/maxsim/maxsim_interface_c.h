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

#ifndef __ZSIM_HOOKS_CONSTS_H__
#define __ZSIM_HOOKS_CONSTS_H__

#include "maxsim/MaxSimInterface.pb.h"

// 64-bit argument
//
typedef uint64_t Arg64_t;

#ifdef __cplusplus
using namespace MaxSimInterface;
#else
typedef enum AddressRangeType AddressRangeType;
#endif

// Address range.
//
typedef struct AddressRange_t {
    // Low boundary of an address range
    uint64_t lo;
    // High boundary of an address range
    uint64_t hi;
    // Type of an address range
    AddressRangeType type;
    // Nesting counter (used for nested ARRAY_CRITICAL_ADDRESS_RANGE registrations)
    uint32_t counter;

#ifdef __cplusplus
    bool operator< (const AddressRange_t & right) const {
        return this->lo < right.lo;
    }

    bool equalTo(const AddressRange_t & right) const {
        return (this->lo == right.lo) && (this->hi == right.hi) && (this->type == right.type);
    }
#endif
} AddressRange_t;

#endif /*__ZSIM_HOOKS_CONSTS_H__*/
