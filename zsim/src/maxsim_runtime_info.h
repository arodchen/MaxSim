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

#ifdef MAXSIM_ENABLED

// MaxSim Maxine runtime information
//
class MaxSimRuntimeInfo {

    friend class MaxSimMediator;

    public:
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

         MAOffset_t MaxineHubTypeOffset;
         bool isMaxineHubTypeOffsetSet;

         MAOffset_t MaxineArrayFirstElementOffset;
         bool isMaxineArrayFirstElementOffsetSet;

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
        MaxSimRuntimeInfo() : isMaxineHubTypeOffsetSet(false), isMaxineArrayFirstElementOffsetSet(false) {}
        ~MaxSimRuntimeInfo() {}
};

#endif // MAXSIM_ENABLED

#endif //SRC_MAXSIM_RUNTIME_INFO_H
