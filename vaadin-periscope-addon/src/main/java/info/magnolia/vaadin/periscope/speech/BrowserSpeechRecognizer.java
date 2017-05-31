package info.magnolia.vaadin.periscope.speech;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.annotations.JavaScript;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;

/**
 * Component which listens for user speech and eventually provides a transcript of it.
 */
@JavaScript("browserspeechrecognizer_connector.js")
public class BrowserSpeechRecognizer extends AbstractJavaScriptComponent implements SpeechRecognizer {

    private final Set<Consumer<String>> listeners = new HashSet<>();

    public BrowserSpeechRecognizer() {
        this.addFunction("speechRecorded", (JavaScriptFunction) transcript -> {
            listeners.forEach(listener -> listener.accept(transcript.getString(0)));
        });
    }

    @Override
    public void record() {
        this.callFunction("record");
    }

    @Override
    public void addSpeechResultListener(Consumer<String> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeSpeechResultListener(Consumer<String> listener) {
        this.listeners.remove(listener);
    }
}
