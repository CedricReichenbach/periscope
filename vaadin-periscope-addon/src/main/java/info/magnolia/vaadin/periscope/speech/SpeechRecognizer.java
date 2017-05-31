package info.magnolia.vaadin.periscope.speech;

import java.util.function.Consumer;

import com.vaadin.ui.Component;

public interface SpeechRecognizer extends Component {
    void record();

    void addListener(Consumer<String> listener);

    void removeListener(Consumer<String> listener);
}
