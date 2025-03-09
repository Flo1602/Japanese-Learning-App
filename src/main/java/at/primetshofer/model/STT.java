package at.primetshofer.model;

import at.primetshofer.logic.tracing.verification.VerificationLogic;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.apache.log4j.Logger;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;

public class STT {

    private static final Logger logger = Logger.getLogger(STT.class);
    private static STT stt = null;
    private final BooleanProperty sttCompleted;
    private String transcript;

    private STT() {
        sttCompleted = new SimpleBooleanProperty(false);
    }

    public static STT getStt() {
        if (stt == null) {
            stt = new STT();
        }
        return stt;
    }

    public void convertAudio(String audioPath) {
        sttCompleted.set(false);
        LibVosk.setLogLevel(LogLevel.WARNINGS);

        try {
            Model model = new Model("vosk-model-ja-0.22"); // Pfad zum Modell

            // Datei mit Audiodaten laden
            File audioFile = new File(audioPath); // Pfad zur WAV-Datei
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

            Recognizer recognizer = new Recognizer(model, AudioRecorder.SAMPLE_RATE);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = audioStream.read(buffer)) != -1) {
                recognizer.acceptWaveForm(buffer, bytesRead);
            }

            transcript = recognizer.getFinalResult();

            recognizer.close();
            model.close();

        } catch (Exception ex) {
            logger.error("Failed to convert audio to text", ex);
        } finally {
            sttCompleted.set(true);
        }
    }

    public String getTranscript() {
        return transcript;
    }

    public BooleanProperty sttCompletedProperty() {
        return sttCompleted;
    }
}
