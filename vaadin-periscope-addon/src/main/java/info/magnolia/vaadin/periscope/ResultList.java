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
package info.magnolia.vaadin.periscope;


import info.magnolia.vaadin.periscope.result.Result;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Manager for periscope results, internally maintaining a Vaadin component.
 */
public class ResultList {

    private final VerticalLayout layout;
    private final Component loadingIcon;

    private final Map<Component, Result> results = new HashMap<>();

    private int selectorPosition = -1;

    @Inject
    public ResultList() {
        layout = new VerticalLayout();
        layout.setImmediate(true);
        layout.addStyleName("result-list");

        loadingIcon = new Label();
        loadingIcon.addStyleName("async-loading");
    }

    public Component getLayout() {
        return layout;
    }

    void moveSelector(final int step) {
        if (step == 0) {
            return;
        }

        selectorPosition += step;

        selectorPosition = Math.max(0, selectorPosition);
        selectorPosition = Math.min(layout.getComponentCount() - 1, selectorPosition);

        // XXX: Ugly - exclusion of supplier headers
        if (selectorPosition >= 0 && layout.getComponent(selectorPosition).getStyleName().contains("supplier-heading")) {
            if (selectorPosition == 0) {
                selectorPosition++;
            } else if (selectorPosition == layout.getComponentCount() - 1) {
                selectorPosition--;
            } else {
                selectorPosition += step > 0 ? 1 : -1;
            }
        }

        clearSelector();
        if (selectorPosition >= 0) {
            layout.getComponent(selectorPosition).addStyleName("selected");
        }
    }

    Optional<Result> getSelectedOrFirstResult() {
        if (layout.getComponentCount() == 0) {
            return Optional.empty();
        }

        // XXX: A bit hacky
        final Component selectedOrFirst = StreamSupport.stream(layout.spliterator(), false)
                .filter(component -> component.getStyleName().contains("selected"))
                .findFirst().orElse(layout.getComponent(1));
        return Optional.of(results.get(selectedOrFirst));
    }

    void clearSelector() {
        layout.forEach(component -> component.removeStyleName("selected"));
    }

    void appendResults(final String title, final List<Result> results) {
        if (results.isEmpty()) {
            return;
        }

        layout.addComponent(createHeading(title));
        results.stream()
                .map(this::createResultEntry)
                .forEach(layout::addComponent);
    }

    private Component createHeading(final String name) {
        final Label label = new Label(name);
        label.setStyleName("supplier-heading");
        return label;
    }

    private Component createResultEntry(final Result result) {
        final Label icon = new Label();
        icon.setStyleName("icon " + result.getIcon().orElse(""));
        final Label text = new Label(result.getHtmlText(), ContentMode.HTML);

        final HorizontalLayout entry = new HorizontalLayout(icon, text);
        entry.setStyleName("result-entry");
        results.put(entry, result);
        return entry;
    }

    void clear() {
        layout.removeAllComponents();
        results.clear();
    }

    void showLoadingIcon() {
        layout.addComponent(loadingIcon);
    }

    void hideLoadingIcon() {
        layout.removeComponent(loadingIcon);
    }
}
