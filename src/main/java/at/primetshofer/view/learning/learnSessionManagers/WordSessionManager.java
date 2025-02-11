package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.model.Controller;
import at.primetshofer.model.entities.Word;
import at.primetshofer.view.learning.learnViews.matchLearnViews.MatchLearnView;
import at.primetshofer.view.learning.learnViews.matchLearnViews.VocabAudioToEnglishMatch;
import at.primetshofer.view.learning.learnViews.matchLearnViews.VocabEnglishToJapaneseMatch;
import javafx.scene.Scene;

import java.util.*;

public class WordSessionManager extends LearnSessionManager {

    private static final int WORDS_PER_SESSION = 15;

    private int currentCounter = 0;
    private List<Word> words;
    private List<Word> neverWords;
    private Map<Word, List<Boolean>> results;
    private final Random rand;
    private boolean listening;

    public WordSessionManager(Scene scene) {
        super(scene);
        rand = new Random();
    }

    @Override
    public void initSessionManager() {
        super.initSessionManager();
        setMaxViews(6);
        currentCounter = 0;
        listening = false;
        results = new HashMap<>();
        neverWords = new ArrayList<>();

        this.words = Controller.getInstance().getNextLearningWords(WORDS_PER_SESSION);

        System.out.println("Words: " + words.size());

        Collections.shuffle(words);

        for (Word word : words) {
            results.put(word, new ArrayList<>());
            neverWords.add(word);
        }
    }

    @Override
    protected void startLearning() {
        nextLearningView();
    }

    @Override
    protected void nextLearningView() {
        currentCounter++;

        if (currentCounter <= 6) {
            MatchLearnView learnView = null;

            switch (rand.nextInt((listening) ? 2 : 3)) {
                case 0 -> learnView = new VocabEnglishToJapaneseMatch(this, getFiveWords());
                case 1 -> {
                    learnView = new VocabEnglishToJapaneseMatch(this, getFiveWords());
                    ((VocabEnglishToJapaneseMatch) learnView).setReverse(true);
                }
                case 2 -> {
                    learnView = new VocabAudioToEnglishMatch(this, getFiveWords());
                    listening = true;
                }
            }

            learnView.setGetDetailedResults(true);

            currentLearnView = learnView;

            bp.setCenter(learnView.initView());
        } else {
            super.learnSessionFinished();
        }

    }

    private List<Word> getFiveWords() {
        List<Word> result = new ArrayList<>();
        int cntr = 5;

        while (cntr > 0 && !neverWords.isEmpty()) {
            result.add(neverWords.removeFirst());
            cntr--;
        }

        while (cntr > 0) {
            if (words.size() > 5) {
                Word word = words.get(rand.nextInt(words.size()));
                for (int i = 0; i < 50 && result.contains(word); i++) {
                    word = words.get(rand.nextInt(words.size()));
                }
                result.add(word);
                cntr--;
            } else {
                result.add(words.get(rand.nextInt(words.size())));
                cntr--;
            }
        }

        return result;
    }

    @Override
    protected void updateProgresses(int percent) {
        Controller controller = Controller.getInstance();
        results.forEach((key, value) -> {
            System.out.print(key.getEnglish() + ": ");
            double wordResult = 0;
            for (Boolean b : value) {
                if (b) {
                    wordResult++;
                }
            }

            if (!value.isEmpty()) {
                wordResult = wordResult / value.size();
                wordResult *= 100;
            }

            controller.addWordProgress(key, (int) wordResult);
            System.out.println(wordResult + "%");
        });
    }

    public void learnViewFinished(boolean success, Map<String, Boolean> results) {
        results.forEach((key, value) -> {
            int id = -1;
            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException e) {
            }
            for (Word word : words) {
                if (word.getJapanese().equals(key) || word.getId() == id) {
                    this.results.get(word).add(value);
                }
            }
        });

        learnViewFinished(success, (String) null);
    }
}
