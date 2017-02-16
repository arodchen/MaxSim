/** $lic$
 * Copyright (C) 2017 by Andrey Rodchenko, School of Computer Science,
 * The University of Manchester
 *
 * This file is part of zsim.
 *
 * zsim is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * If you use this software in your research, we request that you reference
 * the zsim paper ("ZSim: Fast and Accurate Microarchitectural Simulation of
 * Thousand-Core Systems", Sanchez and Kozyrakis, ISCA-40, June 2013) as the
 * source of the simulator in any publications that use this software, and that
 * you send us a citation of your work.
 *
 * zsim is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef SRC_POINTER_TAGGING_H
#define SRC_POINTER_TAGGING_H

#include "zsim.h"

#ifdef POINTER_TAGGING_ENABLED

#   define POINTER_SIZE               (64)
#   define POINTER_TAG_MASK_SIZE      (16)
#   define POINTER_TAG_MASK_MAX       (16) // Maximal number of tag bits which is supported.
                                           // If this parameter is increased to > 16 then
                                           // tag type size (uint16_t) should be increased respectively.

#   define POINTER_NON_TAG_MASK_SIZE  ((POINTER_SIZE - POINTER_TAG_MASK_SIZE))

// Get untagged pointer sign extended.
inline Address getUntaggedPointerSE(Address addr) {
    return (Address) (((int64_t) addr << POINTER_TAG_MASK_SIZE) >> POINTER_TAG_MASK_SIZE);
}

// Get untagged pointer zero extended.
inline Address getUntaggedPointerZE(Address addr) {
    return (Address) (((uint64_t) addr << POINTER_TAG_MASK_SIZE) >> POINTER_TAG_MASK_SIZE);
}

// Get pointer tag.
inline PointerTag_t getPointerTag(Address addr) {
    return (addr >> POINTER_NON_TAG_MASK_SIZE);
}

// Set pointer tag.
inline Address setPointerTag(Address addr, PointerTag_t tag) {
    return (((Address) tag) << POINTER_NON_TAG_MASK_SIZE) | getUntaggedPointerZE(addr);
}

#endif // POINTER_TAGGING_ENABLED

#endif //SRC_POINTER_TAGGING_H
