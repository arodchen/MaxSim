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

#ifdef MA_STATS_ENABLED

// Memory request attributes necessary for memory access statistics collection.
typedef struct MemReqMAStatsAttrs_t {
    PointerTag_t tag; // memory access pointer tag
    MAOffset_t offset; // memory access offset in [base + offset] addressing mode
    Address bblIP; // basic block ip address of memory access operation
} MemReqMAStatsAttrs_t;

// Undefined cache id.
#define UNDEF_CACHE_ID ((uint32_t) -1)

// Cache id type
typedef uint32_t MAStatsCacheGroupId_t;

// CacheRW id type
typedef MAStatsCacheGroupId_t MAStatsCacheRWGroupId_t;

// Memory Access (MA) cache group names for statistics collection.
extern vector<string> MAStatsCacheGroupNames;

inline MAStatsCacheGroupId_t getMAStatsCacheGroupNum() {
    return MAStatsCacheGroupNames.size();
}

inline MAStatsCacheGroupId_t getMAStatsCacheRWGroupNum() {
    return getMAStatsCacheGroupNum() << 1;
}

inline MAStatsCacheRWGroupId_t getMAStatsCacheRWGroupIdForCacheGroupIdAndRW(MAStatsCacheGroupId_t cacheGroupId, bool isWrite) {
    return (cacheGroupId << 1) | isWrite;
}

inline MAStatsCacheGroupId_t getMAStatsCacheGroupIdForCacheRWGroupId(MAStatsCacheRWGroupId_t cacheRWId) {
    return cacheRWId >> 1;
}

inline bool isMAStatsCacheRWGroupIdWrite(MAStatsCacheRWGroupId_t cacheRWGroupId) {
    return cacheRWGroupId & 0x1;
}

inline char const * getMAStatsCacheGroupName(MAStatsCacheGroupId_t cacheGroupId) {
    return MAStatsCacheGroupNames[cacheGroupId].c_str();
}

inline char const * getMAStatsCacheRWGroupName(MAStatsCacheRWGroupId_t cacheRWGroupId) {
    return getMAStatsCacheGroupName(getMAStatsCacheGroupIdForCacheRWGroupId(cacheRWGroupId));
}

#endif

#endif //SRC_MA_STATS_H
