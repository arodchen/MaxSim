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

import com.sun.cri.ci.CiDebugInfo;
import com.sun.max.platform.Platform;
import com.sun.max.unsafe.UnsafeCast;
import com.sun.max.vm.Log;
import com.sun.max.vm.actor.holder.ClassActor;
import com.sun.max.vm.actor.holder.DynamicHub;
import com.sun.max.vm.actor.member.FieldActor;
import com.sun.max.vm.actor.member.InjectedFieldActor;
import com.sun.max.vm.code.Code;
import com.sun.max.vm.code.CodeRegion;
import com.sun.max.vm.compiler.target.TargetMethod;
import com.sun.max.vm.type.TypeDescriptor;

import java.io.FileOutputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZSim-Maxine information builder.
 */
public class MaxSimMaxineInfoBuilder {

    public void printMaxineInfoToFile(String fileName) {
        try {
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = MaxSimPlatform.MaxSimMaxineInfoFileName.getValue();
            }

            FileOutputStream output = new FileOutputStream(fileName);
            MaxSimInterface.MaxineInfoDB.Builder maxineInfoDB = MaxSimInterface.MaxineInfoDB.newBuilder();

            buildMaxineTypesInfo(maxineInfoDB);
            buildMaxineMethodsInfo(maxineInfoDB);
            buildMaxineDataTransInfo(maxineInfoDB);
            buildMaxSimConfig(maxineInfoDB);
            maxineInfoDB.build().writeTo(output);
            output.close();
        } catch (Exception e) {
            Log.println("Could not build MaxineInfo. Exception thrown:" + e.toString());
        }
    }

    class MethodInfoBuilder implements TargetMethod.Closure {

        public MethodInfoBuilder(MaxSimInterface.MaxineInfoDB.Builder maxineInfoDB) {
            this.maxineInfoDB = maxineInfoDB;
            this.methodInfo = MaxSimInterface.MethodInfo.newBuilder();
            this.offsetBCIPair = MaxSimInterface.OffsetBCIPair.newBuilder();
        }

        @Override
        public boolean doTargetMethod(TargetMethod targetMethod) {
            long beginIP = targetMethod.codeStart().toLong();
            int size = targetMethod.codeLength();

            methodInfo.clear();
            if (targetMethod.classMethodActor != null) {
                methodInfo.setName(targetMethod.classMethodActor.name());
                methodInfo.setDesc(targetMethod.classMethodActor.descriptor().toString());
                if (targetMethod.classMethodActor.holder() != null) {
                    methodInfo.setClassId(UnsafeCast.asInt(targetMethod.classMethodActor.holder().getMaxSimTag()));
                }
            } else {
                methodInfo.setName(targetMethod.toString());
            }
            methodInfo.setBeginIP(beginIP);
            methodInfo.setSize(size);
            methodInfo.setKind(methodKind);
            for (int safepointIndex = 0; safepointIndex < targetMethod.safepoints().size(); safepointIndex++) {
                CiDebugInfo info = targetMethod.debugInfoAt(safepointIndex, null);
                if (info !=  null && info.codePos != null) {
                    offsetBCIPair.clear();
                    offsetBCIPair.setOffset(targetMethod.safepoints().posAt(safepointIndex));
                    offsetBCIPair.setBCI(info.codePos.bci);
                    methodInfo.addOffsetBCIPair(offsetBCIPair);
                }
            }
            maxineInfoDB.addMethodInfo(methodInfo);
            return true;
        }

        private void doBootCodeRegion() {
            methodInfo.clear();
            methodInfo.setBeginIP(Code.bootCodeRegion().start().toLong());
            methodInfo.setSize(Code.bootCodeRegion().size().toLong());
            methodInfo.setName(bootCodeRegionName);
            maxineInfoDB.setBootCodeRegInfo(methodInfo);
        }

        private void setKind(MaxSimInterface.MethodInfo.Kind methodKind) {
            this.methodKind = methodKind;
        }

        private String bootCodeRegionName = new String("BootCodeRegion");

        private MaxSimInterface.MethodInfo.Kind methodKind;

        private MaxSimInterface.OffsetBCIPair.Builder offsetBCIPair;

        private MaxSimInterface.MethodInfo.Builder methodInfo;

        private MaxSimInterface.MaxineInfoDB.Builder maxineInfoDB;
    }

    private void buildMaxineMethodsInfo(MaxSimInterface.MaxineInfoDB.Builder maxineInfoDB) {
        CodeRegion codeRegion;
        MethodInfoBuilder methodInfoBuilder = new MethodInfoBuilder(maxineInfoDB);

        codeRegion = Code.getCodeManager().getRuntimeOptCodeRegion();
        methodInfoBuilder.setKind(MaxSimInterface.MethodInfo.Kind.OPTIMIZED);
        codeRegion.doAllTargetMethods(methodInfoBuilder);

        codeRegion = Code.bootCodeRegion();
        methodInfoBuilder.setKind(MaxSimInterface.MethodInfo.Kind.BOOT);
        codeRegion.doAllTargetMethods(methodInfoBuilder);

        codeRegion = Code.getCodeManager().getRuntimeBaselineCodeRegion();
        methodInfoBuilder.setKind(MaxSimInterface.MethodInfo.Kind.BASELINE);
        codeRegion.doAllTargetMethods(methodInfoBuilder);

        methodInfoBuilder.doBootCodeRegion();
    }

    class TypeInfoBuilder implements ClassActor.Closure {

        public TypeInfoBuilder(MaxSimInterface.MaxineInfoDB.Builder maxineInfo) {
            this.maxineInfo = maxineInfo;
            this.classInfo = MaxSimInterface.ClassInfo.newBuilder();
            this.fieldInfo = MaxSimInterface.FieldInfo.newBuilder();
            this.maxClassInfoId = 0;
        }

        @Override
        public boolean doClass(ClassActor classActor) {
            MaxSimInterface.ClassInfo.Kind kind = classActorToTypeKind(classActor);
            int classId = UnsafeCast.asInt(classActor.getMaxSimTag());

            if (classId > maxClassInfoId) {
                maxClassInfoId = classId;
            }
            classInfo.clear();
            classInfo.setId(classId);
            classInfo.setDesc(classActor.name());
            classInfo.setKind(kind);
            if (kind == MaxSimInterface.ClassInfo.Kind.ARRAY) {
                classInfo.setComponentId(UnsafeCast.asInt(classActor.componentClassActor().getMaxSimTag()));
            } else if (kind == MaxSimInterface.ClassInfo.Kind.TUPLE ||
                       kind == MaxSimInterface.ClassInfo.Kind.HYBRID) {
                ClassActor currentClassActor = classActor;
                do {
                    for (FieldActor fieldActor : currentClassActor.localInstanceFieldActors()) {
                        if (!(fieldActor.descriptor().isResolvableWithoutClassLoading(fieldActor.holder().classLoader)) &&
                            !(fieldActor instanceof InjectedFieldActor))
                            continue;
                        fieldInfo.clear();
                        fieldInfo.setName(fieldActor.name());
                        fieldInfo.setOffset(fieldActor.offset());
                        fieldInfo.setClassId(UnsafeCast.asInt(fieldActor.type().getMaxSimTag()));
                        if (fieldActor.isFinal()) {
                            fieldInfo.addProperty(MaxSimInterface.FieldInfo.Property.FINAL);
                        }
                        classInfo.addFieldInfo(fieldInfo);
                    }
                    currentClassActor = currentClassActor.superClassActor;
                } while (currentClassActor != null);
            }
            maxineInfo.addClassInfo(classInfo);

            return true;
        }

        private MaxSimInterface.ClassInfo.Kind classActorToTypeKind(ClassActor classActor) {
            if (classActor.isTupleClass()) {
                return MaxSimInterface.ClassInfo.Kind.TUPLE;
            }
            if (classActor.isArrayClass()) {
                return MaxSimInterface.ClassInfo.Kind.ARRAY;
            }
            if (classActor.isPrimitiveClassActor()) {
                return MaxSimInterface.ClassInfo.Kind.PRIMITIVE;
            }
            if (classActor.isHybridClass()) {
                return MaxSimInterface.ClassInfo.Kind.HYBRID;
            }

            return MaxSimInterface.ClassInfo.Kind.OTHER;
        }

        private int getMaxClassInfoId() {
            return maxClassInfoId;
        }

        private int maxClassInfoId;

        private MaxSimInterface.FieldInfo.Builder fieldInfo;

        private MaxSimInterface.ClassInfo.Builder classInfo;

        private MaxSimInterface.MaxineInfoDB.Builder maxineInfo;
    }

    private void buildMaxineTypesInfo(MaxSimInterface.MaxineInfoDB.Builder maxineInfo) {
        TypeInfoBuilder typeInfoBuilder = new TypeInfoBuilder(maxineInfo);
        ClassActor.allClassesDo(typeInfoBuilder);
        maxineInfo.setMaxClassInfoId(typeInfoBuilder.getMaxClassInfoId());
        maxineInfo.setNullCheckOffset(Platform.platform().nullCheckOffset);
    }

    private void buildMaxineDataTransInfo(MaxSimInterface.MaxineInfoDB.Builder maxineInfoDB) {
        ConcurrentHashMap<TypeDescriptor, MaxSimInterface.DataTransInfo.Builder> typeDescriptorToDataTransInfoMap =
            MaxSimDataTransformationScheme.getTypeDescriptorToDataTransInfoMap();
        for (MaxSimInterface.DataTransInfo.Builder dataTransInfoBuilder : typeDescriptorToDataTransInfoMap.values()) {
            if (dataTransInfoBuilder.hasTransTag()) {
                maxineInfoDB.addDataTransInfo(dataTransInfoBuilder);
            }
        }
    }

    private void buildMaxSimConfig(MaxSimInterface.MaxineInfoDB.Builder maxineInfoDB) {
        MaxSimInterface.MaxSimConfig.Builder MaxSimConfig = MaxSimInterface.MaxSimConfig.newBuilder();
        MaxSimConfig.setLayoutScaleFactor(
            MaxSimInterface.MaxSimConfig.getDefaultInstance().getLayoutScaleFactor());
        MaxSimConfig.setLayoutScaleRefFactor(
            MaxSimInterface.MaxSimConfig.getDefaultInstance().getLayoutScaleRefFactor());
        MaxSimConfig.setPointerTaggingType(MaxSimInterface.MaxSimConfig.getDefaultInstance().getPointerTaggingType());
        maxineInfoDB.setMaxSimConfig(MaxSimConfig);
    }
}
