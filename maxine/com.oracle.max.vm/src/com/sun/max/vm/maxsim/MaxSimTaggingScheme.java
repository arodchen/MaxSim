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

import com.oracle.max.criutils.TTY;
import com.sun.max.annotate.INLINE;
import com.sun.max.unsafe.*;
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.VMConfiguration;
import com.sun.max.vm.VMOptions;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.Code;
import com.sun.max.vm.compiler.target.TargetBundleLayout;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.Layout;
import com.sun.max.vm.layout.SpecificLayout;
import com.sun.max.vm.object.ObjectAccess;
import com.sun.max.vm.reference.Reference;
import com.sun.max.vm.runtime.FatalError;
import com.sun.max.vm.runtime.VmOperation;
import com.sun.max.vm.stack.StackReferenceMapPreparer;
import com.sun.max.vm.thread.VmThread;
import com.sun.max.vm.thread.VmThreadLocal;
import com.sun.max.vm.type.ClassRegistry;

import static com.sun.max.vm.thread.VmThreadLocal.LOWEST_ACTIVE_STACK_SLOT_ADDRESS;

/**
 * MaxSim (un)tagging scheme.
 */
public class MaxSimTaggingScheme {

    /**
     * Pointer (un)tagging VM operation.
     */
    private static class PointerTaggingOperation extends VmOperation {

        /**
         * Cell pointer (un)tagger.
         */
        private static class CellPointerTagger extends CallbackCellVisitor {

            /**
             * A procedure to (un)tag a pointer.
             */
            private static class PointerIndexTagger extends PointerIndexVisitor {

                /**
                 * Action mode.
                 */
                private static ActionMode actionMode;

                @Override
                public void visit(Pointer pointer, int wordIndex) {
                    Reference ref = pointer.getReference(wordIndex);
                    Pointer refPointer = ref.toOrigin();

                    if (refPointer.isZero())
                        return;

                    switch (actionMode) {
                        case MaxSimPointerTaggingVerification:
                            if (refPointer.isUntagged()) {
                                TTY.println("WARNING: Untagged pointer found after tagging: [" +
                                    pointer.plusWords(wordIndex).to0xHexString() + "] == " +
                                    refPointer.to0xHexString() + " " + ref.toJava() + " classActor:" +
                                    Layout.getHub(refPointer).classActor.toString());
                                refPointer = setTagUsingObjectHub(refPointer);
                            }
                            break;
                        case MaxSimPointerTagging:
                            refPointer = setTagUsingObjectHub(refPointer);
                            break;
                        case MaxSimPointerUntaggingVerification:
                            if (!refPointer.isUntagged()) {
                                TTY.println("WARNING: Tagged pointer found after untagging: [" +
                                    pointer.plusWords(wordIndex).to0xHexString() + "] == " +
                                    refPointer.to0xHexString() + " " + ref.toJava() + " classActor:" +
                                    Layout.getHub(refPointer).classActor.toString());
                                refPointer = refPointer.tagClear();
                            }
                            break;
                        case MaxSimPointerUntagging:
                            refPointer = refPointer.tagClear();
                            break;
                        default:
                            FatalError.unexpected("Unsupported tagging action mode.");
                    }
                    ref = Reference.fromOrigin(refPointer);
                    pointer.setReference(wordIndex, ref);
                }
            }

            /**
             * Pointer index (un)tagger.
             */
            private static PointerIndexTagger pointerIndexTagger = new PointerIndexTagger();

            /**
             * Visit an array of references.
             */
            private void visitReferenceArray(Pointer origin) {
                final int length = Layout.readArrayLength(origin);
                int firstElementOffset = Layout.firstElementIndex();
                for (int index = 0; index < length; index++) {
                    pointerIndexTagger.visit(origin, firstElementOffset + index);
                }
            }

            /**
             * Empty callback.
             */
            protected boolean callback(Object object) {
                return false;
            }


