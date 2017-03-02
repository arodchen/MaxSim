/*
 * Copyright (c) 2017, Andrey Rodchenko, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.sun.max.vm.maxsim;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.C_FUNCTION;
import com.sun.max.annotate.INTRINSIC;
import com.sun.max.annotate.INLINE;
import com.sun.max.unsafe.*;
import com.sun.max.vm.intrinsics.MaxineIntrinsicIDs;

import java.lang.reflect.Array;

public class MaxSimMediator {
    /**
     * @see MaxineIntrinsicIDs#MAXSIM_MAGIC_OP
     */
    @INTRINSIC(MAXSIM_MAGIC_OP)
    private static native Word maxsimMagicOp(Word op);

    /**
     * @see MaxineIntrinsicIDs#MAXSIM_MAGIC_OP
     */
    @INTRINSIC(MAXSIM_MAGIC_OP)
    private static native Word maxsimMagicOp(Word op, Word arg);

    @C_FUNCTION
    private static native void maxsim_j_register_address_range(long begin, long end, int type);

    @C_FUNCTION
    private static native void maxsim_j_deregister_address_range(long begin, long end, int type);

    @INLINE
    public static void exitZSimFastForwardingMode() {
        maxsimMagicOp(Address.fromLong(MaxSimInterface.ZSimMagicOpcodes.ZSIM_M_OPC_ROI_BEGIN_VALUE));
    }

    @INLINE
    public static void enterZSimFastForwardingMode() {
        maxsimMagicOp(Address.fromLong(MaxSimInterface.ZSimMagicOpcodes.ZSIM_M_OPC_ROI_END_VALUE));
    }

    @INLINE
    public static int getAvailableProcessors() {
        return maxsimMagicOp(
            Address.fromLong(MaxSimInterface.MaxSimMagicOpcodes.MAXSIM_M_OPC_GET_AVAILABLE_PROCESSORS_NUM_VALUE)).
            asAddress().toInt();
    }

    @INLINE
    public static void dumpEventualStats(int maxineVMOperationMode) {
        maxsimMagicOp(
            Address.fromLong(MaxSimInterface.MaxSimMagicOpcodes.MAXSIM_M_OPC_DUMP_EVENTUAL_STATS_VALUE),
            Address.fromInt(maxineVMOperationMode));
    }
}
