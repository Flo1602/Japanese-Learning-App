package at.primetshofer.model;

import at.primetshofer.model.entities.Settings;
import at.primetshofer.model.util.HibernateUtil;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TTS {

    private static TTS tts = null;

    private static final String VOICEVOX_BASE_URL = "http://localhost:50021"; // Default Voicevox API URL
    private static int speakerId = 0; // Adjust speaker ID based on your preferred speaker

    private TTS(){

    }

    public static TTS getTts() {
        if(tts == null){
            tts = new TTS();
        }
        return tts;
    }

    public File synthesizeAudio(String text, String savePath) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        // Step 1: Generate audio query
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        URI queryUri = URI.create(VOICEVOX_BASE_URL + "/audio_query?text=" + encodedText + "&speaker=" + speakerId);
        HttpRequest queryRequest = HttpRequest.newBuilder()
                .uri(queryUri)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> queryResponse = client.send(queryRequest, HttpResponse.BodyHandlers.ofString());
        if (queryResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to get audio query: " + queryResponse.body());
        }

        // Step 2: Synthesize audio using the generated query
        URI synthesisUri = URI.create(VOICEVOX_BASE_URL + "/synthesis?speaker=" + speakerId);
        HttpRequest synthesisRequest = HttpRequest.newBuilder()
                .uri(synthesisUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(queryResponse.body()))
                .build();

        HttpResponse<InputStream> synthesisResponse = client.send(synthesisRequest, HttpResponse.BodyHandlers.ofInputStream());
        if (synthesisResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to synthesize audio: " + synthesisResponse.body());
        }

        // Step 3: Save the audio to a .wav file

        File tmpFile = new File(savePath);

        if(!tmpFile.getParentFile().exists()){
            tmpFile.getParentFile().mkdirs();
        }
        if(!tmpFile.exists()){
            tmpFile.createNewFile();
        }

        try (InputStream inputStream = synthesisResponse.body(); OutputStream outputStream = new FileOutputStream(tmpFile)) {
            inputStream.transferTo(outputStream);
            return tmpFile;
        }
    }

    public static void updateSpeakerId() {
        speakerId = Controller.getInstance().getSettings().getVoiceId();
    }

    public static int getSpeakerId() {
        return speakerId;
    }

    public static void setSpeakerId(int speakerId) {
        TTS.speakerId = speakerId;
    }
}
