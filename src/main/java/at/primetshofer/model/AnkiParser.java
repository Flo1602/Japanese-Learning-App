package at.primetshofer.model;

import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import jakarta.persistence.EntityManager;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.scene.control.Alert;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AnkiParser {

    private static final Logger logger = Logger.getLogger(AnkiParser.class);

    public static void extractApkg(String apkgPath, String outputDir) throws Exception {
        ZipFile zipFile = new ZipFile(apkgPath);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File entryDestination = new File(outputDir, entry.getName());
            entryDestination.getParentFile().mkdirs();

            if (entry.isDirectory()) {
                entryDestination.mkdirs();
            } else {
                try (InputStream in = zipFile.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(entryDestination)) {
                    in.transferTo(out);
                }
            }
        }
        zipFile.close();
    }

    // Connect to SQLite and fetch notes data
    private static List<Word> parseDatabase(String dbPath) {
        List<Word> words = new ArrayList<>();
        try {
            logger.debug("JDBC connection string: 'jdbc:sqlite:" + dbPath + "'");
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Statement statement = connection.createStatement();

            // Query notes table
            ResultSet notesResult = statement.executeQuery("SELECT flds FROM notes");
            while (notesResult.next()) {
                String fields = notesResult.getString("flds");

                // Split fields by '\x1f' separator
                String[] fieldArray = fields.split("\u001F");
                Word word = new Word();

                if (fieldArray.length > 3 && !fieldArray[3].isBlank() && !fieldArray[2].isBlank() && !fieldArray[0].isBlank()) {
                    word.setJapaneseIgnoreKanji(fieldArray[0]);
                    word.setKana(fieldArray[2]);
                    word.setEnglish(fieldArray[3]);
                    words.add(word);
                }

            }

            connection.close();
        } catch (Exception ex) {
            logger.error("Failed to parse database", ex);
        }

        return words;
    }

    private static String getDBPath(String dir){
        File directory = new File(dir);

        if(directory.exists() && directory.isDirectory()){
            File[] files = directory.listFiles();

            for (File file : files) {
                if(file.getName().matches(".*?\\.anki2.+")){
                    return file.getAbsolutePath();
                }
            }
        }

        return null;
    }

    public static void importAnki(String apkgPath, DoubleProperty finished) {
        try {
            String outputDir = "./tmp";

            // Step 1: Extract the .apkg file
            extractApkg(apkgPath, outputDir);

            // Step 2: Parse the SQLite database
            String dbPath = getDBPath(outputDir);

            logger.debug("Anki DB path: '" + dbPath + "'");

            List<Word> words = parseDatabase(dbPath);
            deleteDirectory(new File(outputDir));

            EntityManager em = HibernateUtil.getEntityManager();

            double progressValue = 1.0 / (words.size() + 1);

            for (Word word : words) {
                TTS tts = TTS.getTts();

                HibernateUtil.startTransaction();

                Word dbWord = em.merge(word);

                String ttsString = (word.getKana() == null) ? word.getJapanese() : word.getKana();

                File file = tts.synthesizeAudio(ttsString, "audio/words/" + dbWord.getId() + ".wav");
                dbWord.setTtsPath(file.getPath());
                dbWord.connectKanji();
                em.merge(dbWord);

                HibernateUtil.commitTransaction();

                Platform.runLater(() -> finished.set(finished.get() + progressValue));
            }

        } catch (Exception ex) {
            logger.error("Error while parsing file or TTS not reachable", ex);

            ViewUtils.showAlert(Alert.AlertType.ERROR,
                    LangController.getText("AnkiParseError"),
                    LangController.getText("ErrorText"));
        }

        finished.set(1);
    }

    public static void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) { // Check if it's a directory
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Recursive call for directories
                        deleteDirectory(file);
                    } else {
                        // Delete files
                        if (!file.delete()) {
                            throw new IOException("Failed to delete file: " + file);
                        }
                    }
                }
            }
            // Finally delete the directory itself
            if (!directory.delete()) {
                throw new IOException("Failed to delete directory: " + directory);
            }
        } else {
            logger.error("Directory '" + directory + "' does not exist.");
        }
    }
}
