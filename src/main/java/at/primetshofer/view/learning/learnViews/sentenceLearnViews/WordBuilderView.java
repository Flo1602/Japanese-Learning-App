package at.primetshofer.view.learning.learnViews.sentenceLearnViews;

import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import jakarta.persistence.EntityManager;

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

        EntityManager entityManager = HibernateUtil.getEntityManager();
        String sql = "SELECT * FROM WORD ORDER BY RAND() LIMIT " + rand.nextInt(5);
        List<Word> resultList = entityManager.createNativeQuery(sql, Word.class).getResultList();

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
