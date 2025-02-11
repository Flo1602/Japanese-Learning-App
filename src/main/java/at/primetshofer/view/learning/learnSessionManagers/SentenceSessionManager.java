package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.model.entities.Sentence;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnViews.sentenceLearnViews.SentenceAudioToJapanese;
import at.primetshofer.view.learning.learnViews.sentenceLearnViews.SentenceEnglishToJapanese;
import at.primetshofer.view.learning.learnViews.sentenceLearnViews.SentenceJapaneseToEnglish;
import at.primetshofer.view.learning.learnViews.sentenceLearnViews.SentenceLearnView;
import jakarta.persistence.EntityManager;
import javafx.scene.Scene;

import java.util.List;
import java.util.Random;

public class SentenceSessionManager extends LearnSessionManager {

    public SentenceSessionManager(Scene scene) {
        super(scene);
    }

    @Override
    protected void startLearning() {
        EntityManager entityManager = HibernateUtil.getEntityManager();
        String sql = "SELECT * FROM SENTENCE ORDER BY RAND() LIMIT 1";
        List<Sentence> resultList = entityManager.createNativeQuery(sql, Sentence.class).getResultList();

        SentenceLearnView sentenceLearnView = null;

        Random rand = new Random();

        switch (rand.nextInt(3)) {
            case 0 -> sentenceLearnView = new SentenceEnglishToJapanese(this, resultList.getFirst());
            case 1 -> sentenceLearnView = new SentenceJapaneseToEnglish(this, resultList.getFirst());
            case 2 -> sentenceLearnView = new SentenceAudioToJapanese(this, resultList.getFirst());
        }

        currentLearnView = sentenceLearnView;

        bp.setCenter(sentenceLearnView.initView());
        sentenceLearnView.playSentenceTTS();
    }

    @Override
    protected void nextLearningView() {
        super.learnSessionFinished();
    }

    @Override
    protected void updateProgresses(int percent) {

    }
}
