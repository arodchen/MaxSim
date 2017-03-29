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
package test.output;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class MaxSimSingleLinkedList {

    private static final int linksNum = 4096;
    private static Random randNumGen = new Random( );
    private static SingleLink [ ] array = new SingleLink [ linksNum ];

    private static SingleLink first = null;
    private static SingleLink last = null;

    private static class SingleLink {
        private SingleLink next;
        private long longData;

        private SingleLink(long data) {
            longData = data;
        }
    }

    private static Comparator < SingleLink > SingleLinkDescendingCmp =
    new Comparator < SingleLink > ( ) {
            @Override
            public int compare( SingleLink l1, SingleLink l2) {
                long l1Data = l1.longData;
                long l2Data = l2.longData;
                if (l1Data != l2Data) {
                    return l1Data < l2Data ? 1 : -1;
                }
                return 0;
            }
        };

    public static void addLink(SingleLink link, int index) {
        assert link.next == null;
        if ( first == null ) {
            assert last == null;
            first = link;
            last = link;
        }
        last.next = link;
        last = link;

        array [ index ] = link;
    }

    public static void reLink( ) {
        first = array [ 0 ];
        for ( int i = 0; i < linksNum - 1; i ++ ) {
            array [ i ].next = array [ i + 1 ];
        }
        array [ linksNum - 1 ].next = null;
        last = array [ linksNum - 1 ];
    }

    public static void main(String [ ] args) {
        long lastData;
        SingleLink lastLink;
        SingleLink link;


        // 1. Create list with random data.
        System.out.print("1. Create list ... ");
        for ( int i = 0; i < linksNum; i ++ ) {
            SingleLink l = new SingleLink( randNumGen.nextLong());
            addLink( l, i);
        }
        System.out.println("done!");

        // 2. Find end element.
        System.out.print("2. Find end element ... ");
        lastData = last.longData;
        lastLink = last;
        link = first;
        System.setProperty("MaxSim.Command", "ROI_BEGIN()");
        while ( link.longData != lastData && link != lastLink ) {
            link = link.next;
        }
        System.setProperty("MaxSim.Command", "ROI_END()");
        System.setProperty("MaxSim.Command", "PRINT_PROFILE_TO_FILE(SingleLinkedList.2.zsim-prof.db)");
        System.setProperty("MaxSim.Command", "RESET_PROFILE_COLLECTION()");
        System.out.println("done!");

        // 3. Sort elements.
        System.out.print("3. Sort elements ... ");
        System.setProperty("MaxSim.Command", "ROI_BEGIN()");
        Arrays.sort( array, SingleLinkDescendingCmp);
        System.setProperty("MaxSim.Command", "ROI_END()");
        System.setProperty("MaxSim.Command", "PRINT_PROFILE_TO_FILE(SingleLinkedList.3.zsim-prof.db)");
        System.setProperty("MaxSim.Command", "RESET_PROFILE_COLLECTION()");
        System.out.println("done!");

        // 4. Relink elements.
        System.out.print("4. Relink ... ");
        reLink( );
        System.out.println("done!");

        // 5. Find end element.
        System.out.print("5. Find end element ... ");
        lastData = last.longData;
        lastLink = last;
        link = first;
        System.setProperty("MaxSim.Command", "ROI_BEGIN()");
        while ( link.longData != lastData && link != lastLink ) {
            link = link.next;
        }
        System.setProperty("MaxSim.Command", "ROI_END()");
        System.setProperty("MaxSim.Command", "PRINT_PROFILE_TO_FILE(SingleLinkedList.5.zsim-prof.db)");
        System.setProperty("MaxSim.Command", "RESET_PROFILE_COLLECTION()");
        System.out.println("done!");
    }
}
