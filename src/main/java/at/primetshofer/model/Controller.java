package at.primetshofer.model;

import at.primetshofer.model.Trainer.KanjiTrainer;
import at.primetshofer.model.Trainer.VocabTrainer;
import at.primetshofer.model.entities.*;
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
import java.util.*;
import java.util.stream.Collectors;

public class Controller {

    private static final int SETTING_ID = 0;

    private static Controller instance;

    private Settings settings;
    private final EntityManager em;
    private MediaPlayer mediaPlayer;
    private final KanjiTrainer kanjiTrainer;
    private final VocabTrainer vocabTrainer;

    private Controller() {
        em = HibernateUtil.getEntityManager();
        kanjiTrainer = KanjiTrainer.getInstance();
        vocabTrainer = VocabTrainer.getInstance();
    }

    public static Controller getInstance() {
        if (instance == null) {
            instance = new Controller();
        }
        return instance;
    }

    private static void compressKanjiProgress(Kanji kanji) {
        List<KanjiProgress> kanjiProgressList = kanji.getProgresses();
        if (kanjiProgressList == null || kanjiProgressList.size() <= 3) {
            return;
        }

        KanjiProgress firstEntry = kanjiProgressList.get(0);
        KanjiProgress middleCompressed = kanjiProgressList.get(kanjiProgressList.size() - 2);
        KanjiProgress lastEntry = kanjiProgressList.get(kanjiProgressList.size() - 1);

        int totalPoints = 0;
        int totalRealMiddleEntries = 0;

        for (int i = 1; i < kanjiProgressList.size() - 1; i++) {
            KanjiProgress p = kanjiProgressList.get(i);
            totalPoints += p.getPoints();
            totalRealMiddleEntries += (p.getCompressedEntries());
        }

        middleCompressed.setPoints(totalPoints);

        middleCompressed.setCompressedEntries(totalRealMiddleEntries);

        List<KanjiProgress> compressedList = new ArrayList<>();
        compressedList.add(firstEntry);
        compressedList.add(middleCompressed);
        compressedList.add(lastEntry);

        kanji.setProgresses(compressedList);
    }

    private static void compressWordProgress(Word word) {
        List<WordProgress> wordProgressList = word.getProgresses();
        if (wordProgressList == null || wordProgressList.size() <= 3) {
            return; // No compression needed
        }

        List<WordProgress> compressedList = new ArrayList<>();

        // Add the first entry
        compressedList.add(wordProgressList.getFirst());

        // Compress middle entries
        WordProgress middleCompressed = wordProgressList.get(wordProgressList.size() - 2);
        middleCompressed.setCompressedEntries(wordProgressList.size() - 2);
        middleCompressed.setLearned(wordProgressList.get(wordProgressList.size() - 2).getLearned()); // Use the timestamp of the second last entry
        middleCompressed.setPoints(wordProgressList.subList(1, wordProgressList.size() - 2)
                .stream()
                .mapToInt(WordProgress::getPoints)
                .sum());
        compressedList.add(middleCompressed);

        // Add the last entry
        compressedList.add(wordProgressList.getLast());

        word.setProgresses(compressedList);
    }

    public Settings getSettings() {
        if (settings == null) {
            EntityManager entityManager = HibernateUtil.getEntityManager();

            settings = entityManager.find(Settings.class, SETTING_ID);

            if (settings == null) {
                settings = new Settings();
                settings.setId(SETTING_ID);
                settings.setNewWords(20);
                settings.setMaxDailyKanji(20);
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
        Platform.runLater(() -> {
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

    public void playAudio(String path) {
        try {
            Media media = new Media(new File(path).toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(200);
            mediaPlayer.play();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Audio not found!");
            alert.showAndWait();
        }
    }

    public void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public Kanji getNextLearningKanji() {
        return kanjiTrainer.getNextLearningKanji();
    }

    public List<Word> getNextLearningWords(int count) {
        return vocabTrainer.getNextLearningWord(count);
    }

    public Kanji addKanjiProgress(Kanji kanji, int percent) {
        kanji = kanjiTrainer.addKanjiProgress(kanji, percent);
        HibernateUtil.startTransaction();
        List<KanjiProgress> oldProgresses = kanji.getProgresses();
        compressKanjiProgress(kanji);
        for (KanjiProgress progress : oldProgresses) {
            if (!kanji.getProgresses().contains(progress)) {
                em.remove(progress);
            }
        }
        em.merge(kanji);
        HibernateUtil.commitTransaction();

        return kanji;
    }

    public Word addWordProgress(Word word, int percent) {
        word = vocabTrainer.addWordProgress(word, percent);
        HibernateUtil.startTransaction();
        List<WordProgress> oldProgresses = word.getProgresses();
        compressWordProgress(word);
        for (WordProgress progress : oldProgresses) {
            if (!word.getProgresses().contains(progress)) {
                em.remove(progress);
            }
        }
        em.merge(word);
        HibernateUtil.commitTransaction();

        return word;
    }

    public void updateLists() {
        updateKanjiList();
        updateWordList();
    }

    public void updateKanjiList() {
        kanjiTrainer.updateKanjiList();
    }

    public void updateWordList() {
        vocabTrainer.updateWordList();
    }

    public double getKanjiProgress() {
        int max = kanjiTrainer.getTodayDueMax();
        if (max == 0) {
            return 100;
        }
        return (double) (100 * (max - kanjiTrainer.getTodayDueCurrent())) / max / 100;
    }

    public double getWordsProgress() {
        int max = vocabTrainer.getTodayDueMax();
        if (max == 0) {
            return 100;
        }
        return (double) (100 * (max - vocabTrainer.getTodayDueCurrent())) / max / 100;
    }

    public int getDueKanjiCount() {
        return kanjiTrainer.getTodayDueCurrent();
    }

    public int getDueWordCount() {
        return vocabTrainer.getTodayDueCurrent();
    }

    public void increaseDueKanjiTmp(int count) {
        kanjiTrainer.tmpDueIncrease(count);
    }

    public void increaseDueWordsTmp(int count) {
        vocabTrainer.tmpDueIncrease(count);
    }

    public int getDueTotalKanjiCount() {
        return kanjiTrainer.getDueTotal();
    }

    public int getDueTotalWordsCount() {
        return vocabTrainer.getDueTotal();
    }

    public List<Kanji> getRandomKanji(int number) {
        return kanjiTrainer.getRandomKanji(number);
    }

    public List<Word> getRandomWordsFromKanjiTrainer(int number) {
        List<Kanji> randomKanji = kanjiTrainer.getRandomKanji(number);
        List<Word> randomWords = new ArrayList<>();
        Random rand = new Random();

        for (Kanji kanji : randomKanji) {
            randomWords.add(kanji.getWords().get(rand.nextInt(kanji.getWords().size())));
        }

        return randomWords;
    }
}
