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

#include "log.h"
#include "zsim.h"

#ifdef MAXSIM_ENABLED

#include "maxsim_mediator.h"
#include "maxsim_interface_c.h"
#include "cpuenum.h"
#include "maxsim_runtime_info.h"
#include "maxsim_interface_helpers.h"
#include "maxsim_address_space_morphing.h"

VOID MaxSimMediator::HandleMaxSimMagicOp(THREADID tid, ADDRINT * op, ADDRINT arg) {
    if (!MaxSimInterfaceHelpers::isMaxSimEnabled()) {
        panic("Thread %d issued MaxSim magic op %ld while MaxSimConfig.isMaxSimEnabled is false!", tid, *op);
    }

    switch (*op) {
        case MAXSIM_M_OPC_GET_AVAILABLE_PROCESSORS_NUM: {
            Arg64_t * availableProcessorsNumPointer = (Arg64_t *) op;

            *(availableProcessorsNumPointer) = cpuenumNumCpus(procIdx);
            return;
        }
        case MAXSIM_M_OPC_REPORT_HUB_OFFSET: {
            MAOffset_t hubOffset = (MAOffset_t) arg;

            MaxSimRuntimeInfo::getInst().setMaxineHubOffset(hubOffset);
            return;
        }
        case MAXSIM_M_OPC_REPORT_HUB_TYPE_OFFSET: {
            MAOffset_t hubTypeOffset = (MAOffset_t) arg;

            MaxSimRuntimeInfo::getInst().setMaxineHubTypeOffset(hubTypeOffset);
            return;
        }
        case MAXSIM_M_OPC_REPORT_ARRAY_FIRST_ELEM_OFFSET: {
            MAOffset_t arrayFirstElemOffset = (MAOffset_t) arg;

            MaxSimRuntimeInfo::getInst().setMaxineArrayFirstElementOffset(arrayFirstElemOffset);
            return;
        }
        case MAXSIM_M_OPC_REGISTER_ADDRESS_RANGE: {
            AddressRange_t * addressRange = (AddressRange_t *) arg;

            MaxSimRuntimeInfo::getInst().registerAddressRange(addressRange);
            return;
        }
        case MAXSIM_M_OPC_DEREGISTER_ADDRESS_RANGE: {
            AddressRange_t * addressRange = (AddressRange_t *) arg;

            MaxSimRuntimeInfo::getInst().deregisterAddressRange(addressRange);
            return;
        }
        case MAXSIM_M_OPC_DUMP_EVENTUAL_STATS: {
            MaxineVMOperationMode maxineVMOperationMode = (MaxineVMOperationMode) arg;
            DumpEventualStats(procIdx, "Request from Maxine VM to dump eventual stats", maxineVMOperationMode);
            return;
        }
        case MAXSIM_M_OPC_FILTER_LOOP_BEGIN: {
            Address addr = (Address) arg;
            MaxSimAddressSpaceMorphing::getInst().beginLoopFiltering(tid, addr);
            return;
        }
        case MAXSIM_M_OPC_FILTER_LOOP_END: {
            MaxSimAddressSpaceMorphing::getInst().endLoopFiltering(tid);
            return;
        }
        default:
            panic("Thread %d issued unknown MaxSim magic op %ld!", tid, *op);
    }
}

#endif // MAXSIM_ENABLED
