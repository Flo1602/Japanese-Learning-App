package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.model.entities.Sentence;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnViews.SpeakingLearnView;
import jakarta.persistence.EntityManager;
import javafx.scene.Scene;

import java.util.List;

public class SpeakingSessionManager extends LearnSessionManager{

    public SpeakingSessionManager(Scene scene) {
        super(scene);
    }

    @Override
    protected void startLearning() {
        EntityManager entityManager = HibernateUtil.getEntityManager();
        String sql = "SELECT * FROM SENTENCE ORDER BY RAND() LIMIT 1";
        List<Sentence> resultList = entityManager.createNativeQuery(sql, Sentence.class).getResultList();

        SpeakingLearnView speakingLearnView;

        speakingLearnView = new SpeakingLearnView(this, resultList.getFirst());
        currentLearnView = speakingLearnView;

        setProgress(100);

        bp.setCenter(speakingLearnView.initView());
        speakingLearnView.playSentenceTTS();
    }

    @Override
    protected void nextLearningView() {
        super.learnSessionFinished();
    }

    @Override
    protected void updateProgresses(int percent) {

    }
}
