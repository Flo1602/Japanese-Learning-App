package at.primetshofer.view.learning.learnViews.sentenceLearnViews;

import at.primetshofer.model.entities.Sentence;
import at.primetshofer.model.entities.SentenceWord;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SentenceJapaneseToEnglish extends SentenceLearnView {

    private final Sentence sentence;

    public SentenceJapaneseToEnglish(LearnSessionManager learnSessionManager, Sentence sentence) {
        super(learnSessionManager, sentence.getId());
        this.sentence = sentence;
        initSuper();
    }

    private void initSuper() {
        super.setToTranslate(sentence.getJapanese());
        super.setTtsPath(sentence.getTtsPath());
        super.setSolution(sentence.getEnglish());
        super.setSynonyms(sentence.getSynonyms());

        String english = sentence.getEnglish().replaceAll("[\\p{Punct}\\p{IsPunctuation}]", "");

        ArrayList<String> words = new ArrayList<>();
        Collections.addAll(words, english.split(" "));

        Random rand = new Random();

        EntityManager entityManager = HibernateUtil.getEntityManager();
        String sql = "SELECT * FROM SENTENCEWORD ORDER BY RAND() LIMIT " + rand.nextInt(7);
        List<SentenceWord> resultList = entityManager.createNativeQuery(sql, SentenceWord.class).getResultList();

        for (SentenceWord sentenceWord : resultList) {
            for (String word : sentenceWord.getWordEnglish().split(" ")) {
                words.add(ViewUtils.cleanText(word));
            }

        }

        super.setWords(words);
        super.setTranslation(english.replaceAll(" ", ""));
        super.setSentenceWords(sentence.getSentenceWords());
    }
}
