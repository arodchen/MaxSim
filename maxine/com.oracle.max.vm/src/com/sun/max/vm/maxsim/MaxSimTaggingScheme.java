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

import com.sun.max.annotate.INLINE;
import com.sun.max.unsafe.*;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.object.ObjectAccess;

/**
 * Maxsim tagging scheme.
 */
public class MaxSimTaggingScheme {

    /**
     * Do tagging.
     */
    static public void doTagging() {
    }

    /**
     * Do untagging.
     */
    static public void doUntagging() {
    }

    /**
     * Comparison of untagged objects.
     *
     * NOTE: The places for insertion of untagged comparison were identified empirically.
     * NOTE: All the code dynamically reachable from doTagging should perform untagged pointers comparison.
     *
     * Returns true if objects are equal ignoring tags,false otherwise.
     */
    @INLINE
    public static boolean compareUntaggedObjects(Object object1, Object object2) {
        if (MaxSimInterfaceHelpers.isTaggingEnabled() && !MaxineVM.isHosted()) {
            return Pointer.equalsUntagged(ObjectAccess.toOrigin(object1), ObjectAccess.toOrigin(object2));
        } else {
            return object1 == object2;
        }
    }

    /**
     * Converts class ID to tag.
     */
    @INLINE
    public static short classIDToTag(int classId) {
        if (MaxineVM.isDebug() && (classId < 0)) {
            FatalError.unexpected("Class ID passed to classIDToTag should not be negative!");
        }
        int tag = classId + MaxSimInterface.PointerTag.TAG_GP_LO_VALUE;
        if ((tag >= MaxSimInterface.PointerTag.DEFINED_TAGS_NUM_VALUE)) {
            return MaxSimInterface.PointerTag.TAG_UNDEFINED_GP_VALUE;
        }
        return (short) tag;
    }

    /**
     * Defines MaxSim tag associated with a hub.
     */
    static public short defineMaxSimHubTag(Hub hub) {
        if (hub.isStatic) {
            return MaxSimInterface.PointerTag.TAG_STATIC_VALUE;
        } else {
            return classIDToTag(hub.classActor.id);
        }
    }
}
