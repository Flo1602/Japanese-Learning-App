package at.primetshofer.view.learning.learnViews.sentenceLearnViews;

import at.primetshofer.model.entities.Sentence;
import at.primetshofer.model.entities.SentenceWord;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import jakarta.persistence.EntityManager;

import java.util.*;

public class SentenceEnglishToJapanese extends SentenceLearnView{

    private Sentence sentence;

    public SentenceEnglishToJapanese(LearnSessionManager learnSessionManager, Sentence sentence) {
        super(learnSessionManager, sentence.getId());
        this.sentence = sentence;
        initSuper();
    }

    private void initSuper(){
        super.setToTranslate(sentence.getEnglish());
        super.setSolution(sentence.getJapanese());
        super.setSynonyms(sentence.getSynonyms());

        String japanese = sentence.getJapanese().replaceAll("[\\p{Punct}\\p{IsPunctuation}]", "");

        List<String> sentenceWordStrings = new ArrayList<>();
        for (SentenceWord sentenceWord : sentence.getSentenceWords()) {
            if(sentenceWord.getWordJapanese() != null && !sentenceWord.getWordJapanese().isBlank()){
                sentenceWordStrings.add(sentenceWord.getWordJapanese());
            }
            if(sentenceWord.getWordEnglish() != null && !sentenceWord.getWordEnglish().isBlank()){
                sentenceWordStrings.add(sentenceWord.getWordEnglish());
            }
        }
        ArrayList<String> words = ViewUtils.splitByDelimiters(japanese, sentenceWordStrings);

        Random rand = new Random();

        EntityManager entityManager = HibernateUtil.getEntityManager();
        String sql = "SELECT * FROM SENTENCEWORD ORDER BY RAND() LIMIT " + rand.nextInt(7);
        List<SentenceWord> resultList = entityManager.createNativeQuery(sql, SentenceWord.class).getResultList();

        for (SentenceWord sentenceWord : resultList) {
            words.add(ViewUtils.cleanText(sentenceWord.getWordJapanese()));
        }

        super.setWords(words);
        super.setTranslation(japanese);
        super.setSentenceWords(sentence.getSentenceWords());
    }
}
