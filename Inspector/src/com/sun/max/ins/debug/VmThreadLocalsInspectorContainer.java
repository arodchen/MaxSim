/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.ins.debug;

import javax.swing.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.value.*;

/**
 * A tabbed container for {@link VmThreadLocalsInspector}s.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class VmThreadLocalsInspectorContainer extends TabbedInspector<VmThreadLocalsInspector, VmThreadLocalsInspectorContainer> {

    /**
     * @return the singleton container instance, if it exists; null otherwise.
     */
    public static VmThreadLocalsInspectorContainer get(Inspection inspection) {
        return UniqueInspector.find(inspection, VmThreadLocalsInspectorContainer.class);
    }

    /**
     * Return the singleton ThreadLocalsInspectorContainer, creating it if necessary.
     * Policy is to populate container automatically at creation with an inspector for each existing thread.
     */
    private static VmThreadLocalsInspectorContainer make(Inspection inspection) {
        VmThreadLocalsInspectorContainer threadLocalsInspectorContainer = get(inspection);
        if (threadLocalsInspectorContainer == null) {
            Trace.begin(1, "[VmThreadLocalsInspector] initializing");
            threadLocalsInspectorContainer = new VmThreadLocalsInspectorContainer(inspection, Residence.INTERNAL);
            for (TeleNativeThread thread : inspection.teleVM().threads()) {
                threadLocalsInspectorContainer.add(new VmThreadLocalsInspector(inspection, thread, threadLocalsInspectorContainer));
            }
            Trace.end(1, "[VmThreadLocalsInspector] initializing");
        }
        threadLocalsInspectorContainer.updateThreadFocus(inspection.focus().thread());
        return threadLocalsInspectorContainer;
    }


    /**
     * Find an existing thread locals inspector for a thread in the {@link TeleVM}.
     * Will create a fully populated ThreadLocalsInspectorContainer if one doesn't already exist.
     *
     * @return null if thread locals inspector for thread doesn't exist
     */
    public static VmThreadLocalsInspector getInspector(Inspection inspection, TeleNativeThread teleNativeThread) {
        final VmThreadLocalsInspectorContainer threadLocalsInspectorContainer = make(inspection);
        for (VmThreadLocalsInspector threadLocalsInspector : threadLocalsInspectorContainer) {
            if (threadLocalsInspector.teleNativeThread().equals(teleNativeThread)) {
                return threadLocalsInspector;
            }
        }
        return null;
    }

    /**
     * Returns the registers inspector for a thread in the {@link TeleVM}, creating it if necessary.
     * Creates a ThreadLocalsInspectorContainer, if one doesn't already exist, and populates
     * it with registers inspectors for every existing thread.
     */
    public static VmThreadLocalsInspector makeInspector(Inspection inspection, TeleNativeThread teleNativeThread) {
        final VmThreadLocalsInspectorContainer threadLocalsInspectorContainer = make(inspection);
        // if the container is newly created, it will create an inspector for every thread, so wait to check
        VmThreadLocalsInspector threadLocalsInspector = getInspector(inspection, teleNativeThread);
        if (threadLocalsInspector == null) {
            threadLocalsInspector = new VmThreadLocalsInspector(inspection, teleNativeThread, threadLocalsInspectorContainer);
            threadLocalsInspectorContainer.add(threadLocalsInspector);
        }
        return threadLocalsInspector;
    }

    private boolean _threadSetNeedsUpdate;

    @Override
    public void threadSetChanged(long epoch) {
        _threadSetNeedsUpdate = true;
        super.threadSetChanged(epoch);
    }

    private final ChangeListener _tabChangeListener = new ChangeListener() {

        public void stateChanged(ChangeEvent event) {
            // A ThreadLocalsInspector tab has become visible that was not visible before.
            final VmThreadLocalsInspector selectedInspector = getSelected();
            // Decide whether to propagate the new thread selection.
            // If the new tab selection agrees with the global thread
            // selection, then this change is just an initialization or update notification.
            if (selectedInspector != null) {
                inspection().focus().setThread(selectedInspector.teleNativeThread());
            }
        }
    };

    private VmThreadLocalsInspectorContainer(Inspection inspection, Residence residence) {
        super(inspection, residence, inspection.geometry().threadLocalsFrameDefaultLocation(), inspection.geometry().threadLocalsFramePrefSize(), "threadLocalsInspector");
        _threadSetNeedsUpdate = true;
        addChangeListener(_tabChangeListener);
    }

    @Override
    public String getTextForTitle() {
        return "Thread Locals";
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        if (isShowing() || force) {
            if (_threadSetNeedsUpdate || force) {
                // Mark all inspectors for possible deletion if their thread is no longer active
                for (VmThreadLocalsInspector threadLocalsInspector : this) {
                    threadLocalsInspector.setMarked(true);
                }
                for (TeleNativeThread thread : teleVM().threads()) {
                    final UniqueInspector.Key<VmThreadLocalsInspector> key = UniqueInspector.Key.create(VmThreadLocalsInspector.class, LongValue.from(thread.id()));
                    final VmThreadLocalsInspector threadLocalsInspector = UniqueInspector.find(inspection(), key);
                    if (threadLocalsInspector == null) {
                        add(new VmThreadLocalsInspector(inspection(), thread, this));
                    } else {
                        threadLocalsInspector.setMarked(false);
                    }
                }
                // Any remaining marked inspectors should be deleted as the threads have gone away
                for (VmThreadLocalsInspector threadLocalsInspector : this) {
                    if (threadLocalsInspector.marked()) {
                        close(threadLocalsInspector);
                    }
                }
                _threadSetNeedsUpdate = false;
            }
            updateThreadFocus(focus().thread());
            super.refreshView(epoch, force);
        }
    }

    @Override
    public void add(VmThreadLocalsInspector threadLocalsInspector) {
        super.add(threadLocalsInspector, threadLocalsInspector.getTextForTitle());
        threadLocalsInspector.frame().invalidate();
        threadLocalsInspector.frame().repaint();
    }


    @Override
    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
        updateThreadFocus(teleNativeThread);
    }

    /**
     * Change the selected tab, if needed, to agree with the global thread selection.
     */
    public void updateThreadFocus(TeleNativeThread selectedThread) {
        for (VmThreadLocalsInspector inspector : this) {
            if (inspector.teleNativeThread().equals(selectedThread)) {
                if (!isSelected(inspector)) {
                    // Select and highlight the tabbed inspector; triggers change event  that will cause it to be refreshed.
                    inspector.highlight();
                }
                break;
            }
        }
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        removeChangeListener(_tabChangeListener);
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        dispose();
    }

}
