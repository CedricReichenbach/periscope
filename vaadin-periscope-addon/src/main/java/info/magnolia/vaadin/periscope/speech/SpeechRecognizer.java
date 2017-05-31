package info.magnolia.vaadin.periscope.speech;

import java.util.function.Consumer;

import com.vaadin.ui.Component;

public interface SpeechRecognizer extends Component {
    void record();

    void addSpeechResultListener(Consumer<String> listener);

    void removeSpeechResultListener(Consumer<String> listener);
}
