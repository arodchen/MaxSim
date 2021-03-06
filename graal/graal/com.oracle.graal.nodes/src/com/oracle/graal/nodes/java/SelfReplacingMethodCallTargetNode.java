/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.nodes.java;

import java.lang.reflect.Modifier;

import com.oracle.graal.api.meta.JavaType;
import com.oracle.graal.api.meta.ResolvedJavaMethod;
import com.oracle.graal.graph.GraalInternalError;
import com.oracle.graal.graph.NodeInputList;
import com.oracle.graal.nodes.ValueNode;
import com.oracle.graal.nodes.spi.LIRGeneratorTool;
import com.oracle.graal.nodes.spi.Lowerable;
import com.oracle.graal.nodes.spi.LoweringTool;

/**
 * A SelfReplacingMethodCallTargetNode replaces itself in the graph when being lowered with a
 * {@link MethodCallTargetNode} that calls the stored replacement target method.
 * 
 * This node is used for method handle call nodes which have a constant call target but are not
 * inlined.
 */
public class SelfReplacingMethodCallTargetNode extends MethodCallTargetNode implements Lowerable {

    // Replacement method data
    private final ResolvedJavaMethod replacementTargetMethod;
    private final JavaType replacementReturnType;
    @Input private final NodeInputList<ValueNode> replacementArguments;

    public SelfReplacingMethodCallTargetNode(InvokeKind invokeKind, ResolvedJavaMethod targetMethod, ValueNode[] arguments, JavaType returnType, ResolvedJavaMethod replacementTargetMethod,
                    ValueNode[] replacementArguments, JavaType replacementReturnType) {
        super(invokeKind, targetMethod, arguments, returnType);
        this.replacementTargetMethod = replacementTargetMethod;
        this.replacementReturnType = replacementReturnType;
        this.replacementArguments = new NodeInputList<>(this, replacementArguments);
    }

    public ResolvedJavaMethod replacementTargetMethod() {
        return replacementTargetMethod;
    }

    public JavaType replacementReturnType() {
        return replacementReturnType;
    }

    public NodeInputList<ValueNode> replacementArguments() {
        return replacementArguments;
    }

    @Override
    public void lower(LoweringTool tool, LoweringType loweringType) {
        InvokeKind invokeKind = Modifier.isStatic(replacementTargetMethod.getModifiers()) ? InvokeKind.Static : InvokeKind.Special;
        MethodCallTargetNode replacement = graph().add(
                        new MethodCallTargetNode(invokeKind, replacementTargetMethod, replacementArguments.toArray(new ValueNode[replacementArguments.size()]), replacementReturnType));

        // Replace myself...
        this.replaceAndDelete(replacement);
    }

    @Override
    public void generate(LIRGeneratorTool gen) {
        throw GraalInternalError.shouldNotReachHere("should have replaced itself");
    }
}
