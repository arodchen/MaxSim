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

import java.util.Comparator;

public class MaxSimInterfaceComparators {

    /**
     * Class profiling information sorting type.
     */
    public enum ClassProfSortingType {
        MemFootprintDescendingOrder,
        MemAccCountDescendingOrder;
    }

    /**
     * Field profiling information sorting type.
     */
    public enum FieldProfSortingType {
        OffsetAscendingOrder,
        MemAccCountDescendingOrder;
    }

    /**
     * Allocation profiling information sorting type.
     */
    public enum AllocProfSortingType {
        MemFootprintDescendingOrder;
    }

    /**
     * Allocation site profiling information sorting type.
     */
    public enum AllocSiteProfSortingType {
        CountDescendingOrder
    }

    /**
     * Cache miss profiling information sorting type.
     */
    public enum CacheMissProfSortingType {
        CountDescendingOrder
    }

    /**
     * Comparator to sort {@link MaxSimInterface.OffsetBCIPair}s in offset ascending order.
     */
    public static Comparator<MaxSimInterface.OffsetBCIPair> OffsetBCIPairOffsetAscendingCmp =
            new Comparator<MaxSimInterface.OffsetBCIPair>() {
                @Override
                public int compare(MaxSimInterface.OffsetBCIPair e1, MaxSimInterface.OffsetBCIPair e2) {
                    int e1Offset = e1.getOffset();
                    int e2Offset = e2.getOffset();
                    if (e1Offset != e2Offset) {
                        return e2Offset < e1Offset ? 1 : -1;
                    }
                    return 0;
                }
            };

    /**
     * Comparator to sort {@link MaxSimInterface.MethodInfo}s in begin IP ascending order.
     */
    public static Comparator<MaxSimInterface.MethodInfo> MethodInfoBeginIPAscendingCmp =
            new Comparator<MaxSimInterface.MethodInfo>() {
                @Override
                public int compare(MaxSimInterface.MethodInfo e1, MaxSimInterface.MethodInfo e2) {
                    long e1BeginIP = e1.getBeginIP();
                    long e2BeginIP = e2.getBeginIP();
                    if (e1BeginIP != e2BeginIP) {
                        return e2BeginIP < e1BeginIP ? 1 : -1;
                    }
                    return 0;
                }
            };

    /**
     * Comparator to sort {@link MaxSimInterface.ClassProf}s in memory accesses count descending order.
     */
    public static Comparator<MaxSimInterface.ClassProf> ClassProfMemAccCountDescendingCmp =
            new Comparator<MaxSimInterface.ClassProf>() {
                @Override
                public int compare(MaxSimInterface.ClassProf e1, MaxSimInterface.ClassProf e2) {
                    long e1MemAccCount = e1.getMemAccCount();
                    long e2MemAccCount = e2.getMemAccCount();
                    if (e1MemAccCount != e2MemAccCount) {
                        return e1MemAccCount < e2MemAccCount ? 1 : -1;
                    }
                    return 0;
                }
            };

    /**
     * Comparator to sort {@link MaxSimInterface.ClassProf}s in memory footprint descending order.
     */
    public static Comparator<MaxSimInterface.ClassProf> ClassProfMemFootprintDescendingCmp =
            new Comparator<MaxSimInterface.ClassProf>() {
                @Override
                public int compare(MaxSimInterface.ClassProf e1, MaxSimInterface.ClassProf e2) {
                    long e1MemFootprint = e1.getMemAllSize();
                    long e2MemFootprint = e2.getMemAllSize();

                    if (e1MemFootprint != e2MemFootprint) {
                        return e1MemFootprint < e2MemFootprint ? 1 : -1;
                    }
                    return 0;
                }
            };

    /**
     * Comparator to sort {@link MaxSimInterface.ClassProf}s in memory allocations count descending order.
     */
    public static Comparator<MaxSimInterface.ClassProf> ClassProfMemAllCountDescendingCmp =
            new Comparator<MaxSimInterface.ClassProf>() {
                @Override
                public int compare(MaxSimInterface.ClassProf e1, MaxSimInterface.ClassProf e2) {
                    long e1Allocations = e1.getMemAllCount();
                    long e2Allocations = e2.getMemAllCount();

                    if (e1Allocations != e2Allocations) {
                        return e1Allocations < e2Allocations ? 1 : -1;
                    }
                    return 0;
                }
            };

    /**
     * Comparator to sort {@link MaxSimInterface.FieldProf}s in offset ascending order.
     */
    public static Comparator<MaxSimInterface.FieldProf> FieldProfOffsetAscendingCmp =
            new Comparator<MaxSimInterface.FieldProf>() {
                @Override
                public int compare(MaxSimInterface.FieldProf e1, MaxSimInterface.FieldProf e2) {
                    int e1Offset = e1.getOffset();
                    int e2Offset = e2.getOffset();

                    assert e1Offset != e2Offset;
                    return e2Offset < e2Offset ? -1 : +1;
                }
            };

