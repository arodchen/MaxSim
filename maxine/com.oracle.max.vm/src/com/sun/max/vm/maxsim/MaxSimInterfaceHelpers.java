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

import com.sun.max.annotate.FOLD;

public class MaxSimInterfaceHelpers {

    @FOLD
    static public boolean isMaxSimEnabled() {
        return MaxSimInterface.MaxSimConfig.getDefaultInstance().getIsMaxSimEnabled();
    }

    @FOLD
    static public int getLayoutScaleFactor() {
        return MaxSimInterface.MaxSimConfig.getDefaultInstance().getLayoutScaleFactor();
    }

    @FOLD
    static public int getLayoutScaleRefFactor() {
        return MaxSimInterface.MaxSimConfig.getDefaultInstance().getLayoutScaleRefFactor();
    }

    @FOLD
    static public boolean isClassIDTagging() {
        return (MaxSimInterface.MaxSimConfig.getDefaultInstance().getPointerTaggingType() ==
            MaxSimInterface.PointerTaggingType.CLASS_ID_TAGGING);
    }

    static public boolean isClassIDTagging(MaxSimInterface.PointerTaggingType pointerTaggingType) {
        return (pointerTaggingType == MaxSimInterface.PointerTaggingType.CLASS_ID_TAGGING);
    }

    @FOLD
    static public boolean isAllocationSiteIDTagging() {
        return (MaxSimInterface.MaxSimConfig.getDefaultInstance().getPointerTaggingType() ==
            MaxSimInterface.PointerTaggingType.ALLOC_SITE_ID_TAGGING);
    }

    static public boolean isAllocationSiteIDTagging(MaxSimInterface.PointerTaggingType pointerTaggingType) {
        return (pointerTaggingType == MaxSimInterface.PointerTaggingType.ALLOC_SITE_ID_TAGGING);
    }

    @FOLD
    static public boolean isArrayLengthTagging() {
        return (MaxSimInterface.MaxSimConfig.getDefaultInstance().getPointerTaggingType() ==
            MaxSimInterface.PointerTaggingType.ARRAY_LENGTH_TAGGING);
    }

    static public boolean isArrayLengthTagging(MaxSimInterface.PointerTaggingType pointerTaggingType) {
        return (pointerTaggingType == MaxSimInterface.PointerTaggingType.ARRAY_LENGTH_TAGGING);
    }
}
