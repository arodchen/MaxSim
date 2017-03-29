/*
 * Copyright (c) 2017 by Andrey Rodchenko, School of Computer Science,
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.max.vm.maxsim;

import com.sun.max.annotate.INLINE;
import com.sun.max.unsafe.Address;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.VMOptions;
import com.sun.max.vm.VMStringOption;
import com.sun.max.vm.runtime.*;

public class MaxSimPlatform {
    /**
     * Indicates whether MaxSim is in fast forwarding mode.
     */
    private static boolean isMaxSimFastForwarding = true;

    /**
     * Indicates whether MaxSim is in fast forwarding mode.
     */
    @INLINE
    public static boolean isMaxSimFastForwarding() {
        return isMaxSimFastForwarding;
    }

    /**
     * Indicator of the phase when pointer tagging is generative (pointer are being tagged).
     *
     * When set to false (during untagging) no new tags are introduced on allocation and pointer materialization
     * (e.g. InflatedMonitorLockword64#getBoundMonitor).
     */
    private static boolean isPointerTaggingGenerative = false;

    /**
     * Indicates whether tagging is active in MaxSim (tagged pointers are present).
     */
    @INLINE
    public static boolean isPointerTaggingActive() {
        return MaxSimInterfaceHelpers.isTaggingEnabled() && !isMaxSimFastForwarding;
    }

    /**
     * Indicates whether pointer tagging is generative in MaxSim (pointers are being tagged).
     */
    @INLINE
    public static boolean isPointerTaggingGenerative() {
        return MaxSimInterfaceHelpers.isTaggingEnabled() && isPointerTaggingGenerative;
    }

    /**
     * Instructs MaxSim to exit fast forwarding mode.
     */
    public static synchronized void exitMaxSimFastForwardingMode() {
        // report Maxine parameters
        MaxSimMediator.reportHubOffsetToZSim();
        MaxSimMediator.reportHubTypeOffsetToZSim();
        MaxSimMediator.reportArrayFirstElemOffsetToZSim();
        MaxSimMediator.reportAllocationFrontierAddressRanges();

        // exit ZSim fast forwarding mode
        MaxSimMediator.exitZSimFastForwardingMode();
        isMaxSimFastForwarding = false;

        if (isPointerTaggingActive()) {
            // do pointer tagging of all object pointers
            isPointerTaggingGenerative = true;
            MaxSimTaggingScheme.doTagging();
            // enable profiling collection
            if (isMaxSimProfiling()) {
                MaxSimMediator.enableProfileCollection(
                    MaxSimInterface.MaxineVMOperationMode.MAXINE_VM_OPERATION_MODE_RUNNING_NON_GC_VALUE);
            }
        }

        // dump ZSim eventual stats
        if (!isMaxSimProfiling()) {
            MaxSimMediator.dumpEventualStats(
                MaxSimInterface.MaxineVMOperationMode.MAXINE_VM_OPERATION_MODE_RUNNING_NON_GC_VALUE);
        }
    }

    /**
     * Instructs MaxSim to enter fast forwarding mode.
     */
    public static synchronized void enterMaxSimFastForwardingMode() {
        // dump ZSim eventual stats
        if (!isMaxSimProfiling()) {
            MaxSimMediator.dumpEventualStats(
                MaxSimInterface.MaxineVMOperationMode.MAXINE_VM_OPERATION_MODE_UNKNOWN_VALUE);
        }

        if (isPointerTaggingActive()) {
            // disable profiling collection
            if (isMaxSimProfiling()) {
                MaxSimMediator.disableProfileCollection(
                    MaxSimInterface.MaxineVMOperationMode.MAXINE_VM_OPERATION_MODE_UNKNOWN_VALUE);
            }

            // do pointer untagging of all object pointers
            isPointerTaggingGenerative = false;
            MaxSimTaggingScheme.doUntagging();
        }

        // enter ZSim fast forwarding mode
        isMaxSimFastForwarding = true;
        MaxSimMediator.enterZSimFastForwardingMode();
    }

    /**
     * Checks the validity of MaxSim configuration.
     */
    public static void checkConfiguration() {
        if (!MaxSimInterfaceHelpers.isMaxSimEnabled()) {
            return;
        }
        FatalError.check(Address.POINTER_TAG_MASK_SIZE <= Address.POINTER_TAG_MASK_MAX,
            "POINTER_TAG_MASK_SIZE is greater POINTER_TAG_MASK_MAX.");
    }

    /**
     * Do actions related to MaxSim when VM has just entered running phase.
     */
    public static void doMaxSimOnVMEnteringRunningPhase() {
        assert MaxineVM.isRunning();
        if (MaxSimExitFFOnVMEnter && isMaxSimFastForwarding) {
            exitMaxSimFastForwardingMode();
        }
    }

    /**
     * Do actions related to MaxSim when VM is about to exit running phase.
     */
    public static void doMaxSimOnVMExitingRunningPhase() {
        assert MaxineVM.isRunning() || MaxineVM.isStarting();
        if (MaxSimEnterFFOnVMExit && !isMaxSimFastForwarding) {
            enterMaxSimFastForwardingMode();
        }
        if (MaxSimPrintProfileOnVMExit) {
            MaxSimMediator.printProfileToFile(null);
        }
    }

    /**
     * Indicates whether MaxSim profiling information should be collected.
     */
    public static boolean isMaxSimProfiling() {
        return MaxSimInterfaceHelpers.isMaxSimEnabled() && MaxSimProfiling;
    }

    /**
     * Get MaxSim Maxine information builder.
     */
    public static MaxSimMaxineInfoBuilder getMaxSimMaxineInfoBuilder() {
        return MaxSimMaxineInfoBuilder;
    }

    /**
     * ZSim-Maxine information builder.
     */
    private static MaxSimMaxineInfoBuilder MaxSimMaxineInfoBuilder = new MaxSimMaxineInfoBuilder();

    public static boolean MaxSimPrintProfileOnVMExit;
    static {
        VMOptions.addFieldOption("-XX:", "MaxSimPrintProfileOnVMExit", MaxSimPlatform.class,
            "Makes MaxSim to print profiling information on VM exit (default: false).", MaxineVM.Phase.PRISTINE);
    }

    public static VMStringOption MaxSimMaxineInfoFileName = VMOptions.register(new VMStringOption("-XX:MaxSimMaxineInfoFileName=", false, "maxine-info.db",
        "MaxSim Maxine information file name"), MaxineVM.Phase.PRISTINE);

    public static VMStringOption MaxSimZSimProfileFileName = VMOptions.register(new VMStringOption("-XX:MaxSimZSimProfileFileName=", false, "zsim-prof.db",
        "MaxSim ZSim profile file name"), MaxineVM.Phase.PRISTINE);

    public static boolean MaxSimProfiling;
    static {
        VMOptions.addFieldOption("-XX:", "MaxSimProfiling", MaxSimPlatform.class,
            "Enables MaxSim profiling (default: false).", MaxineVM.Phase.PRISTINE);
    }

    public static boolean MaxSimExitFFOnVMEnter;
    static {
        VMOptions.addFieldOption("-XX:", "MaxSimExitFFOnVMEnter", MaxSimPlatform.class,
            "Makes MaxSim exit fast forwarding mode on VM enter (default: false).", MaxineVM.Phase.PRISTINE);
    }

    public static boolean MaxSimEnterFFOnVMExit;
    static {
        VMOptions.addFieldOption("-XX:", "MaxSimEnterFFOnVMExit", MaxSimPlatform.class,
            "Make MaxSim enter fast forwarding mode on VM exit (default: false).", MaxineVM.Phase.PRISTINE);
    }

    /**
     * Undefined address.
     */
    public static final long UNDEFINED_ADDRESS = -1L;

    /**
     * Layout scale factor equal to one.
     */
    public static final int LSF_ONE = 1;

    /**
     * Checks the validity of MaxSim configuration.
     */
    static {
        MaxSimPlatform.checkConfiguration();
    }
}
