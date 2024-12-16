package at.primetshofer.model;

import at.primetshofer.model.Trainer.KanjiTrainer;
import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.KanjiProgress;
import at.primetshofer.model.entities.Settings;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.Stylesheet;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Controller {

    private static final int SETTING_ID = 0;

    private static Controller instance;

    private Settings settings;
    private EntityManager em;
    private MediaPlayer mediaPlayer;
    private KanjiTrainer kanjiTrainer;

    private Controller() {
        em = HibernateUtil.getEntityManager();
        kanjiTrainer = KanjiTrainer.getInstance();
    }

    public static Controller getInstance() {
        if (instance == null) {
            instance = new Controller();
        }
        return instance;
    }

    public Settings getSettings() {
        if(settings == null) {
            EntityManager entityManager = HibernateUtil.getEntityManager();

            settings = entityManager.find(Settings.class, SETTING_ID);

            if(settings == null) {
                settings = new Settings();
                settings.setId(SETTING_ID);
                settings.setNewWords(20);
                settings.setStyleSheet(Stylesheet.CUPERTINO_DARK);
                settings.setVoiceId(0);

                HibernateUtil.startTransaction();
                entityManager.persist(settings);
                HibernateUtil.commitTransaction();

                settings = getSettings();
            }
        }

        return settings;
    }

    public List<Word> getWordsWithoutSentences() {
        String jpql = "SELECT w FROM Word w WHERE w.active = true AND w.sentences IS EMPTY";
        TypedQuery<Word> query = em.createQuery(jpql, Word.class);
        return query.getResultList();
    }

    public List<Word> getWordsWithoutQuestions() {
        String jpql = "SELECT w FROM Word w WHERE w.active = true AND w.questions IS EMPTY";
        TypedQuery<Word> query = em.createQuery(jpql, Word.class);
        return query.getResultList();
    }

    public void saveSettings() {
        EntityManager entityManager = HibernateUtil.getEntityManager();
        entityManager.getTransaction().begin();
        entityManager.merge(settings);
        entityManager.getTransaction().commit();
    }

    public void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    public void copyToClipboard(File file) {
        Platform.runLater(() ->{
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putFiles(List.of(file));
            clipboard.setContent(content);
        });
    }

    public File getWordListCSV() throws IOException {
        String jpql = "SELECT w FROM Word w WHERE w.active = true";
        TypedQuery<Word> query = em.createQuery(jpql, Word.class);
        List<Word> words = query.getResultList();

        File base = new File("basic_japanese_words.csv");
        File tmp = new File("japanese_words.csv");

        // Read existing words from the base CSV
        Set<String> existingWords = new HashSet<>();
        if (base.exists()) {
            try (BufferedReader reader = Files.newBufferedReader(base.toPath())) {
                existingWords = reader.lines()
                        .skip(1) // Skip header line if present
                        .map(line -> line.split(",")[0]) // Extract Japanese word
                        .collect(Collectors.toSet());
            }
        }

        // Filter out words that are already in the base CSV
        Set<String> finalExistingWords = existingWords;
        List<String[]> newWords = words.stream()
                .filter(word -> !finalExistingWords.contains(word.getJapanese()))
                .map(word -> new String[]{word.getJapanese(), word.getEnglish()})
                .collect(Collectors.toList());

        // Write to tmp CSV
        try (BufferedWriter writer = Files.newBufferedWriter(tmp.toPath())) {
            writer.write("Japanese,English\n"); // Header
            if (base.exists()) {
                Files.lines(base.toPath()).forEach(line -> {
                    try {
                        writer.write(line + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            for (String[] newWord : newWords) {
                writer.write(newWord[0] + "," + newWord[1] + "\n");
            }
        }

        return tmp;
    }

    public void playAudio(String path){
        try{
            Media media = new Media(new File(path).toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.play();
        } catch (Exception ex){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Audio not found!");
            alert.showAndWait();
        }
    }

    public void stopAudio(){
        if(mediaPlayer != null){
            mediaPlayer.stop();
        }
    }

    public Kanji getNextLearningKanji(){
        return kanjiTrainer.getNextLearningKanji();
    }

    public void addKanjiProgress(Kanji kanji, int percent) {
        kanji = kanjiTrainer.addKanjiProgress(kanji, percent);
        HibernateUtil.startTransaction();
        List<KanjiProgress> oldProgresses = kanji.getProgresses();
        kanji = compressKanjiProgress(kanji);
        for (KanjiProgress progress : oldProgresses) {
            if(!kanji.getProgresses().contains(progress)){
                em.remove(progress);
            }
        }
        em.merge(kanji);
        HibernateUtil.commitTransaction();
    }

    public void updateLists(){
        kanjiTrainer.updateKanjiList();
    }

    public double getKanjiProgress(){
        int max = kanjiTrainer.getTodayDueMax();
        if (max == 0) {
            return 100;
        }
        return (double) (100 * (max-kanjiTrainer.getTodayDueCurrent())) / max / 100;
    }

    public static Kanji compressKanjiProgress(Kanji kanji) {
        //Todo: Compression does not work
        List<KanjiProgress> kanjiProgressList = kanji.getProgresses();
        if (kanjiProgressList == null || kanjiProgressList.size() <= 3) {
            return kanji; // No compression needed
        }

        List<KanjiProgress> compressedList = new ArrayList<>();

        // Add the first entry
        compressedList.add(kanjiProgressList.get(0));

        // Compress middle entries
        KanjiProgress middleCompressed = kanjiProgressList.get(kanjiProgressList.size()-2);
        middleCompressed.setCompressedEntries(kanjiProgressList.size() - 2);
        middleCompressed.setLearned(kanjiProgressList.get(kanjiProgressList.size() - 2).getLearned()); // Use the timestamp of the second last entry
        middleCompressed.setPoints(kanjiProgressList.subList(1, kanjiProgressList.size() - 2)
                .stream()
                .mapToInt(KanjiProgress::getPoints)
                .sum());
        compressedList.add(middleCompressed);

        // Add the last entry
        compressedList.add(kanjiProgressList.get(kanjiProgressList.size() - 1));

        kanji.setProgresses(compressedList);

        return kanji;
    }
}
