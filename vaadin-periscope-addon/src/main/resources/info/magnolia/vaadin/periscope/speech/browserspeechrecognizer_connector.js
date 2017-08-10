window.info_magnolia_vaadin_periscope_speech_BrowserSpeechRecognizer = function() {
    var SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    var SpeechGrammarList = window.SpeechGrammarList || window.webkitSpeechGrammarList;
    var SpeechRecognitionEvent = window.SpeechRecognitionEvent || window.webkitSpeechRecognitionEvent;

    var self = this;

    if (!SpeechRecognition) {
        throw "No speech recognition support in this browser";
    }

    var recognition = new SpeechRecognition();

    recognition.onresult = function(event) {
        var transcript = event.results[0][0].transcript;
        self.speechRecorded(transcript);
    };

    this.record = function() {
        recognition.start();
    }
};
