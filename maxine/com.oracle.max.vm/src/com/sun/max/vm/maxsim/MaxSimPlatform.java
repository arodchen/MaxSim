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
        }

        // dump ZSim eventual stats
        MaxSimMediator.dumpEventualStats(
            MaxSimInterface.MaxineVMOperationMode.MAXINE_VM_OPERATION_MODE_RUNNING_NON_GC_VALUE);
    }

    /**
     * Instructs MaxSim to enter fast forwarding mode.
     */
    public static synchronized void enterMaxSimFastForwardingMode() {
        // dump ZSim eventual stats
        MaxSimMediator.dumpEventualStats(
            MaxSimInterface.MaxineVMOperationMode.MAXINE_VM_OPERATION_MODE_UNKNOWN_VALUE);

        if (isPointerTaggingActive()) {
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
        if (MaxSimInterfaceHelpers.isMaxSimEnabled() && MaxSimExitFFOnVMEnter && isMaxSimFastForwarding) {
            exitMaxSimFastForwardingMode();
        }
    }

    /**
     * Do actions related to MaxSim when VM is about to exit running phase.
     */
    public static void doMaxSimOnVMExitingRunningPhase() {
        assert MaxineVM.isRunning() || MaxineVM.isStarting();
        if (MaxSimInterfaceHelpers.isMaxSimEnabled() && MaxSimEnterFFOnVMExit && !isMaxSimFastForwarding) {
            enterMaxSimFastForwardingMode();
        }
    }

    public static boolean MaxSimExitFFOnVMEnter;
    static {
        VMOptions.addFieldOption("-XX:", "MaxSimExitFFOnVMEnter", MaxSimPlatform.class,
            "Make MaxSim exit fast forwarding mode on VM enter (default: false).", MaxineVM.Phase.PRISTINE);
    }

    public static boolean MaxSimEnterFFOnVMExit;
    static {
        VMOptions.addFieldOption("-XX:", "MaxSimEnterFFOnVMExit", MaxSimPlatform.class,
            "Make MaxSim enter fast forwarding mode on VM exit (default: false).", MaxineVM.Phase.PRISTINE);
    }

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
