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
package com.oracle.graal.phases.common;

import java.util.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.*;


/**
 * A factory that permits subclasses of {@link InliningPhase} to be created. To create instances of a {@code InliningPhase} subclass,
 * register the custom factory with {@link #registerFactory(InliningPhaseFactory)}. This is a workaround for the fact that the creation of
 * {@link InliningPhase} is currently wired into the compiler and not handled in a {@link Suite}.
 */
public class InliningPhaseFactory {

    /**
     * The name of the system property specifying a subclass of {@link InliningPhaseFactory} that is
     * to be instantiated and used at runtime to create {@link InliningPhase} instances. If not specified,
     * then a default factory is used that simply creates plain {@link InliningPhase} instances.
     */
    public static final String INLINING_PHASE_FACTORY_CLASS_PROPERTY_NAME = "graal.inliningphase.factory.class";

    private static InliningPhaseFactory instance;

    private static InliningPhaseFactory checkInstance() {
        if (instance == null) {
            instance  = new InliningPhaseFactory();
        }
        return instance;
    }

    /**
     * Subclasses override this method to instantiate objects of an {@link InliningPhase} subclass.
      */
    protected InliningPhase newInliningPhase(MetaAccessProvider runtime, Map<Invoke, Double> hints, Replacements replacements, Assumptions assumptions, GraphCache cache, PhasePlan plan,
                    OptimisticOptimizations optimisticOpts) {
        return new InliningPhase(runtime, hints, replacements, assumptions, cache, plan, optimisticOpts);
    }

    public static void registerFactory(InliningPhaseFactory inliningPhaseFactory) {
        instance = inliningPhaseFactory;
    }

    /**
     * Creates an {@link InliningPhase} instance using the factory.
     */
    public static InliningPhase create(MetaAccessProvider runtime, Map<Invoke, Double> hints, Replacements replacements, Assumptions assumptions, GraphCache cache, PhasePlan plan,
                    OptimisticOptimizations optimisticOpts) {
        return checkInstance().newInliningPhase(runtime, hints, replacements, assumptions, cache, plan, optimisticOpts);
    }
}