            /**
             * Visits a cell for an object and (un)tags it.
             */
            public Pointer visitCell(Pointer cell) {
                final Pointer origin = Layout.cellToOrigin(cell);
                final Hub hub = Layout.getHub(origin);

                // Update hub reference
                pointerIndexTagger.visit(cell, Layout.hubIndex());

                // Update referent
                if (hub.isJLRReference) {
                    int referentWordIndex = ClassRegistry.JLRReference_referent.offset() / Word.size();
                    pointerIndexTagger.visit(origin, referentWordIndex);
                }

                // Update other references in an object
                final SpecificLayout specificLayout = hub.specificLayout;
                if (specificLayout.isTupleLayout()) {
                    TupleReferenceMap.visitReferences(hub, origin, pointerIndexTagger);
                    return cell.plus(hub.tupleSize);
                }
                if (specificLayout.isReferenceArrayLayout()) {
                    visitReferenceArray(origin);
                } else if (specificLayout.isHybridLayout()) {
                    TupleReferenceMap.visitReferences(hub, origin, pointerIndexTagger);
                }

                return cell.plus(Layout.size(origin));
            }

            /**
             * Sets pointer tagging (true) or untagging (false) operation mode.
             */
            void SetOperationMode(ActionMode actionMode) {
                pointerIndexTagger.actionMode = actionMode;
            }
        }

        /**
         * Cell pointer (un)tagger.
         */
        private static final CellPointerTagger cellPointerTagger = new CellPointerTagger();

        /**
         * Roots (un)tagger.
         */
        private static final SequentialHeapRootsScanner heapRootsTagger =
            new SequentialHeapRootsScanner(CellPointerTagger.pointerIndexTagger);

        /**
         * Pointer (un)tagging operation constructor.
         */
        protected PointerTaggingOperation() { super("PointerTaggingOperation", null, Mode.Safepoint);}

        @Override
        protected void doAfterFrozen(VmThread vmThread) {
            Pointer tla = vmThread.tla();
            final boolean threadWasInNative = LOWEST_ACTIVE_STACK_SLOT_ADDRESS.load(tla).isZero();

            if (threadWasInNative) {
                VmThreadLocal.prepareStackReferenceMap(tla);
            } else {
                final StackReferenceMapPreparer stackReferenceMapPreparer = vmThread.stackReferenceMapPreparer();
                stackReferenceMapPreparer.completeStackReferenceMap(tla);
            }
        }

        @Override
        protected void doBeforeThawingThread(VmThread thread) {
            LOWEST_ACTIVE_STACK_SLOT_ADDRESS.store3(thread.tla(), Address.zero());
        }

        @Override
        protected void doIt() {
            cellPointerTagger.SetOperationMode(cellPointerTagger.pointerIndexTagger.actionMode);
            VmThreadLocal.prepareCurrentStackReferenceMap();
            Heap.invokeGCCallbacks(Heap.GCCallbackPhase.BEFORE);
            if (TraceMaxSimTagging) {
                Log.println(cellPointerTagger.pointerIndexTagger.actionMode + " of heap roots.");
            }
            heapRootsTagger.run();
            if (TraceMaxSimTagging) {
                Log.println(cellPointerTagger.pointerIndexTagger.actionMode + " of code.");
            }
            Code.visitCells(cellPointerTagger, true);
            if (TraceMaxSimTagging) {
                Log.println(cellPointerTagger.pointerIndexTagger.actionMode + " of heap.");
            }
            VMConfiguration.vmConfig().heapScheme().walkHeap(cellPointerTagger);
            Heap.invokeGCCallbacks(Heap.GCCallbackPhase.AFTER);
        }

        /**
         * Do pointer (un)tagging.
         */
        protected void doTagging(ActionMode actionMode) {
            cellPointerTagger.SetOperationMode(actionMode);
            submit();
        }
    }

