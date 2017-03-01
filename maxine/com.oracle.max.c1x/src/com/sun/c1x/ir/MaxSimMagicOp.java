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
package com.sun.c1x.ir;

import com.oracle.max.criutils.*;
import com.sun.cri.ci.*;

/**
 * Instruction implementing the semantics of MaxSim magic operation.
 */
public final class MaxSimMagicOp extends Instruction {

    public final CiRegister register;
    public final CiKind ciKind;

    /**
     * Creates a {@link MaxSimMagicOp} instance.
     */
    public MaxSimMagicOp(CiRegister register, CiKind ciKind) {
        super(CiKind.Void);
        this.register = register;
        this.ciKind = ciKind;
        setFlag(Flag.LiveSideEffect); // ensure this instruction is not eliminated
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitMaxSimMagicOp(this);
    }

    @Override
    public void print(LogStream out) {
        out.print("maxsim_magic_op ").print(register.toString());
    }
}
