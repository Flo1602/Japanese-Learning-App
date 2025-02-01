package at.primetshofer.model.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class JishoAudioFetcher {

    private static final String JISHO_URL = "https://jisho.org/search/";

    public static File fetchAudioURL(String japanese, int id) {
        try {
            String searchUrl = JISHO_URL + japanese;
            Document doc = Jsoup.connect(searchUrl).get();

            for (Element audioElement : doc.select("audio")) {
                String audioId = audioElement.id();
                audioId = audioId.replaceAll("audio_", "");
                audioId = audioId.split(":")[0];

                if (audioId.equals(japanese)) { // Ensure the audio file is related to the word
                    Element sourceElement = audioElement.selectFirst("source");
                    if (sourceElement != null) {
                        String audioUrl = sourceElement.attr("src");
                        return downloadAudio(audioUrl, id);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static File downloadAudio(String audioUrl, int id) {
        try {
            URL url = new URL("https:" + audioUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            File audioFile = new File("audio/words/" + id + ".mp3");
            try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(audioFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return audioFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