    /**
     * Pointer (un)tagging VM operation.
     */
    private static final PointerTaggingOperation pointerTaggingOperation = new PointerTaggingOperation();

    /**
     * Indicator that pointer (un)tagging is in progress
     */
    private static boolean isPointerTaggingInProgress = false;

    /**
     * Indicator that verification of pointer (un)tagging with correction should be performed (for debugging purposes).
     */
    private static final boolean doTaggingVerificationWithCorrection = false;

    /**
     * Action mode.
     */
    private enum ActionMode{
        MaxSimPointerTagging,
        MaxSimPointerUntagging,
        MaxSimPointerTaggingVerification,
        MaxSimPointerUntaggingVerification
    }

    /**
     * Flag enabling/disabling tracing of MaxSim (un)tagging.
     */
    private static boolean TraceMaxSimTagging;
    static {
        VMOptions.addFieldOption("-XX:", "TraceMaxSimTagging", MaxSimTaggingScheme.class, "Trace MaxSim tagging.");
    }

    /**
     * Comparison of untagged objects.
     *
     * NOTE: The places for insertion of untagged comparison were identified empirically.
     * NOTE: All the code dynamically reachable from doTagging should perform untagged pointers comparison.
     *
     * Returns true if objects are equal ignoring tags,false otherwise.
     */
    @INLINE
    public static boolean compareUntaggedObjects(Object object1, Object object2) {
        if (MaxSimInterfaceHelpers.isTaggingEnabled() && !MaxineVM.isHosted()) {
            return Pointer.equalsUntagged(ObjectAccess.toOrigin(object1), ObjectAccess.toOrigin(object2));
        } else {
            return object1 == object2;
        }
    }

    /**
     * Return true if (un)tagging is in progress, false otherwise.
     */
    @INLINE
    public static boolean isTaggingInProgress() {
        return isPointerTaggingInProgress;
    }

    /**
     * Do tagging.
     */
    static public void doTagging() {
        assert isPointerTaggingInProgress == false;
        isPointerTaggingInProgress = true;
        pointerTaggingOperation.doTagging(ActionMode.MaxSimPointerTagging);
        isPointerTaggingInProgress = false;
        if (doTaggingVerificationWithCorrection) {
            doTaggingVerificationWithCorrection();
        }
    }

    /**
     * Do tagging verification.
     */
    static public void doTaggingVerificationWithCorrection() {
        assert isPointerTaggingInProgress == false;
        pointerTaggingOperation.doTagging(ActionMode.MaxSimPointerTaggingVerification);
    }

    /**
     * Do untagging.
     */
    static public void doUntagging() {
        assert isPointerTaggingInProgress == false;
        isPointerTaggingInProgress = true;
        pointerTaggingOperation.doTagging(ActionMode.MaxSimPointerUntagging);
        isPointerTaggingInProgress = false;
        if (doTaggingVerificationWithCorrection) {
            doUntaggingVerificationWithCorrection();
        }
    }

    /**
     * Do untagging verification.
     */
    static public void doUntaggingVerificationWithCorrection() {
        assert isPointerTaggingInProgress == false;
        pointerTaggingOperation.doTagging(ActionMode.MaxSimPointerUntaggingVerification);
    }

    static final MaxSimHubTagUpdater maximHubTagUpdater = new MaxSimHubTagUpdater();

    /**
     * MaxSim hub tag updater.
     */
    static class MaxSimHubTagUpdater implements ClassActor.Closure {
        public boolean doClass(ClassActor classActor) {
            DynamicHub hub = classActor.dynamicHub();
            if (hub == null) {
                return true;
            }
            hub.setMaxSimHubTag(defineMaxSimHubTag(hub));
            return true;
        }
    }

