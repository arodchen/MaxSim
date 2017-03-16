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
import com.sun.max.lang.Classes;
import com.sun.max.unsafe.*;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.actor.holder.Hub;
import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.compiler.target.TargetMethod;
import com.sun.max.vm.intrinsics.MaxineIntrinsicIDs;
import com.sun.max.vm.layout.Layout;
import com.sun.max.vm.object.Hybrid;

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

    @INLINE
    public static void beginLoopFiltering(Address address) {
        maxsimMagicOp(Address.fromLong(MaxSimInterface.MaxSimMagicOpcodes.MAXSIM_M_OPC_FILTER_LOOP_BEGIN_VALUE), address);
    }

    @INLINE
    public static void endLoopFiltering() {
        MaxSimMediator.maxsimMagicOp(Address.fromLong(MaxSimInterface.MaxSimMagicOpcodes.MAXSIM_M_OPC_FILTER_LOOP_END_VALUE));
    }

    @INLINE
    public static void reportMaxSimHubTypeOffsetToZSim() {
        maxsimMagicOp(
            Address.fromLong(MaxSimInterface.MaxSimMagicOpcodes.MAXSIM_M_OPC_REPORT_HUB_TYPE_OFFSET_VALUE),
            Address.fromInt(Hub.maxsimHubTypeFieldOffset()));
    }

    @INLINE
    public static void reportArrayFirstElemOffsetToZSim() {
        maxsimMagicOp(
            Address.fromLong(MaxSimInterface.MaxSimMagicOpcodes.MAXSIM_M_OPC_REPORT_ARRAY_FIRST_ELEM_OFFSET_VALUE),
            Address.fromInt(Layout.charArrayLayout().getElementOffsetFromOrigin(0).toInt()));
    }

    @INLINE
    public static void registerAddressRange(Address lo, Address hi, int type) {
        maxsim_j_register_address_range(lo.toLong(), hi.toLong(), type);
    }

    @INLINE
    public static void deregisterAddressRange(Address lo, Address hi, int type) {
        maxsim_j_deregister_address_range(lo.toLong(), hi.toLong(), type);
    }

    public static void reportAllocationFrontierAddressRanges() {
        for (TargetMethod tm : MaxineVM.vm().compilationBroker.optimizingCompiler.getAllocationFrontierMethods()) {
            maxsim_j_register_address_range(tm.start().toLong(), tm.end().toLong(),
                MaxSimInterface.AddressRangeType.ALLOCATION_FRONTIER_ADDRESS_RANGE_VALUE);
        }

        for (TargetMethod tm : MaxineVM.vm().compilationBroker.baselineCompiler.getAllocationFrontierMethods()) {
            maxsim_j_register_address_range(tm.start().toLong(), tm.end().toLong(),
                MaxSimInterface.AddressRangeType.ALLOCATION_FRONTIER_ADDRESS_RANGE_VALUE);
        }

        TargetMethod tm = ClassMethodActor.fromJava(
            Classes.getDeclaredMethod(Object.class, "clone")).currentTargetMethod();
        maxsim_j_register_address_range(tm.start().toLong(), tm.end().toLong(),
            MaxSimInterface.AddressRangeType.ALLOCATION_FRONTIER_ADDRESS_RANGE_VALUE);

        tm = ClassMethodActor.fromJava(
            Classes.getDeclaredMethod(Hybrid.class, "expand", int.class)).currentTargetMethod();
        maxsim_j_register_address_range(tm.start().toLong(), tm.end().toLong(),
            MaxSimInterface.AddressRangeType.ALLOCATION_FRONTIER_ADDRESS_RANGE_VALUE);

        tm = ClassMethodActor.fromJava(
            Classes.getDeclaredMethod(Array.class, "newArray", Class.class, int.class)).currentTargetMethod();
        maxsim_j_register_address_range(tm.start().toLong(), tm.end().toLong(),
            MaxSimInterface.AddressRangeType.ALLOCATION_FRONTIER_ADDRESS_RANGE_VALUE);
    }
}
