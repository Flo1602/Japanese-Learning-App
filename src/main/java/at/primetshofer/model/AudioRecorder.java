package at.primetshofer.model;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class AudioRecorder {

    private static TargetDataLine targetLine;
    private static ByteArrayOutputStream byteArrayOutputStream;
    public static final int SAMPLE_RATE = 8000;
    public static final int SAMPLE_SIZE_IN_BITS = 16;
    private static Thread recordingThread;
    private static boolean recordingStarted;

    public static void startRecording() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Mikrofon wird nicht unterstützt.");
                return;
            }

            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();

            byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            final int[] bytesRead = new int[1];
            recordingStarted = true;

            // Aufnahme in einem separaten Thread
            recordingThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    bytesRead[0] = targetLine.read(buffer, 0, buffer.length);
                    byteArrayOutputStream.write(buffer, 0, bytesRead[0]);
                }
            });

            recordingThread.start();
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    public static void stopRecording(String outputFilePath) {
        if(!recordingStarted) {
            return;
        }
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
        }

        try {
            recordingThread.interrupt();
            byte[] audioData = byteArrayOutputStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(audioData);
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, 1, true, false);
            AudioInputStream audioStream = new AudioInputStream(inputStream, format, audioData.length / format.getFrameSize());

            // Audiodaten in eine WAVE-Datei schreiben
            if(outputFilePath != null){
                File outputFile = new File(outputFilePath);
                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, outputFile);
            }

            byteArrayOutputStream.close();
            recordingStarted = false;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