    /**
     * Converts class ID to tag.
     */
    @INLINE
    public static short classIDToTag(int classId) {
        if (MaxineVM.isDebug() && (classId < 0)) {
            FatalError.unexpected("Class ID passed to classIDToTag should not be negative!");
        }
        int tag = classId + MaxSimInterface.PointerTag.TAG_GP_LO_VALUE;
        if ((tag >= MaxSimInterface.PointerTag.DEFINED_TAGS_NUM_VALUE)) {
            return MaxSimInterface.PointerTag.TAG_UNDEFINED_GP_VALUE;
        }
        return (short) tag;
    }

    /**
     * Converts tag to class ID.
     */
    @INLINE
    public static int tagToClassID(short tag) {
        if (MaxSimInterfaceHelpers.isGeneralPurposeTag(tag)) {
            return UnsafeCast.asInt((short) (tag - MaxSimInterface.PointerTag.TAG_GP_LO_VALUE));
        }
        return ClassIDManager.NULL_CLASS_ID;
    }

    /**
     * Defines MaxSim tag associated with a hub.
     */
    static public short defineMaxSimHubTag(Hub hub) {
        if (hub.isStatic) {
            return MaxSimInterface.PointerTag.TAG_STATIC_VALUE;
        } else {
            return classIDToTag(hub.classActor.id);
        }
    }

    /**
     * Sets tag using a hub accessible from an object pointer.
     */
    @INLINE
    static public Pointer setTagUsingObjectHub(Pointer p) {
        if (MaxSimPlatform.isPointerTaggingGenerative()) {
            final Hub hub = Layout.getHub(p);
            if (hub.isStatic) {
                return p.tagSet((short) MaxSimInterface.PointerTag.TAG_STATIC_VALUE);
            }
            if (MaxSimInterfaceHelpers.isClassIDTagging()) {
                final short tag = hub.getMaxSimHubTag();
                return p.tagSet(tag);
            } else if (MaxSimInterfaceHelpers.isAllocationSiteIDTagging()) {
                return p.tagSet((short) MaxSimInterface.PointerTag.TAG_UNDEFINED_GP_VALUE);
            } else {
                FatalError.unimplemented();
            }

        }
        return p;
    }

    /**
     * Sets tag during allocation.
     */
    @INLINE
    static public Pointer setTagDuringAllocation(Pointer p, short tag) {
        if (MaxSimPlatform.isPointerTaggingGenerative()) {
            if (MaxSimInterfaceHelpers.isClassIDTagging()) {
                p = p.tagSet(tag);
            } else if (MaxSimInterfaceHelpers.isAllocationSiteIDTagging()) {
                short allocationSiteEstimationId = MaxSimMediator.getAllocationSiteEstimationId(tag);
                p = p.tagSet(allocationSiteEstimationId);
            } else {
                FatalError.unimplemented();
            }
        }
        return p;
    }

    /**
     * Sets tag during copying garbage collection.
     */
    @INLINE
    static public Pointer setTagDuringCopyingGC(Pointer p, Reference fromRef) {
        if (MaxSimPlatform.isPointerTaggingGenerative()) {
            if (MaxSimInterfaceHelpers.isClassIDTagging() ||
                MaxSimInterfaceHelpers.isAllocationSiteIDTagging()) {
                final short fromTag = fromRef.toOrigin().tagGet();
                p = p.tagSet(fromTag);
            } else {
                FatalError.unimplemented();
            }
        }
        return p;
    }

    /**
     * Sets tag during code cell visit.
     */
    @INLINE
    static public Pointer setTagDuringCodeCellVisit(Pointer p, TargetBundleLayout.ArrayField field) {
        if (MaxSimPlatform.isPointerTaggingGenerative()) {
            switch (field) {
                case scalarLiterals:
                case referenceLiterals:
                case code:
                    p = p.tagSet((short) MaxSimInterface.PointerTag.TAG_CODE_VALUE);
                    break;
                default:
                    if (MaxineVM.isDebug()) {
                        FatalError.unexpected("Unexpected ArrayField!");
                    }
            }
        }
        return p;
    }
}
