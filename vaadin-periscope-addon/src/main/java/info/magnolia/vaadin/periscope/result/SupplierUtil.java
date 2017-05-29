/**
 * This file Copyright (c) 2017 Magnolia International
 * Ltd.  (http://www.magnolia-cms.com). All rights reserved.
 *
 *
 * This file is dual-licensed under both the Magnolia
 * Network Agreement and the GNU General Public License.
 * You may elect to use one or the other of these licenses.
 *
 * This file is distributed in the hope that it will be
 * useful, but AS-IS and WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, or NONINFRINGEMENT.
 * Redistribution, except as permitted by whichever of the GPL
 * or MNA you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or
 * modify this file under the terms of the GNU General
 * Public License, Version 3, as published by the Free Software
 * Foundation.  You should have received a copy of the GNU
 * General Public License, Version 3 along with this program;
 * if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * 2. For the Magnolia Network Agreement (MNA), this file
 * and the accompanying materials are made available under the
 * terms of the MNA which accompanies this distribution, and
 * is available at http://www.magnolia-cms.com/mna.html
 *
 * Any modifications to this file must keep this entire header
 * intact.
 *
 */
package info.magnolia.vaadin.periscope.result;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;

/**
 * Convenience methods for result suppliers.
 */
public abstract class SupplierUtil {

    /**
     * Highlight (using HTML tags) all occurrences of a query string, ignoring case.
     *
     * @param text Text in which parts should be highlighted
     * @param query Parts to highlight
     * @return Highlighted string
     */
    public static String highlight(final String text, final String query) {
        if (StringUtils.isBlank(query)) {
            return text;
        }

        final List<Integer> startIndices = allIndicesOf(text, query);
        final List<Integer> endIndices = startIndices.stream().map(i -> i + query.length()).collect(toList());

        // we run back to front to not mess up indices when inserting tags
        Collections.reverse(startIndices);
        Collections.reverse(endIndices);
        Queue<Integer> startQueue = new LinkedList<>(startIndices);
        Queue<Integer> endQueue = new LinkedList<>(endIndices);

        StringBuilder highlighted = new StringBuilder(text);
        while (!startQueue.isEmpty() || !endQueue.isEmpty()) {
            final Integer startCandidate = startQueue.peek();
            final Integer endCandidate = endQueue.peek();

            if (startCandidate != null && (endCandidate == null || startCandidate > endCandidate)) {
                highlighted.insert(startCandidate, "<strong>");
                startQueue.poll();
            } else {
                highlighted.insert(endCandidate, "</strong>");
                endQueue.poll();
            }
        }

        return highlighted.toString();
    }

    private static List<Integer> allIndicesOf(final String text, final String query) {
        final List<Integer> indices = new ArrayList<>();
        int index = StringUtils.indexOfIgnoreCase(text, query);
        while (index >= 0) {
            indices.add(index);
            index = StringUtils.indexOfIgnoreCase(text, query, index + 1);
        }
        return indices;
    }
}
