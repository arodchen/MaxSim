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

import com.google.protobuf.CodedInputStream;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MaxSimProfileRepresentation {

    private static final int MAX_PROF_DB_SIZE = 1 << 28;

    private static String ZSimProfileDBOptionName =
        new String("ZSimProfileDB");

    private static String ZSimProfileDBOptionHelp =
        new String("Location of the file containing ZSim profile data base.");

    private static String MaxineInfoDBOptionName =
        new String("MaxineInfoDB");

    private static String MaxineInfoDBOptionHelp =
        new String("Location of the file containing Maxine information data base.");

    private boolean profileLoaded;

    private MaxSimInterface.ZSimProfDB ZSimProfileDB;

    private MaxSimInterface.MaxineInfoDB MaxineInfoDB;

    private MaxSimInterface.ClassInfo [] IdToClassInfoMap;

    private MaxSimInterface.AllocSiteProf [] IdToAllocationSiteEntryMap;

    private MaxSimInterface.CacheRWGroupInfo [] IdToCacheRWGroupInfoMap;

    private ArrayList<MaxSimInterface.MethodInfo> methodInfoArraySortedByAccendingIP;

    private long [] methodInfoIPArraySortedByAccendingIP;

    private int [][] methodBCIArraySortedByAccendingIP;

    private int [][] methodOffsetArraySortedByAccendingIP;

    private MaxSimInterface.PointerTaggingType pointerTaggingType;

    private MaxSimInterface.MaxSimConfig MaxSimConfig;

    public MaxSimInterface.MaxSimConfig getMaxSimConfig() {
        return MaxSimConfig;
    }

    public MaxSimInterface.PointerTaggingType getPointerTaggingType() {
        return pointerTaggingType;
    }

    public static String getZSimProfileDBOptionName() {
        return ZSimProfileDBOptionName;
    }

    public static String getZSimProfileDBOptionHelp() {
        return ZSimProfileDBOptionHelp;
    }

    public static String getMaxineInfoDBOptionName() {
        return MaxineInfoDBOptionName;
    }

    public static String getMaxineInfoDBOptionHelp() {
        return MaxineInfoDBOptionHelp;
    }

    public MaxSimInterface.ZSimProfDB getZSimProfileDB() {
        return ZSimProfileDB;
    }

    public MaxSimInterface.MaxineInfoDB getMaxineInfoDB() {
        return MaxineInfoDB;
    }

    public MaxSimInterface.ClassInfo getClassInfoByProfId(int profId) {
        if (MaxSimInterfaceHelpers.isClassIDTagging(pointerTaggingType)) {
            return getClassInfoByClassId(profId);
        }
        return null;
    }

    public MaxSimInterface.ClassInfo getClassInfoByClassId(int id) {
        if (MaxSimInterfaceHelpers.isAggregateTag((short) id)) {
            return null;
        }
        return IdToClassInfoMap[id];
    }

    public MaxSimInterface.AllocSiteProf getAllocationSiteEntryById(int id) {
        return IdToAllocationSiteEntryMap[id];
    }

    public MaxSimInterface.CacheRWGroupInfo getCacheRWGroupInfoById(int id) {
        return IdToCacheRWGroupInfoMap[id];
    }

    public int getCacheRWGroupsNum() {
        return IdToCacheRWGroupInfoMap.length;
    }

    public MaxSimInterface.MethodInfo getMethodInfoByIP(long ip) {
        MaxSimInterface.MethodInfo methodInfo;
        int index = Arrays.binarySearch(methodInfoIPArraySortedByAccendingIP, ip);

        if (index < 0) {
            index = -(index + 1) - 1;
        }
        if (index < 0) {
            return null;
        }
        methodInfo = methodInfoArraySortedByAccendingIP.get(index);
        if (ip < (methodInfo.getBeginIP() + methodInfo.getSize())) {
            return methodInfo;
        }

        return null;
    }

    public int getBCIByIP(long ip) {
        MaxSimInterface.MethodInfo methodInfo;
        int i = Arrays.binarySearch(methodInfoIPArraySortedByAccendingIP, ip);

        if (i < 0) {
            i = -(i + 1) - 1;
        }
        if (i < 0) {
            return -1;
        }
        methodInfo = methodInfoArraySortedByAccendingIP.get(i);
        if (ip < (methodInfo.getBeginIP() + methodInfo.getSize())) {
            int o = (int) (ip - methodInfo.getBeginIP());
            int j = Arrays.binarySearch(methodOffsetArraySortedByAccendingIP[i], o);
            if (j < 0) {
                j = -(j + 1);
            }
            if (j == methodOffsetArraySortedByAccendingIP[i].length) {
                return -1;
            }
            return methodBCIArraySortedByAccendingIP[i][j];
        }

        return -1;
    }

    public MaxSimInterface.FieldInfo getFieldInfoByOffset(MaxSimInterface.ClassInfo classInfo, int offset) {
        if (classInfo == null) {
            return null;
        }
        for (MaxSimInterface.FieldInfo fieldInfo : classInfo.getFieldInfoList()) {
            if (fieldInfo.getOffset() == offset) {
                return fieldInfo;
            }
        }
        return null;
    }

    public boolean isProfileLoaded() {
        return profileLoaded;
    }

    public void loadZSimProfileAndMaxineInfoDBs(String zsimProfileDBFileName, String zsimMaxineInfoDBFileName) {
        if (zsimProfileDBFileName == null || zsimMaxineInfoDBFileName == null || profileLoaded) {
            return;
        }
        try {
            CodedInputStream zsimProfileDBInputStream = CodedInputStream.newInstance(
                new FileInputStream(zsimProfileDBFileName));

            zsimProfileDBInputStream.setSizeLimit(MAX_PROF_DB_SIZE);
            ZSimProfileDB = MaxSimInterface.ZSimProfDB.parseFrom(zsimProfileDBInputStream);
            MaxineInfoDB = MaxSimInterface.MaxineInfoDB.parseFrom(new FileInputStream(zsimMaxineInfoDBFileName));

            IdToClassInfoMap = new MaxSimInterface.ClassInfo [MaxineInfoDB.getMaxClassInfoId() + 1];
            for (MaxSimInterface.ClassInfo ci : MaxineInfoDB.getClassInfoList()) {
                IdToClassInfoMap[ci.getId()] = ci;
            }

            IdToAllocationSiteEntryMap = new MaxSimInterface.AllocSiteProf [ZSimProfileDB.getMaxAllocSiteProfId() + 1];
            for (MaxSimInterface.AllocSiteProf entry : ZSimProfileDB.getAllocSiteProfList()) {
                IdToAllocationSiteEntryMap[entry.getId()] = entry;
            }

            IdToCacheRWGroupInfoMap =
                new MaxSimInterface.CacheRWGroupInfo [ZSimProfileDB.getCacheRWGroupInfoCount()];
            for (MaxSimInterface.CacheRWGroupInfo entry : ZSimProfileDB.getCacheRWGroupInfoList()) {
                IdToCacheRWGroupInfoMap[entry.getCacheRWGroupId()] = entry;
            }

            methodInfoArraySortedByAccendingIP =
                new ArrayList<MaxSimInterface.MethodInfo>(MaxineInfoDB.getMethodInfoList());
            Collections.sort(methodInfoArraySortedByAccendingIP, MaxSimInterfaceComparators.MethodInfoBeginIPAscendingCmp);

            methodInfoIPArraySortedByAccendingIP = new long[methodInfoArraySortedByAccendingIP.size()];
            methodBCIArraySortedByAccendingIP = new int[methodInfoArraySortedByAccendingIP.size()][];
            methodOffsetArraySortedByAccendingIP = new int[methodInfoArraySortedByAccendingIP.size()][];
            for (int i = 0; i < methodInfoArraySortedByAccendingIP.size(); i++) {
                MaxSimInterface.MethodInfo methodInfo = methodInfoArraySortedByAccendingIP.get(i);
                methodInfoIPArraySortedByAccendingIP[i] = methodInfo.getBeginIP();
                assert i == 0 ||
                    methodInfoIPArraySortedByAccendingIP[i] > methodInfoIPArraySortedByAccendingIP[i - 1];

                ArrayList<MaxSimInterface.OffsetBCIPair> offsetBCIPairArraySortedByOffset =
                    new ArrayList<MaxSimInterface.OffsetBCIPair>(methodInfo.getOffsetBCIPairList());
                Collections.sort(offsetBCIPairArraySortedByOffset, MaxSimInterfaceComparators.OffsetBCIPairOffsetAscendingCmp);

                methodBCIArraySortedByAccendingIP[i] = new int[methodInfo.getOffsetBCIPairCount()];
                methodOffsetArraySortedByAccendingIP[i] = new int[methodInfo.getOffsetBCIPairCount()];

                for (int j = 0; j < methodInfo.getOffsetBCIPairCount(); j++) {
                    methodBCIArraySortedByAccendingIP[i][j] = offsetBCIPairArraySortedByOffset.get(j).getBCI();
                    methodOffsetArraySortedByAccendingIP[i][j] = offsetBCIPairArraySortedByOffset.get(j).getOffset();
                }
            }

            MaxSimConfig = MaxineInfoDB.getMaxSimConfig();
            pointerTaggingType = MaxSimConfig.getPointerTaggingType();

            profileLoaded = true;
        } catch (Exception e) {
            unloadZSimProfileAndMaxineInfoDBs();
            System.out.println("WARNING: Could not parse files: " + zsimProfileDBFileName + ", " + zsimMaxineInfoDBFileName);
            System.out.println(e);
        }
    }

    public void unloadZSimProfileAndMaxineInfoDBs() {
        profileLoaded = false;
        IdToClassInfoMap = null;
        MaxineInfoDB = null;
        ZSimProfileDB = null;
        pointerTaggingType = null;
        IdToAllocationSiteEntryMap = null;
        IdToCacheRWGroupInfoMap = null;
        methodInfoArraySortedByAccendingIP = null;
        pointerTaggingType = null;
        MaxSimConfig = null;
    }
}
