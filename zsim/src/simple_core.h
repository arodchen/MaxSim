/** $lic$
 * Copyright (C) 2012-2015 by Massachusetts Institute of Technology
 * Copyright (C) 2010-2013 by The Board of Trustees of Stanford University
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

#ifndef SIMPLE_CORE_H_
#define SIMPLE_CORE_H_

//A simple core model with IPC=1 except on memory accesses

#include "core.h"
#include "memory_hierarchy.h"
#include "pad.h"
#include "ma_prof.h"

class FilterCache;

class SimpleCore : public Core {
    protected:
        FilterCache* l1i;
        FilterCache* l1d;

        uint64_t instrs;
        uint64_t curCycle;
        uint64_t phaseEndCycle; //next stopping point
        uint64_t haltedCycles;

        bool isCondBrunch;
        bool doSimulateBbl;

        static const int MA_NUM_MAX = 512;
        Address maAddr[MA_NUM_MAX];
        MASize_t maSize[MA_NUM_MAX];
        Address maBase[MA_NUM_MAX];
        bool maIsLoad[MA_NUM_MAX];
        int maNum;

#ifdef MA_PROF_ENABLED
        Address curBblAddr;
#endif

    public:
        SimpleCore(FilterCache* _l1i, FilterCache* _l1d, g_string& _name);
        void initStats(AggregateStat* parentStat);

        uint64_t getInstrs() const {return instrs;}
        uint64_t getPhaseCycles() const;
        uint64_t getCycles() const {return curCycle - haltedCycles;}

        void contextSwitch(int32_t gid);
        virtual void join();

        InstrFuncPtrs GetFuncPtrs();

    protected:
        //Simulation functions
        inline void load(Address addr, MASize_t size, Address base);
        inline void store(Address addr, MASize_t size, Address base);
        inline void loadSim(Address addr, MASize_t size, Address base);
        inline void storeSim(Address addr, MASize_t size, Address base);
        inline void bbl(THREADID tid, Address bblAddr, BblInfo* bblInstrs);

        static void LoadFunc(THREADID tid, ADDRINT addr, UINT32 size, ADDRINT base);
        static void StoreFunc(THREADID tid, ADDRINT addr, UINT32 size, ADDRINT base);
        static void BblFunc(THREADID tid, ADDRINT bblAddr, BblInfo* bblInfo);
        static void PredLoadFunc(THREADID tid, ADDRINT addr, UINT32 size, ADDRINT base, BOOL pred);
        static void PredStoreFunc(THREADID tid, ADDRINT addr, UINT32 size, ADDRINT base, BOOL pred);

        static void BranchFunc(THREADID tid, ADDRINT addr, BOOL taken, ADDRINT takenNpc, ADDRINT notTakenNpc) {
            static_cast<SimpleCore*>(cores[tid])->isCondBrunch = (addr != 0);
        }
}  ATTR_LINE_ALIGNED; //This needs to take up a whole cache line, or false sharing will be extremely frequent

#endif  // SIMPLE_CORE_H_

