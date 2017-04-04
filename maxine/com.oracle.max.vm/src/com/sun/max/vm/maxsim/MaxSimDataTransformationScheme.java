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
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.VMOptions;
import com.sun.max.vm.VMStringOption;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.actor.holder.Hub;
import com.sun.max.vm.type.JavaTypeDescriptor;
import com.sun.max.vm.type.TypeDescriptor;

import java.io.FileInputStream;
import java.util.concurrent.ConcurrentHashMap;

public class MaxSimDataTransformationScheme {

    /**
     * Activates MaxSim data transformation.
     */
    public static void activate() {
        if (!MaxSimInterfaceHelpers.isClassIDTagging() || !MaxSimDataTransDB.isPresent()) {
            return;
        }
        String dataTransDBFileName = MaxSimDataTransDB.getValue();
        try {
            MaxSimInterface.DataTransDB dataTransDB =
                MaxSimInterface.DataTransDB.parseFrom(new FileInputStream(dataTransDBFileName));

            for (MaxSimInterface.DataTransInfo dataTrans : dataTransDB.getDataTransInfoList()) {
                TypeDescriptor td = JavaTypeDescriptor.parseTypeDescriptor(dataTrans.getTypeDesc());
                typeDescriptorToDataTransInfoMap.put(td, MaxSimInterface.DataTransInfo.newBuilder(dataTrans));
            }
        } catch (Exception e) {
            Log.println("WARNING: Could not parse file: " + dataTransDBFileName);
            Log.println(e);
        }
        ClassActor.allClassesDo(maxsimDataTransInfoRegistrar);
    }

    /**
     * Registers new dynamic hub.
     */
    @INLINE
    public static void register(Hub hub) {
        if (!MaxSimInterfaceHelpers.isClassIDTagging() || !MaxSimDataTransDB.isPresent()) {
            return;
        }
        MaxSimInterface.DataTransInfo.Builder dataTransInfo =
            typeDescriptorToDataTransInfoMap.get(hub.classActor.typeDescriptor);
        if (dataTransInfo != null && !dataTransInfo.hasTransTag()) {
            dataTransInfo.setTransTag(hub.getMaxSimHubTag());
            MaxSimMediator.activateDataTransViaAddrSpaceMorph(dataTransInfo.build().toByteArray());
        }
    }

    /**
     * Returns type descriptor to data transformation information map.
     */
    public static ConcurrentHashMap<TypeDescriptor, MaxSimInterface.DataTransInfo.Builder> getTypeDescriptorToDataTransInfoMap() {
        return typeDescriptorToDataTransInfoMap;
    }

    /**
     * MaxSim data transformation registrar.
     */
    static class MaxSimDataTransInfoRegistrar implements ClassActor.Closure {
        public boolean doClass(ClassActor classActor) {
            Hub hub = classActor.dynamicHub();
            if (hub == null) {
                return true;
            }
            register(hub);
            return true;
        }
    }

    static final MaxSimDataTransInfoRegistrar maxsimDataTransInfoRegistrar = new MaxSimDataTransInfoRegistrar();

    /**
     * Type descriptor to data transformation map.
     */
    private static final ConcurrentHashMap<TypeDescriptor, MaxSimInterface.DataTransInfo.Builder> typeDescriptorToDataTransInfoMap =
        new ConcurrentHashMap<TypeDescriptor, MaxSimInterface.DataTransInfo.Builder>();

    private static VMStringOption MaxSimDataTransDB = VMOptions.register(new VMStringOption("-XX:MaxSimDataTransDB=", false, null,
        "MaxSim data transformation database for address space morphing."), MaxineVM.Phase.PRISTINE);
}
