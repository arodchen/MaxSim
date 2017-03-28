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

import com.sun.max.program.option.Option;
import com.sun.max.program.option.OptionSet;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MaxSimProfilePrinter {

    private static final OptionSet options = new OptionSet();

    private static final Option<String> ZSimProfileDBOption = options.newStringOption(
        MaxSimProfileRepresentation.getZSimProfileDBOptionName(), null,
        MaxSimProfileRepresentation.getZSimProfileDBOptionHelp());

    private static final Option<String> MaxineInfoDBOption = options.newStringOption(
        MaxSimProfileRepresentation.getMaxineInfoDBOptionName(), null,
        MaxSimProfileRepresentation.getMaxineInfoDBOptionHelp());

    private static final Option<String> OutputFileNameOption = options.newStringOption(
        "o", "maxsim-prof.txt", "Output file name.");

    private static final Option<Boolean> help = options.newBooleanOption("help", false, "Show help message and exit.");

    private static final String classIdShortName = new String("ci");

    private static final String memAccCountShortName = new String("ac");

    private static final String memAllCountShortName = new String("al");

    private static final String arrayLengthShortName = new String("al");

    private static final String objRelFinRefName = new String("orfr");

    private static final String allocationSiteIdShortName = new String("asi");

    private static final String fieldPropertyShortName = new String("p");

    private static final String classPropertyShortName = new String("cp");

    private static final String finalPropShortName = new String("f");

    private static final String objRefPropShortName = new String("a");

    private static final String offsetShortName = new String("o");

    private static final String readShortName = new String("r");

    private static final String writeShortName = new String("w");

    private static final String countShortName = new String("c");

    private static final String versionShortName = new String("v");

    private static final String missCountShortName = new String("m");

    private static final String idShortName = new String("i");

    private static final String offsetLoShortName = new String("ol");

    private static final String offsetHiShortName = new String("oh");

    private static final String begShortName = new String("b");

    private static final String endShortName = new String("e");

    private static final String frequencyShortName = new String("f");

    private static final String memFootprintShortName = new String("mf");

    private static final String classComponentIndexShortName = new String("cci");

    private static final String byteCodeIndexShortName = new String("bci");

    private static final String hexPrefixName = new String("0x");

    private static final String undefinedFunctionName = new String("undefined");

    private static final String methodKindShortName = new String("k");

    private static final String methodKindOptimizedName = new String("O");

    private static final String methodKindBootName = new String("I");

    private static final String methodKindBaselineName = new String("B");

    private static final String methodKindNativeName = new String("N");

    private final static String coldPointerFieldName = new String("<cp>");

    private final static String nullCheckFieldName = new String("<nc>");

    private static final DecimalFormat decimalFormat = new DecimalFormat("#.00");

    private final MaxSimInterfaceComparators.ClassProfSortingType classProfSortingType =
            MaxSimInterfaceComparators.ClassProfSortingType.MemFootprintDescendingOrder;

    private final MaxSimInterfaceComparators.FieldProfSortingType fieldProfSortingType =
            MaxSimInterfaceComparators.FieldProfSortingType.MemAccCountDescendingOrder;

    private final MaxSimInterfaceComparators.AllocProfSortingType allocProfSortingType =
            MaxSimInterfaceComparators.AllocProfSortingType.MemFootprintDescendingOrder;

    private final MaxSimInterfaceComparators.AllocSiteProfSortingType allocSiteProfSortingType =
            MaxSimInterfaceComparators.AllocSiteProfSortingType.CountDescendingOrder;

    private final MaxSimInterfaceComparators.CacheMissProfSortingType cacheMissProfSortingType =
            MaxSimInterfaceComparators.CacheMissProfSortingType.CountDescendingOrder;

    private final MaxSimProfileRepresentation MaxSimProfRep = new MaxSimProfileRepresentation();

    private long taggedGPObjMemFootprint = 0;
    private long taggedGPObjMemAllCount = 0;
    private long taggedGPObjMemAccCount = 0;
    private long taggedGPObjReadCount = 0;
    private long taggedGPObjWriteCount = 0;
    private long [] taggedGPObjCacheRWGroupMissCount;

    public static void main(String[] programArguments) {
        options.parseArguments(programArguments);
        if (help.getValue() || programArguments.length == 0) {
            options.printHelp(System.out, 80);
            return;
        }

        MaxSimProfilePrinter MaxSimProfilePrinter = new MaxSimProfilePrinter();
        MaxSimProfilePrinter.loadAndPrint(ZSimProfileDBOption.getValue(), MaxineInfoDBOption.getValue());

        return;
    }

    /**
     * Loads and prints profiling information.
     */
    public void loadAndPrint(String ZSimProfileDBFileName, String MaxineInfoDBFileName) {
        MaxSimProfRep.loadZSimProfileAndMaxineInfoDBs(ZSimProfileDBFileName, MaxineInfoDBFileName);
        if (MaxSimProfRep.isProfileLoaded()) {
            try {
                PrintWriter MaxSimProfOut = new PrintWriter(OutputFileNameOption.getValue());
                print(MaxSimProfOut);
                MaxSimProfOut.close();
            } catch (Exception e) {
                System.out.println("Could not print profiling information. Exception thrown: " + e.toString());
            }
        } else {
            System.out.println("Profile loading failed!");
            return;
        }
        MaxSimProfRep.unloadZSimProfileAndMaxineInfoDBs();
    }

    /**
     * Prints allocation site profiling information.
     */
    private void printAllocSiteProfs(PrintWriter MaxSimProfOut) {
        ArrayList<MaxSimInterface.AllocSiteProf> allocSiteProfs =
            new ArrayList<MaxSimInterface.AllocSiteProf>(MaxSimProfRep.getZSimProfileDB().getAllocSiteProfList());

        Comparator<MaxSimInterface.AllocSiteProf> allocationSiteProfComparator =
             MaxSimInterfaceComparators.getAllocSiteProfComparatorBySortingType(allocSiteProfSortingType);

        Collections.sort(allocSiteProfs, allocationSiteProfComparator);

        MaxSimProfOut.println("=== Allocation Sites ===");
        for (MaxSimInterface.AllocSiteProf allocationSiteEntry : allocSiteProfs) {
            int id = allocationSiteEntry.getId();
            long IP = allocationSiteEntry.getIP();
            int classId = allocationSiteEntry.getClassId();

            MaxSimInterface.MethodInfo methodInfo = MaxSimProfRep.getMethodInfoByIP(IP);
            MaxSimInterface.ClassInfo classInfo = MaxSimProfRep.getClassInfoByProfId(classId);

            printIPInfo(MaxSimProfOut, methodInfo, IP);
            MaxSimProfOut.print("(");
            MaxSimProfOut.print(allocationSiteIdShortName + ":" + id);
            MaxSimProfOut.print(" ");
            MaxSimProfOut.print(classIdShortName + ":" + classId);
            MaxSimProfOut.print(" ");
            MaxSimProfOut.print(countShortName + ":" + allocationSiteEntry.getCount());
            MaxSimProfOut.println(")");
        }
        MaxSimProfOut.println("");
    }

    /**
     * Prints class name before method name.
     */
    private void printClassNameBeforeMethodName(PrintWriter MaxSimProfOut, MaxSimInterface.MethodInfo methodInfo) {
        if (methodInfo.hasClassId()) {
            int classId = methodInfo.getClassId();
            MaxSimInterface.ClassInfo classInfo = MaxSimProfRep.getClassInfoByClassId(classId);
            if (classInfo != null) {
                MaxSimProfOut.print(DescriptorPrinter.typeDescriptorToName(classInfo.getDesc(), true, true));
                MaxSimProfOut.print(".");
            }
        }
    }

    /**
     * Prints instruction pointer information.
     */
    private void printIPInfo(PrintWriter MaxSimProfOut, MaxSimInterface.MethodInfo methodInfo, long ip) {
        MaxSimProfOut.print("[");
        if (methodInfo != null) {
            printClassNameBeforeMethodName(MaxSimProfOut, methodInfo);
            MaxSimProfOut.print(methodInfo.getName());
            if (!methodInfo.getDesc().isEmpty()) {
                MaxSimProfOut.print(DescriptorPrinter.methodDescriptorToName(methodInfo.getDesc(), false));
            }
            MaxSimProfOut.print("+" + (ip - methodInfo.getBeginIP()));
            MaxSimProfOut.print("(" + methodKindShortName + ":");
            switch (methodInfo.getKind()) {
                default:
                    assert false : "invalid method kind:" + methodInfo.getKind();
                    break;
                case OPTIMIZED:
                    MaxSimProfOut.print(methodKindOptimizedName);
                    break;
                case BOOT:
                    MaxSimProfOut.print(methodKindBootName);
                    break;
                case BASELINE:
                    MaxSimProfOut.print(methodKindBaselineName);
                    break;
                case NATIVE:
                    MaxSimProfOut.print(methodKindNativeName);
                    break;
            }
            MaxSimProfOut.print(" " + byteCodeIndexShortName + ":" + MaxSimProfRep.getBCIByIP(ip));
            MaxSimProfOut.print(")");
        } else if (ip == MaxSimPlatform.UNDEFINED_ADDRESS) {
            MaxSimProfOut.print(undefinedFunctionName);
        } else {
            MaxSimProfOut.print(hexPrefixName + Long.toHexString(ip));
            MaxSimProfOut.print("(" + methodKindShortName + ":" + methodKindNativeName + ")");
        }
        MaxSimProfOut.print("]");
    }

    /**
     * Prints cache miss information from a list.
     */
    private void printCacheMissInfoFromList(PrintWriter MaxSimProfOut,
                                            ArrayList<MaxSimInterface.CacheMissProf> cacheMissProfList) {
        for (MaxSimInterface.CacheMissProf cacheMissEntry : cacheMissProfList) {
            long IP = cacheMissEntry.getIP();
            int classId = cacheMissEntry.getClassId();
            long missCount = cacheMissEntry.getCount();
            MaxSimInterface.MethodInfo methodInfo = MaxSimProfRep.getMethodInfoByIP(IP);
            MaxSimInterface.ClassInfo classInfo = MaxSimProfRep.getClassInfoByProfId(classId);

            printIPInfo(MaxSimProfOut, methodInfo, IP);
            MaxSimProfOut.print("(");
            MaxSimProfOut.print(missCountShortName +":" + missCount);
            MaxSimProfOut.print(" ");
            MaxSimProfOut.print(idShortName + ":" + classId);
            MaxSimProfOut.print(" ");
            MaxSimProfOut.print(offsetLoShortName + ":" + cacheMissEntry.getOffsetLo());
            MaxSimProfOut.print(" ");
            MaxSimProfOut.print(offsetHiShortName + ":" + cacheMissEntry.getOffsetHi());
            MaxSimProfOut.println(")");
        }
    }

    /**
     * Prints cache misses information.
     */
    private void printCacheMisses(PrintWriter MaxSimProfOut) {
        for (int i = 0; i < MaxSimProfRep.getZSimProfileDB().getCacheRWGroupMissProfCount(); i++) {
            MaxSimInterface.CacheRWGroupInfo cacheRWGroupInfo = MaxSimProfRep.getCacheRWGroupInfoById(i);

            MaxSimInterface.CacheRWGroupMissProf CacheRWGroupMissProfEntry =
                MaxSimProfRep.getZSimProfileDB().getCacheRWGroupMissProf(i);

            ArrayList<MaxSimInterface.CacheMissProf> cacheMissProfList =
                new ArrayList<MaxSimInterface.CacheMissProf>(CacheRWGroupMissProfEntry.getCacheMissProfList());

            Comparator<MaxSimInterface.CacheMissProf> cacheMissEntryComparator =
                MaxSimInterfaceComparators.getCacheMissProfComparatorBySortingType(cacheMissProfSortingType);

            Collections.sort(cacheMissProfList, cacheMissEntryComparator);

            MaxSimProfOut.println("=== " + cacheRWGroupInfo.getCacheGroupName() +
                (cacheRWGroupInfo.getIsWrite() ? writeShortName : readShortName) + " Cache Misses ===");
            printCacheMissInfoFromList(MaxSimProfOut, cacheMissProfList);
            MaxSimProfOut.println("");
        }
    }

    /**
     * Prints boot code region information.
     */
    private void printBootCodeRegInfo(PrintWriter MaxSimProfOut) {
        MaxSimInterface.MethodInfo bootCodeRegion = MaxSimProfRep.getMaxineInfoDB().getBootCodeRegInfo();
        long begIP = bootCodeRegion.getBeginIP();
        long endIP = bootCodeRegion.getBeginIP() + bootCodeRegion.getSize();
        MaxSimProfOut.println(bootCodeRegion.getName() + "(" +
            begShortName + ":" + hexPrefixName + Long.toHexString(begIP) + " " +
            endShortName + ":" + hexPrefixName + Long.toHexString(endIP) + ")");
    }

    /**
     * Initializes printing.
     */
    private void printInit() {
        taggedGPObjCacheRWGroupMissCount = new long [MaxSimProfRep.getCacheRWGroupsNum()];
    }

    /**
     * Finalizes printing.
     */
    private void printFini() {
        taggedGPObjCacheRWGroupMissCount = null;
    }

    /**
     * Prints profiling information.
     */
    private void print(PrintWriter MaxSimProfOut) {
        printInit();
        printClassProfInfo(MaxSimProfOut);
        printAllocSiteProfs(MaxSimProfOut);
        printCacheMisses(MaxSimProfOut);
        printFini();
    }

    /**
     * Prints tagged GP object summary information.
     */
    private void printTaggedGPObjSummaryInfo(PrintWriter MaxSimProfOut) {
        MaxSimProfOut.print("TaggedGPMemoryAccesses.Overall" + "(" +
            memFootprintShortName + ":" + taggedGPObjMemFootprint + " " +
            memAllCountShortName + ":" + taggedGPObjMemAllCount + " " +
            memAccCountShortName + ":" + taggedGPObjMemAccCount + " " +
            memAccCountShortName + "/" + memFootprintShortName + ":" +
            decimalFormat.format((double) taggedGPObjMemAccCount / (double) taggedGPObjMemFootprint) + " " +
            readShortName + ":" + taggedGPObjReadCount + " " +
            writeShortName + ":" + taggedGPObjWriteCount);

            for (int i = 0; i < MaxSimProfRep.getCacheRWGroupsNum(); i++) {
                MaxSimInterface.CacheRWGroupInfo info = MaxSimProfRep.getCacheRWGroupInfoById(i);
                MaxSimProfOut.print(" " + info.getCacheGroupName() + (info.getIsWrite() ? writeShortName : readShortName) +
                    missCountShortName + ":" + taggedGPObjCacheRWGroupMissCount[i]);
            }
        MaxSimProfOut.println(")");
    }

    /**
     * Prints class profiling information cache miss summary for field profiling information list.
     */
    private void printCacheMissSummaryForFieldProfList(PrintWriter MaxSimProfOut,
                                                       ArrayList<MaxSimInterface.FieldProf> fieldProfArrayList) {
        long [] classCacheRWGroupMissCount = new long [MaxSimProfRep.getCacheRWGroupsNum()];

        for (MaxSimInterface.FieldProf fieldEntry : fieldProfArrayList) {
            for (int i = 0; i < MaxSimProfRep.getCacheRWGroupsNum(); i++) {
                classCacheRWGroupMissCount[i] += fieldEntry.getCacheRWGroupMissCount(i);
            }
        }
        for (int i = 0; i < MaxSimProfRep.getCacheRWGroupsNum(); i++) {
            MaxSimInterface.CacheRWGroupInfo info = MaxSimProfRep.getCacheRWGroupInfoById(i);
            MaxSimProfOut.print(((i == 0) ? "" : " ") + info.getCacheGroupName() +
                (info.getIsWrite() ? writeShortName : readShortName) + missCountShortName + ":" +
                classCacheRWGroupMissCount[i]);
        }
    }

    /**
     * Prints field profiling information name.
     */
    private void printFieldProfName(PrintWriter MaxSimProfOut,
                                    MaxSimInterface.FieldProf fieldProf,
                                    MaxSimInterface.ClassInfo classInfo) {
        if (classInfo != null) {
            MaxSimInterface.FieldInfo fieldInfo = MaxSimProfRep.getFieldInfoByOffset(classInfo, fieldProf.getOffset());
            if (fieldInfo != null) {
                MaxSimProfOut.print(fieldInfo.getName());
            } else if (fieldProf.getOffset() == MaxSimProfRep.getMaxineInfoDB().getNullCheckOffset()) {
                MaxSimProfOut.print(nullCheckFieldName);
            }
        }
    }

    /**
     * Prints field info properties.
     */
    private void printFieldPropertiesAndClassId(PrintWriter MaxSimProfOut,
                                                MaxSimInterface.FieldInfo fieldInfo) {
        if (fieldInfo != null) {
            int fieldClassId = fieldInfo.getClassId();
            MaxSimInterface.ClassInfo fieldClassInfo = MaxSimProfRep.getClassInfoByClassId(fieldClassId);
            if (fieldClassInfo != null) {
                if (fieldInfo.getPropertyList().contains(MaxSimInterface.FieldInfo.Property.FINAL)) {
                    if (fieldClassInfo.getKind() == MaxSimInterface.ClassInfo.Kind.PRIMITIVE) {
                        MaxSimProfOut.print(" " + fieldPropertyShortName + ":" + finalPropShortName);
                        MaxSimProfOut.print(java.lang.Character.toLowerCase(fieldClassInfo.getDesc().charAt(0)));
                    } else if (fieldClassInfo.getKind() != MaxSimInterface.ClassInfo.Kind.OTHER) {
                        MaxSimProfOut.print(
                            " " + fieldPropertyShortName + ":" + finalPropShortName + objRefPropShortName);
                    }
                } else {
                    if ((fieldClassInfo.getKind() != MaxSimInterface.ClassInfo.Kind.PRIMITIVE) &&
                        (fieldClassInfo.getKind() != MaxSimInterface.ClassInfo.Kind.OTHER)) {
                        MaxSimProfOut.print(" " + fieldPropertyShortName + ":" + objRefPropShortName);
                    }
                }
            }
            MaxSimProfOut.print(" " + classIdShortName + ":" + fieldClassId);
        }
    }

    /**
     * Prints field profiling information list.
     */
    private void printFieldProfList(PrintWriter MaxSimProfOut,
                                    MaxSimInterface.ClassProf classProf,
                                    ArrayList<MaxSimInterface.FieldProf> fieldProfArrayList) {
        int classEntryId = classProf.getId();
        long memAccCount = classProf.getMemAccCount();
        boolean isTaggedGPClass = !MaxSimInterfaceHelpers.isAggregateTag((short) classEntryId);
        MaxSimInterface.ClassInfo classInfo = MaxSimProfRep.getClassInfoByProfId(classEntryId);

        for (MaxSimInterface.FieldProf fieldEntry : fieldProfArrayList) {
            long readCount = fieldEntry.getReadCount();
            long writeCount = fieldEntry.getWriteCount();
            long readAndWriteCount = readCount + writeCount;
            MaxSimInterface.FieldInfo fieldInfo = MaxSimProfRep.getFieldInfoByOffset(classInfo, fieldEntry.getOffset());
            long [] fieldCacheRWGroupMissCount = new long [MaxSimProfRep.getCacheRWGroupsNum()];

            for (int i = 0; i < MaxSimProfRep.getCacheRWGroupsNum(); i++) {
                fieldCacheRWGroupMissCount[i] = fieldEntry.getCacheRWGroupMissCount(i);
            }

            if (isTaggedGPClass || (MaxSimInterface.PointerTag.TAG_UNDEFINED_GP_VALUE == classEntryId)) {
                taggedGPObjReadCount += readCount;
                taggedGPObjWriteCount += writeCount;

                for (int i = 0; i < MaxSimProfRep.getCacheRWGroupsNum(); i++) {
                    taggedGPObjCacheRWGroupMissCount[i] += fieldCacheRWGroupMissCount[i];
                }
            }

            MaxSimProfOut.print(" ");
            printFieldProfName(MaxSimProfOut, fieldEntry, classInfo);

            MaxSimProfOut.print("(" +
                offsetShortName + ":" + fieldEntry.getOffset());
            printFieldPropertiesAndClassId(MaxSimProfOut, fieldInfo);
            MaxSimProfOut.print(" " +
                readShortName + ":" + readCount + " " +
                writeShortName + ":" + writeCount);
                for (int i = 0; i < MaxSimProfRep.getCacheRWGroupsNum(); i++) {
                    MaxSimInterface.CacheRWGroupInfo info = MaxSimProfRep.getCacheRWGroupInfoById(i);
                    MaxSimProfOut.print(" " + info.getCacheGroupName() +
                        (info.getIsWrite() ? writeShortName : readShortName) + missCountShortName + ":" +
                        fieldCacheRWGroupMissCount[i]);
                }
            MaxSimProfOut.print(" " + frequencyShortName + ":" +
                decimalFormat.format((double) readAndWriteCount / (double) memAccCount));
            MaxSimProfOut.print(")");
        }
    }

    /**
     * Prints tagged aggregate class profiling information.
     */
    private void printTaggedAggregateClassProf(PrintWriter MaxSimProfOut, MaxSimInterface.ClassProf classProf) {
        int classEntryId = classProf.getId();
        short tag = (short) classEntryId;
        long memoryFootprint = classProf.getMemAllSize();
        long memAccCount = classProf.getMemAccCount();
        long memAllCount = classProf.getMemAllCount();

        if (MaxSimInterface.PointerTag.TAG_UNDEFINED_GP_VALUE == tag) {
            taggedGPObjMemFootprint += memoryFootprint;
            taggedGPObjMemAccCount += memAccCount;
            taggedGPObjMemAllCount += memAllCount;
        }

        ArrayList<MaxSimInterface.AllocProf> memAllocProfArrayList =
            new ArrayList<MaxSimInterface.AllocProf>(classProf.getAllocProfList());

        Comparator<MaxSimInterface.AllocProf> memoryAllocationEntryComparator =
            MaxSimInterfaceComparators.getAllocProfComparatorBySortingType(allocProfSortingType);

        Collections.sort(memAllocProfArrayList, memoryAllocationEntryComparator);

        assert MaxSimInterfaceHelpers.isAggregateTag(tag);

        ArrayList<MaxSimInterface.FieldProf> fieldProfArrayList =
            new ArrayList<MaxSimInterface.FieldProf>(classProf.getFieldProfList());

        Comparator<MaxSimInterface.FieldProf> fieldProfComparator =
            MaxSimInterfaceComparators.getFieldProfComparatorBySortingType(fieldProfSortingType);

        Collections.sort(fieldProfArrayList, fieldProfComparator);

        printUntypedTagName(MaxSimProfOut, tag);
        MaxSimProfOut.print("(" +
            classIdShortName + ":" + classEntryId + " ");
        if (memoryFootprint != 0) {
            MaxSimProfOut.print(memFootprintShortName + ":" + memoryFootprint);
            printAllocProfList(MaxSimProfOut, memAllocProfArrayList);
            MaxSimProfOut.print(" " + memAccCountShortName + ":" + memAccCount);
            MaxSimProfOut.print(" " + memAccCountShortName + "/" + memFootprintShortName + ":" +
                decimalFormat.format((double) memAccCount / (double) memoryFootprint) + " ");
        } else {
            MaxSimProfOut.print(memAccCountShortName + ":" + memAccCount + " ");
        }
        printCacheMissSummaryForFieldProfList(MaxSimProfOut, fieldProfArrayList);
        MaxSimProfOut.print(")" + ":");
        printFieldProfList(MaxSimProfOut, classProf, fieldProfArrayList);
    }

    /**
     * Prints tagged GP class profiling information name.
     */
    private void printTaggedGPClassProfName(PrintWriter MaxSimProfOut, MaxSimInterface.ClassProf classProf) {
        int classId = classProf.getId();

        if (MaxSimInterfaceHelpers.isClassIDTagging(MaxSimProfRep.getPointerTaggingType()) ||
            !MaxSimInterfaceHelpers.isGeneralPurposeTag((short) classId)) {

            MaxSimInterface.ClassInfo classInfo = MaxSimProfRep.getClassInfoByClassId(classId);
            MaxSimProfOut.print(DescriptorPrinter.typeDescriptorToName(classInfo.getDesc(), true, true));
            MaxSimProfOut.print("(" + classInfo.getDesc() + ")");

        } else if (MaxSimInterfaceHelpers.isAllocationSiteIDTagging(MaxSimProfRep.getPointerTaggingType())) {

            MaxSimInterface.AllocSiteProf allocSiteProf = MaxSimProfRep.getAllocationSiteEntryById(classId);
            MaxSimInterface.MethodInfo methodInfo = MaxSimProfRep.getMethodInfoByIP(allocSiteProf.getIP());
            MaxSimInterface.ClassInfo classInfo = MaxSimProfRep.getClassInfoByClassId(allocSiteProf.getClassId());
            MaxSimProfOut.print(DescriptorPrinter.typeDescriptorToName(classInfo.getDesc(), true, true));
            MaxSimProfOut.print("(" + classInfo.getDesc() + ")");
            MaxSimProfOut.print("@");
            printIPInfo(MaxSimProfOut, methodInfo, allocSiteProf.getIP());

        } else {
            assert false : "Unsupported tagging type.";
        }
    }

    /**
     * Prints tagged GP class profiling information properties and class id information.
     */
    private void printTaggedGPClassProfPropertiesAndClassId(PrintWriter MaxSimProfOut,
                                                            MaxSimInterface.ClassProf classProf) {
        MaxSimInterface.ClassInfo classInfo = null;
        int classId = classProf.getId();

        if (MaxSimInterfaceHelpers.isAllocationSiteIDTagging(MaxSimProfRep.getPointerTaggingType()) &&
            MaxSimInterfaceHelpers.isGeneralPurposeTag((short) classId)) {
            MaxSimProfOut.print(allocationSiteIdShortName + ":" + classId + " ");
            classId = MaxSimProfRep.getAllocationSiteEntryById(classId).getClassId();
        }

        MaxSimProfOut.print(classIdShortName + ":" + classId);
        classInfo = MaxSimProfRep.getClassInfoByClassId(classId);
        if (classInfo != null) {
            if (classInfo.hasComponentId()) {
                int componentClassId = classInfo.getComponentId();
                MaxSimInterface.ClassInfo componentClassInfo = MaxSimProfRep.getClassInfoByClassId(componentClassId);
                MaxSimProfOut.print(" " + classComponentIndexShortName + ":" + componentClassId);
                if ((componentClassInfo != null) &&
                    (componentClassInfo.getKind() != MaxSimInterface.ClassInfo.Kind.PRIMITIVE) &&
                    (componentClassInfo.getKind() != MaxSimInterface.ClassInfo.Kind.OTHER)) {
                    MaxSimProfOut.print(" " + classPropertyShortName + ":" + objRefPropShortName);
                }
            }
        }
    }

    /**
     * Prints allocation profile information list.
     */
    private void printAllocProfList(PrintWriter MaxSimProfOut,
                                    ArrayList<MaxSimInterface.AllocProf> memAllocProfArrayList) {
        boolean firstEntry = true;
        MaxSimProfOut.print("(");
        for (MaxSimInterface.AllocProf memoryAllocationEntry : memAllocProfArrayList) {
            int size = memoryAllocationEntry.getSize();
            long allocationCount = memoryAllocationEntry.getCount();
            if (firstEntry) {
                firstEntry = false;
            } else {
                MaxSimProfOut.print(" ");
            }
            MaxSimProfOut.print("s:" + size + "(" + allocationCount + ")");
        }
        MaxSimProfOut.print(")");
    }

    /**
     * Prints tagged GP class profiling information.
     */
    private void printTaggedGPClassProf(PrintWriter MaxSimProfOut, MaxSimInterface.ClassProf classProf) {
        long memoryFootprint = classProf.getMemAllSize();
        long memAccCount = classProf.getMemAccCount();
        long memAllCount = classProf.getMemAllCount();

        taggedGPObjMemFootprint += memoryFootprint;
        taggedGPObjMemAccCount += memAccCount;
        taggedGPObjMemAllCount += memAllCount;

        ArrayList<MaxSimInterface.AllocProf> memAllocProfArrayList =
            new ArrayList<MaxSimInterface.AllocProf>(classProf.getAllocProfList());

        Comparator<MaxSimInterface.AllocProf> memoryAllocationEntryComparator =
            MaxSimInterfaceComparators.getAllocProfComparatorBySortingType(allocProfSortingType);

        Collections.sort(memAllocProfArrayList, memoryAllocationEntryComparator);

        ArrayList<MaxSimInterface.FieldProf> fieldProfArrayList =
            new ArrayList<MaxSimInterface.FieldProf>(classProf.getFieldProfList());

        Comparator<MaxSimInterface.FieldProf> fieldEntryComparator =
            MaxSimInterfaceComparators.getFieldProfComparatorBySortingType(fieldProfSortingType);

        Collections.sort(fieldProfArrayList, fieldEntryComparator);

        printTaggedGPClassProfName(MaxSimProfOut, classProf);
        MaxSimProfOut.print("(");
        printTaggedGPClassProfPropertiesAndClassId(MaxSimProfOut, classProf);
        MaxSimProfOut.print(" " + memFootprintShortName + ":" + memoryFootprint);
        printAllocProfList(MaxSimProfOut, memAllocProfArrayList);
        MaxSimProfOut.print(" " + memAccCountShortName + ":" + memAccCount);
        MaxSimProfOut.print(" " + memAccCountShortName + "/" + memFootprintShortName + ":" +
            decimalFormat.format((double) memAccCount / (double) memoryFootprint) + " ");
        printCacheMissSummaryForFieldProfList(MaxSimProfOut, fieldProfArrayList);;
        MaxSimProfOut.print("):");
        printFieldProfList(MaxSimProfOut, classProf, fieldProfArrayList);
    }

    /**
     * Prints class profiling information from a list.
     */
    private void printClassProfInfoFromList(PrintWriter MaxSimProfOut,
                                            ArrayList<MaxSimInterface.ClassProf> classProfArrayList) {
        for (MaxSimInterface.ClassProf classProf : classProfArrayList) {
            MaxSimInterface.ClassInfo classInfo = null;
            int classEntryId = classProf.getId();
            long memAccCount = classProf.getMemAccCount();
            long memAllCount = classProf.getMemAllCount();
            short tag = (short) classEntryId;

            if (memAccCount == 0 && memAllCount == 0)
                continue;

            if (MaxSimInterfaceHelpers.isAggregateTag(tag)) {
                printTaggedAggregateClassProf(MaxSimProfOut, classProf);
            } else {
                printTaggedGPClassProf(MaxSimProfOut, classProf);
            }
            MaxSimProfOut.println();
        }
    }

    /**
     * Prints class profiling information.
     */
    private void printClassProfInfo(PrintWriter MaxSimProfOut) {
        List<MaxSimInterface.ClassProf> classProfList = MaxSimProfRep.getZSimProfileDB().getClassProfList();

        ArrayList<MaxSimInterface.ClassProf> classProfArrayList =
            new ArrayList<MaxSimInterface.ClassProf>(classProfList);

        Comparator<MaxSimInterface.ClassProf> classEntryComparator =
            MaxSimInterfaceComparators.getClassProfComparatorBySortingType(classProfSortingType);

        Collections.sort(classProfArrayList, classEntryComparator);
        MaxSimProfOut.println("=== Maxine Info ===");
        printBootCodeRegInfo(MaxSimProfOut);
        MaxSimProfOut.println("");
        MaxSimProfOut.println("=== Memory Accesses ===");
        printClassProfInfoFromList(MaxSimProfOut, classProfArrayList);
        printTaggedGPObjSummaryInfo(MaxSimProfOut);
        MaxSimProfOut.println();
    }

    /**
     * Type and method descriptors printer.
     */
    private static class DescriptorPrinter {

        /**
         * Converts type descriptor to name.
         */
        private static final String typeDescriptorToName(String string, boolean printVoid, boolean printFullQual) {
            switch (string.charAt(0)) {
                default:
                    assert false : "invalid type descriptor:" + string;
                    return string;
                case 'B':
                    return "byte";
                case 'C':
                    return "char";
                case 'D':
                    return "double";
                case 'F':
                    return "float";
                case 'I':
                    return "int";
                case 'J':
                    return "long";
                case 'S':
                    return "short";
                case 'V':
                    return printVoid ? "void" : "";
                case 'Z':
                    return "boolean";
                case 'L':
                    int subInd = 1;
                    if (!printFullQual) {
                        subInd = string.lastIndexOf('/');
                        subInd = (subInd < 0) ? 1 : (subInd + 1);
                    }
                    return string.substring(subInd, string.length() - 1).replace('/', '.');
                case '[':
                    return typeDescriptorToName(string.substring(1), printVoid, printFullQual) + "[]";
            }
        }

        /**
         * Converts type descriptor end index in the string, which is the next index after the last index belonging to
         * type descriptor.
         */
        private static int findTypeDescriptorEndIndex(String string, int typeDescBegInd) {
            switch (string.charAt(typeDescBegInd)) {
                default:
                    assert false : "invalid type descriptor:" + string.substring(typeDescBegInd);
                case 'B':
                case 'C':
                case 'D':
                case 'F':
                case 'I':
                case 'J':
                case 'S':
                case 'V':
                case 'Z':
                    return typeDescBegInd + 1;
                case 'L':
                    return string.indexOf(';', typeDescBegInd) + 1;
                case '[':
                    return findTypeDescriptorEndIndex(string, typeDescBegInd + 1);
            }
        }

        /**
         * Converts method descriptor to name.
         */
        private static final String methodDescriptorToName(String string, boolean printRetDesc) {
            String resString = new String("");
            int opnPrnInd = string.indexOf('(');
            int clsPrnInd = string.indexOf(')');
            int typeDescBegInd;
            int typeDescEndInd;
            boolean firstArg;

            assert opnPrnInd == 0 && clsPrnInd > 0 : "invalid method descriptor:" + string;
            if (printRetDesc) {
                typeDescBegInd = clsPrnInd + 1;
                typeDescEndInd = string.length();
                resString = resString.concat(
                    typeDescriptorToName(string.substring(typeDescBegInd, typeDescEndInd), false, false));
            }
            typeDescBegInd = opnPrnInd + 1;
            resString = resString.concat("(");
            firstArg = true;
            while (typeDescBegInd < clsPrnInd) {
                if (!firstArg) {
                    resString = resString.concat(", ");
                }
                typeDescEndInd = findTypeDescriptorEndIndex(string, typeDescBegInd);
                resString = resString.concat(
                    typeDescriptorToName(string.substring(typeDescBegInd, typeDescEndInd), false, false));
                typeDescBegInd = typeDescEndInd;
                firstArg = false;
            }
            resString = resString.concat(")");

            return resString;
        }
    }

    /**
     * Prints untyped tag name.
     */
    public static void printUntypedTagName(PrintWriter MaxSimProfOut, short tag) {
        switch (tag) {
            case (short) MaxSimInterface.PointerTag.TAG_UNDEFINED_VALUE:
                MaxSimProfOut.print("TaggedAggregateMemoryAccesses.UndefinedLoadsAndStores");
                break;
            case (short) MaxSimInterface.PointerTag.TAG_FETCHES_VALUE:
                MaxSimProfOut.print("TaggedAggregateMemoryAccesses.Fetches");
                break;
            case (short) MaxSimInterface.PointerTag.TAG_CODE_VALUE:
                MaxSimProfOut.print("TaggedAggregateMemoryAccesses.CodeLoadsAndStores");
                break;
            case (short) MaxSimInterface.PointerTag.TAG_HEAP_VALUE:
                MaxSimProfOut.print("TaggedAggregateMemoryAccesses.HeapLoadsAndStores");
                break;
            case (short) MaxSimInterface.PointerTag.TAG_STACK_VALUE:
                MaxSimProfOut.print("TaggedAggregateMemoryAccesses.StackLoadsAndStores");
                break;
            case (short) MaxSimInterface.PointerTag.TAG_TLS_VALUE:
                MaxSimProfOut.print("TaggedAggregateMemoryAccesses.TLSLoadsAndStores");
                break;
            case (short) MaxSimInterface.PointerTag.TAG_NATIVE_VALUE:
                MaxSimProfOut.print("TaggedAggregateMemoryAccesses.NativeLoadsAndStores");
                break;
            case (short) MaxSimInterface.PointerTag.TAG_STATIC_VALUE:
                MaxSimProfOut.print("TaggedAggregateMemoryAccesses.StaticLoadsAndStores");
                break;
            case (short) MaxSimInterface.PointerTag.TAG_UNDEFINED_GP_VALUE:
                MaxSimProfOut.print("TaggedAggregateMemoryAccesses.UndefinedGPLoadsAndStores");
                break;
            default:
                assert false : "Unexpected tag: " + tag;
        }
    }
}
