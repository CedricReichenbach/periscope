package info.magnolia.vaadin.periscope.speech;

import info.magnolia.speech.GoogleSpeechRecognitionService;
import info.magnolia.vaadin.speech.AudioRecorder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.ui.AbstractComponent;

public class GoogleSpeechRecognizer extends AbstractComponent implements SpeechRecognizer {

    final AudioRecorder audioRecorder;
    final GoogleSpeechRecognitionService googleSpeechRecognitionService;

    private final Set<Consumer<String>> listeners = new HashSet<>();

    public GoogleSpeechRecognizer() {
        this.audioRecorder = new AudioRecorder();
        this.googleSpeechRecognitionService = new GoogleSpeechRecognitionService();
        audioRecorder.addValueChangeListener((AudioRecorder.ValueChangeListener) wavBinary -> {
            Optional<String> recognisedAudio = googleSpeechRecognitionService.recognise(wavBinary);
            if (recognisedAudio.isPresent()) {
                String speech = recognisedAudio.get();
                listeners.forEach(listener -> listener.accept(speech));
            }
        });
    }

    @Override
    public void record() {
        audioRecorder.record();
        try {
            // FIXME: Find a better solution
            Thread.sleep(2000);
            audioRecorder.stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addSpeechResultListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeSpeechResultListener(Consumer<String> listener) {
        listeners.remove(listener);
    }
}
