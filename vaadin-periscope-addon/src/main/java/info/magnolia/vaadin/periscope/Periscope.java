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

import info.magnolia.vaadin.periscope.order.NeuralNetworkManager;
import info.magnolia.vaadin.periscope.result.AsyncResultSupplier;
import info.magnolia.vaadin.periscope.result.Result;
import info.magnolia.vaadin.periscope.result.ResultSupplier;
import info.magnolia.vaadin.periscope.result.SearchFailedException;
import info.magnolia.vaadin.periscope.speech.BrowserSpeechRecognizer;
import info.magnolia.vaadin.periscope.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.Lists;
import com.vaadin.annotations.StyleSheet;
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
@StyleSheet("vaadin://addons/periscope/css/ionicons.min.css")
public class Periscope extends VerticalLayout {

    private final Collection<ResultSupplier> resultSuppliers;
    private final Collection<AsyncResultSupplier> asyncResultSuppliers;
    private final Collection<CompletableFuture> runningAsyncSearches = new ArrayList<>();

    private final TextField input;
    private final ResultList resultList;
    private final SpeechRecognizer speechRecognizer;
    private final NeuralNetworkManager resultsNetworkManager;

    public Periscope(final Collection<ResultSupplier> resultSuppliers, final Collection<AsyncResultSupplier> asyncResultSuppliers) {
        this(resultSuppliers, asyncResultSuppliers, new BrowserSpeechRecognizer());
    }

    public Periscope(final Collection<ResultSupplier> resultSuppliers, final Collection<AsyncResultSupplier> asyncResultSuppliers, final SpeechRecognizer speechRecognizer) {
        super();

        this.resultSuppliers = resultSuppliers;
        this.asyncResultSuppliers = asyncResultSuppliers;
        this.speechRecognizer = speechRecognizer;
        this.resultsNetworkManager = new NeuralNetworkManager();

        this.addStyleName("periscope");

        input = new TextField();
        input.setInputPrompt("Type ahead to search...");
        input.addStyleName("search-field");
        this.addComponent(input);

        resultList = new ResultList();
        this.addComponent(resultList.getLayout());

        input.addTextChangeListener(event -> consumeQuery(event.getText(), false));
        input.setTextChangeEventMode(AbstractTextField.TextChangeEventMode.EAGER);

        input.addShortcutListener(createInputShortcut(ShortcutAction.KeyCode.ARROW_DOWN, () -> resultList.moveSelector(1)));
        input.addShortcutListener(createInputShortcut(ShortcutAction.KeyCode.ARROW_UP, () -> resultList.moveSelector(-1)));
        input.addShortcutListener(createInputShortcut(ShortcutAction.KeyCode.ENTER, () -> {
            final Optional<Result> selectedOrFirstResult = resultList.getSelectedOrFirstResult();
            try {
                selectedOrFirstResult.ifPresent(this::resultPicked);
            } catch (SearchFailedException e) {
                changeResultListToReflectException();
            }

            // TODO: Find a way to blur input (remove focus)
        }));
        input.addBlurListener(event -> resultList.clearSelector());

        resultList.onResultPick(this::resultPicked);

        this.addComponent(createSpeechButton());
    }

    private void resultPicked(Result result) {
        resultsNetworkManager.train(input.getValue(), result);
        result.getAction().run();
    }

    private synchronized void consumeQuery(final String query, final boolean autoExecuteFirst) {
        // FIXME: Properly use push instead
        getUI().setPollInterval(500);

        runningAsyncSearches.forEach(search -> search.cancel(true));

        resultList.clear();

        for (final ResultSupplier supplier : resultSuppliers) {
            List<Result> results = supplier.search(query);

            resultsNetworkManager.addResults(results);
            resultsNetworkManager.sort(query, results);

            if (autoExecuteFirst && !results.isEmpty()) {
                // typically a case of vocal command
                try {
                    results.get(0).getAction().run();
                }
                // TODO: shall be lesser scoped.
                catch (Exception e) {
                    changeResultListToReflectException();
                }
                return;
            }

            resultList.appendResults(supplier.getTitle(), results);
        }

        final AtomicBoolean autoExecuteDone = new AtomicBoolean(false);
        asyncResultSuppliers.forEach(supplier -> {
            resultList.showLoadingIcon();

            // XXX: Synchronize?
            final CompletableFuture<List<Result>> search = supplier.search(query);
            runningAsyncSearches.add(search);
            search.thenAccept(results -> {
                        resultsNetworkManager.addResults(results);
                        resultsNetworkManager.sort(query, results);

                        resultList.hideLoadingIcon();
                        runningAsyncSearches.remove(search);

                        if (autoExecuteFirst && autoExecuteDone.get()) {
                            return;
                        }

                        if (autoExecuteFirst && !results.isEmpty()) {
                            // typically a case of vocal command
                            try {
                                results.get(0).getAction().run();
                            }
                            // TODO: shall be lesser scoped.
                            catch (Exception e) {
                                changeResultListToReflectException();
                            }
                            autoExecuteDone.set(true);
                            return;
                        }

                        resultList.appendResults(supplier.getTitle(), results);
                        getUI().push();
                    }
            );
        });
    }

    private void changeResultListToReflectException() {
        resultList.clear();
        // TODO: Have to be more specific e.g. red etc.
        resultList.appendResults("Command result", Lists.newArrayList(new Result("Voice Command failed.", () -> {
        })));
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
        final Button startStopButton = new Button();
        startStopButton.addStyleName("record-button");
        startStopButton.setCaptionAsHtml(true);
        startStopButton.setCaption("<span class=\"ion-mic-a\"></span>");
        startStopButton.setClickShortcut(ShortcutAction.KeyCode.R, ShortcutAction.ModifierKey.SHIFT, ShortcutAction.ModifierKey.ALT);

        final AtomicBoolean isRecording = new AtomicBoolean(false);

        speechRecognizer.addSpeechResultListener(transcript -> {
            input.setValue(transcript);
            this.consumeQuery(transcript, true);

            startStopButton.removeStyleName("recording");
            isRecording.set(false);
        });

        startStopButton.addClickListener((Button.ClickListener) event -> {
            if (isRecording.get()) {
                return;
            }

            speechRecognizer.record();

            startStopButton.addStyleName("recording");
            isRecording.set(true);
        });

        final VerticalLayout speechWrapper = new VerticalLayout(startStopButton, speechRecognizer);
        speechWrapper.addStyleName("speech-recognition");
        return speechWrapper;
    }
}
