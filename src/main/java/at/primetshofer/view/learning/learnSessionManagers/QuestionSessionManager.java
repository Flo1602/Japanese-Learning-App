package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.model.entities.Question;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnViews.QuestionLearnView;
import jakarta.persistence.EntityManager;
import javafx.scene.Scene;

import java.util.List;

public class QuestionSessionManager extends LearnSessionManager {

    public QuestionSessionManager(Scene scene) {
        super(scene);
    }

    @Override
    protected void startLearning() {
        EntityManager entityManager = HibernateUtil.getEntityManager();
        String sql = "SELECT * FROM QUESTION ORDER BY RAND() LIMIT 1";
        List<Question> resultList = entityManager.createNativeQuery(sql, Question.class).getResultList();

        QuestionLearnView learnView;

        learnView = new QuestionLearnView(this, resultList.getFirst(), true, true);
        currentLearnView = learnView;

        bp.setCenter(learnView.initView());
        learnView.playQuestionTTS();
    }

    @Override
    protected void nextLearningView() {
        super.learnSessionFinished();
    }

    @Override
    protected void updateProgresses(int percent) {

    }
}
