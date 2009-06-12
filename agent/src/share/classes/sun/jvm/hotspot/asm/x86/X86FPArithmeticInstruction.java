/*
 * Copyright 2002-2003 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 *
 */

package sun.jvm.hotspot.asm.x86;

import sun.jvm.hotspot.asm.*;

public class X86FPArithmeticInstruction extends X86FPInstruction
                                        implements ArithmeticInstruction {
   final private int operation; //RTL operation
   final private Operand operand1;
   final private Operand operand2;
   final private String description;

   public X86FPArithmeticInstruction(String name, int operation, Operand op1, Operand op2, int size, int prefixes) {
      super(name, size, prefixes);
      this.operation = operation;
      this.operand1 = op1;
      this.operand2 = op2;
      description = initDescription();
   }

   protected String initDescription() {
      StringBuffer buf = new StringBuffer();
      buf.append(getPrefixString());
      buf.append(getName());
      buf.append(spaces);
      if (operand1 != null) {
         buf.append(getOperandAsString(operand1));
      }
      if (operand2 != null) {
         buf.append(comma);
         buf.append(getOperandAsString(operand2));
      }
      return buf.toString();
   }

   public String asString(long currentPc, SymbolFinder symFinder) {
      return description;
   }

   public Operand getArithmeticDestination() {
      return operand1;
   }
   public Operand getOperand1() {
      return operand1;
   }

   public Operand getOperand2() {
      return operand2;
   }

   public Operand[] getArithmeticSources() {
      return (new Operand[] { operand1, operand2});
   }

   public int getOperation() {
      return operation;
   }

   public boolean isArithmetic() {
      return true;
   }
}
