/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.c1x.ir;

import com.sun.c1x.ci.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.value.*;

/**
 * The <code>LoadIndexed</code> instruction represents a read from an element of an array.
 *
 * @author Ben L. Titzer
 */
public class LoadIndexed extends AccessIndexed {

    /**
     * Creates a new LoadIndexed instruction.
     * @param array the instruction producing the array
     * @param index the instruction producing the index
     * @param length the instruction producing the length
     * @param elementType the element type
     * @param stateBefore the state before executing this instruction
     */
    public LoadIndexed(Value array, Value index, Value length, CiKind elementType, ValueStack stateBefore) {
        super(array, index, length, elementType, stateBefore);
    }

    /**
     * Gets the declared type of this instruction's result.
     * @return the declared type
     */
    @Override
    public RiType declaredType() {
        RiType arrayType = array().declaredType();
        if (arrayType == null) {
            return null;
        }
        return arrayType.componentType();
    }

    /**
     * Gets the exact type of this instruction's result.
     * @return the exact type
     */
    @Override
    public RiType exactType() {
        RiType declared = declaredType();
        return declared != null ? declared.exactType() : null;
    }

    @Override
    public void accept(ValueVisitor v) {
        v.visitLoadIndexed(this);
    }
}
