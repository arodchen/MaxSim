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

#ifndef SRC_MA_STATS_H
#define SRC_MA_STATS_H

#include "zsim.h"
#include "stats.h"

#include <string>
#include <vector>

using std::string;
using std::vector;

#ifdef MA_PROF_ENABLED

// Memory request attributes necessary for profiling of memory access.
typedef struct MemReqMAProfAttrs_t {
    PointerTag_t tag; // memory access pointer tag
    MAOffset_t offset; // memory access offset in [base + offset] addressing mode
    Address bblIP; // basic block ip address of memory access operation
} MemReqMAProfAttrs_t;

// Undefined cache id.
#define UNDEF_CACHE_ID ((uint32_t) -1)

// Cache id type
typedef uint32_t MAProfCacheGroupId_t;

// CacheRW id type
typedef MAProfCacheGroupId_t MAProfCacheRWGroupId_t;

// Memory Access (MA) cache group names for profiling collection.
extern vector<string> MAProfCacheGroupNames;

inline MAProfCacheGroupId_t getMAProfCacheGroupNum() {
    return MAProfCacheGroupNames.size();
}

inline MAProfCacheGroupId_t getMAProfCacheRWGroupNum() {
    return getMAProfCacheGroupNum() << 1;
}

inline MAProfCacheRWGroupId_t getMAProfCacheRWGroupIdForCacheGroupIdAndRW(MAProfCacheGroupId_t cacheGroupId, bool isWrite) {
    return (cacheGroupId << 1) | isWrite;
}

inline MAProfCacheGroupId_t getMAProfCacheGroupIdForCacheRWGroupId(MAProfCacheRWGroupId_t cacheRWId) {
    return cacheRWId >> 1;
}

inline bool isMAProfCacheRWGroupIdWrite(MAProfCacheRWGroupId_t cacheRWGroupId) {
    return cacheRWGroupId & 0x1;
}

inline char const * getMAProfCacheGroupName(MAProfCacheGroupId_t cacheGroupId) {
    return MAProfCacheGroupNames[cacheGroupId].c_str();
}

inline char const * getMAProfCacheRWGroupName(MAProfCacheRWGroupId_t cacheRWGroupId) {
    return getMAProfCacheGroupName(getMAProfCacheGroupIdForCacheRWGroupId(cacheRWGroupId));
}

#endif

#endif //SRC_MA_STATS_H
