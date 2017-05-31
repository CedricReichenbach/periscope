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

import info.magnolia.vaadin.periscope.result.AsyncResultSupplier;
import info.magnolia.vaadin.periscope.result.Result;
import info.magnolia.vaadin.periscope.result.ResultSupplier;
import info.magnolia.vaadin.periscope.speech.BrowserSpeechRecognizer;
import info.magnolia.vaadin.periscope.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.vaadin.event.FieldEvents;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Periscope Vaadin component, which contains a search bar and shows live results while typing ahead.
 */
public class Periscope extends VerticalLayout {

    private final Collection<ResultSupplier> resultSuppliers;
    private final Collection<AsyncResultSupplier> asyncResultSuppliers;
    private final Collection<CompletableFuture> runningAsyncSearches = new ArrayList<>();

    private final TextField input;
    private final ResultList resultList;
    private final SpeechRecognizer speechRecognizer;

    public Periscope(final Collection<ResultSupplier> resultSuppliers, final Collection<AsyncResultSupplier> asyncResultSuppliers) {
        this(resultSuppliers, asyncResultSuppliers, new BrowserSpeechRecognizer());
    }

    public Periscope(final Collection<ResultSupplier> resultSuppliers, final Collection<AsyncResultSupplier> asyncResultSuppliers, final SpeechRecognizer speechRecognizer) {
        super();

        this.resultSuppliers = resultSuppliers;
        this.asyncResultSuppliers = asyncResultSuppliers;
        this.speechRecognizer = speechRecognizer;

        this.addStyleName("periscope");

        input = new TextField();
        input.setInputPrompt("Type ahead to search...");
        input.addStyleName("search-field");
        this.addComponent(input);

        resultList = new ResultList();
        this.addComponent(resultList.getLayout());

        input.addTextChangeListener(this::inputChanged);
        input.setTextChangeEventMode(AbstractTextField.TextChangeEventMode.EAGER);

        input.addShortcutListener(createInputShortcut(ShortcutAction.KeyCode.ARROW_DOWN, () -> resultList.moveSelector(1)));
        input.addShortcutListener(createInputShortcut(ShortcutAction.KeyCode.ARROW_UP, () -> resultList.moveSelector(-1)));
        input.addShortcutListener(createInputShortcut(ShortcutAction.KeyCode.ENTER, () -> {
            final Optional<Result> selectedOrFirstResult = resultList.getSelectedOrFirstResult();
            selectedOrFirstResult.ifPresent(result -> result.getAction().run());

            // TODO: Find a way to blur input (remove focus)
        }));
        input.addBlurListener(event -> resultList.clearSelector());

        this.addComponent(createSpeechButton());
    }

    private void inputChanged(FieldEvents.TextChangeEvent event) {
        // FIXME: Properly use push instead
        getUI().setPollInterval(500);

        runningAsyncSearches.forEach(search -> search.cancel(true));

        final String query = event.getText();

        resultList.clear();

        for (final ResultSupplier supplier : resultSuppliers) {
            List<Result> results = supplier.search(query);
            resultList.appendResults(supplier.getTitle(), results);
        }
        asyncResultSuppliers.forEach(supplier -> {
            resultList.showLoadingIcon();

            // XXX: Synchronize?
            final CompletableFuture<List<Result>> search = supplier.search(query);
            runningAsyncSearches.add(search);
            search.thenAccept(results -> {
                        resultList.appendResults(supplier.getTitle(), results);
                        resultList.hideLoadingIcon();

                        runningAsyncSearches.remove(search);

                        getUI().push();
                    }
            );
        });
    }

    private ShortcutListener createInputShortcut(final int key, final Runnable action) {
        return new ShortcutListener("Shortcut for key #" + key, key, null) {
            @Override
            public void handleAction(Object sender, Object target) {
                if (target != input) {
                    return;
                }

                action.run();
            }
        };
    }

    private Component createSpeechButton() {
        speechRecognizer.addListener(input::setValue);

        final Button startStopButton = new Button("Speech");
        startStopButton.addClickListener((Button.ClickListener) event -> {
            speechRecognizer.record();
        });

        return new VerticalLayout(startStopButton, speechRecognizer);
    }
}
