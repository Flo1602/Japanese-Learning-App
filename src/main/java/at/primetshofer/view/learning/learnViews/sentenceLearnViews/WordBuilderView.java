package at.primetshofer.view.learning.learnViews.sentenceLearnViews;

import at.primetshofer.model.Controller;
import at.primetshofer.model.entities.Word;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WordBuilderView extends SentenceLearnView {

    private Word word;

    public WordBuilderView(LearnSessionManager learnSessionManager, Word word) {
        super(learnSessionManager, -1);
        this.word = word;
        super.setDisableOverwrite(true);
        initSuper();
    }

    private void initSuper(){
        super.setSolution(word.getJapanese() + " (" + word.getEnglish() + ")");
        super.setTtsPath(word.getTtsPath());
        super.setTranslation(word.getJapanese());

        Random rand = new Random();

        ArrayList<String> symbols = new ArrayList<>();

        List<Word> resultList = Controller.getInstance().getRandomWordsFromKanjiTrainer(rand.nextInt(1, 5));

        resultList.add(word);

        for (Word word : resultList) {
            char[] chars = word.getJapanese().toCharArray();
            for (char aChar : chars) {
                symbols.add(aChar + "");
            }
        }

        super.setWords(symbols);

        super.playSentenceTTS();
    }
}