    /**
     * Comparator to sort {@link MaxSimInterface.FieldProf}s in memory accesses count ascending order.
     */
    public static Comparator<MaxSimInterface.FieldProf> FieldProfMemAccCountDescendingCmp =
            new Comparator<MaxSimInterface.FieldProf>() {
                @Override
                public int compare(MaxSimInterface.FieldProf e1, MaxSimInterface.FieldProf e2) {
                    long e1MemAccCount = e1.getReadCount() + e1.getWriteCount();
                    long e2MemAccCount = e2.getReadCount() + e2.getWriteCount();

                    if (e1MemAccCount != e2MemAccCount) {
                        return e1MemAccCount < e2MemAccCount ? +1 : -1;
                    }
                    return 0;
                }
            };

    /**
     * Comparator to sort {@link MaxSimInterface.AllocProf}s in memory footprint descending order.
     */
    public static Comparator<MaxSimInterface.AllocProf> AllocProfMemFootprintDescendingCmp =
            new Comparator<MaxSimInterface.AllocProf>() {
                @Override
                public int compare(MaxSimInterface.AllocProf e1, MaxSimInterface.AllocProf e2) {
                    long e1MemFootprint = ((long) e1.getSize()) * e1.getCount();
                    long e2MemFootprint = ((long) e2.getSize()) * e2.getCount();

                    if (e1MemFootprint != e2MemFootprint) {
                        return e1MemFootprint < e2MemFootprint ? 1 : -1;
                    }
                    return 0;
                }
            };

    /**
     * Comparator to sort {@link MaxSimInterface.CacheMissProf}s in count descending order.
     */
    public static Comparator<MaxSimInterface.CacheMissProf> CacheMissProfCountDescendingCmp =
            new Comparator<MaxSimInterface.CacheMissProf>() {
                @Override
                public int compare(MaxSimInterface.CacheMissProf e1, MaxSimInterface.CacheMissProf e2) {
                    long e1MemAccCount = e1.getCount();
                    long e2MemAccCount = e2.getCount();
                    if (e1MemAccCount != e2MemAccCount) {
                        return e1MemAccCount < e2MemAccCount ? 1 : -1;
                    }
                    return 0;
                }
            };

    /**
     * Comparator to sort {@link MaxSimInterface.AllocSiteProf}s in count descending order.
     */
    public static Comparator<MaxSimInterface.AllocSiteProf> AllocSiteProfCountDescendingCmp =
            new Comparator<MaxSimInterface.AllocSiteProf>() {
                @Override
                public int compare(MaxSimInterface.AllocSiteProf e1, MaxSimInterface.AllocSiteProf e2) {
                    long e1AllocationsCount = e1.getCount();
                    long e2AllocationsCount = e2.getCount();
                    if (e1AllocationsCount != e2AllocationsCount) {
                        return e1AllocationsCount < e2AllocationsCount ? 1 : -1;
                    }
                    return 0;
                }
            };

    /**
     * Get comparator by class profiling information sorting type.
     */
    public static Comparator<MaxSimInterface.ClassProf> getClassProfComparatorBySortingType(ClassProfSortingType t) {
        switch (t) {
            case MemFootprintDescendingOrder:
                return ClassProfMemFootprintDescendingCmp;
            case MemAccCountDescendingOrder:
                return ClassProfMemAccCountDescendingCmp;
            default:
                assert false;
                return null;
        }
    }

    /**
     * Get comparator by field profiling information sorting type.
     */
    public static Comparator<MaxSimInterface.FieldProf> getFieldProfComparatorBySortingType(FieldProfSortingType t) {
        switch (t) {
            case MemAccCountDescendingOrder:
                return MaxSimInterfaceComparators.FieldProfMemAccCountDescendingCmp;
            case OffsetAscendingOrder:
                return MaxSimInterfaceComparators.FieldProfOffsetAscendingCmp;
            default:
                assert false;
                return null;
        }
    }

    /**
     * Get comparator by allocation profiling information sorting type.
     */
    public static Comparator<MaxSimInterface.AllocProf> getAllocProfComparatorBySortingType(AllocProfSortingType t) {
        switch (t) {
            case MemFootprintDescendingOrder:
                return AllocProfMemFootprintDescendingCmp;
            default:
                assert false;
                return null;
        }
    }

    /**
     * Get comparator by allocation site profiling information sorting type.
     */
    public static Comparator<MaxSimInterface.AllocSiteProf> getAllocSiteProfComparatorBySortingType(
        AllocSiteProfSortingType t) {
        switch (t) {
            case CountDescendingOrder:
                return MaxSimInterfaceComparators.AllocSiteProfCountDescendingCmp;
            default:
                assert false;
                return null;
        }
    }

    /**
     * Get comparator by cache miss profiling information sorting type.
     */
    public static Comparator<MaxSimInterface.CacheMissProf> getCacheMissProfComparatorBySortingType(
        CacheMissProfSortingType t) {
        switch (t) {
            case CountDescendingOrder:
                return MaxSimInterfaceComparators.CacheMissProfCountDescendingCmp;
            default:
                assert false;
                return null;
        }
    }
}
