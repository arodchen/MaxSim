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
#include "ma_stats.h"

#if defined(MA_PROF_ENABLED) && defined(MAXSIM_ENABLED)

class MaxSimStatsDB {
    public:
        // Add memory access
        void addMemoryAccess(PointerTag_t tag, MAOffset_t offset, Address bblIP, bool isWrite) {
        }

        // Add cache miss
        void addCacheMiss(PointerTag_t tag, MAOffset_t offset, Address bblIP, bool isWrite, MAProfCacheGroupId_t cacheGroupId, int missCount) {
        }

    // Singleton part
    public:
        // Get instance
        static MaxSimStatsDB & getInst() {
            static MaxSimStatsDB maxsimStatsDB;

            return maxsimStatsDB;
        }

        // Delete copy and move constructors and assign operators
        MaxSimStatsDB(MaxSimStatsDB const&) = delete;
        MaxSimStatsDB(MaxSimStatsDB&&) = delete;
        MaxSimStatsDB& operator=(MaxSimStatsDB const&) = delete;
        MaxSimStatsDB& operator=(MaxSimStatsDB &&) = delete;

    private:
        // Privatize constructor and destructor
        MaxSimStatsDB() {}
        ~MaxSimStatsDB() {}
};

#endif // MA_PROF_ENABLED && MAXSIM_ENABLED

#endif // MAXINE_STATISTICS_H_
