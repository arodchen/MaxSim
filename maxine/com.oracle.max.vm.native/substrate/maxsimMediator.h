/*
 * Copyright (c) 2017 by Andrey Rodchenko, School of Computer Science,
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef __ZSIM_HOOKS_H__
#define __ZSIM_HOOKS_H__

#include <stdint.h>
#include <stdio.h>
#include "zsim/src/maxsim_interface_c.h"

//Avoid optimizing compilers moving code around this barrier
#define COMPILER_BARRIER() { __asm__ __volatile__("" ::: "memory");}

#ifdef __x86_64__
#define HOOKS_STR  "HOOKS"
static inline void maxsim_magic_op(uint64_t op) {
    COMPILER_BARRIER();
    __asm__ __volatile__("xchg %%rcx, %%rcx;" : : "c"(op));
    COMPILER_BARRIER();
}
static inline void maxsim_magic_op_arg(uint64_t op, uint64_t env) {
    COMPILER_BARRIER();
    __asm__ __volatile__("xchg %%rcx, %%rcx;" : : "c"(op), "b"(env));
    COMPILER_BARRIER();
}
#else
#define HOOKS_STR  "NOP-HOOKS"
static inline void maxsim_magic_op(uint64_t op) {
    //NOP
}
static inline void maxsim_magic_op_arg(uint64_t op, uint64_t env) {
    //NOP
}
#endif

static inline void maxsim_c_register_address_range(uint64_t lo, uint64_t hi, AddressRangeType type) {
    AddressRange_t range;
    range.lo = lo;
    range.hi = hi;
    range.type = type;
    maxsim_magic_op_arg(MAXSIM_M_OPC_REGISTER_ADDRESS_RANGE, (uint64_t) & range);
}

static inline void maxsim_c_deregister_address_range(uint64_t lo, uint64_t hi, AddressRangeType type) {
    AddressRange_t range;
    range.lo = lo;
    range.hi = hi;
    range.type = type;
    maxsim_magic_op_arg(MAXSIM_M_OPC_DEREGISTER_ADDRESS_RANGE, (uint64_t) & range);
}

#endif /*__ZSIM_HOOKS_H__*/

