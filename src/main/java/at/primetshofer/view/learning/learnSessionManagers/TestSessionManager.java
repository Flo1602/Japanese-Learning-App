package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnViews.WordDefense;
import jakarta.persistence.EntityManager;
import javafx.scene.Scene;

import java.util.List;

public class TestSessionManager extends LearnSessionManager{

    public TestSessionManager(Scene scene) {
        super(scene);
    }

    @Override
    protected void startLearning() {
        EntityManager entityManager = HibernateUtil.getEntityManager();
        String sql = "SELECT * FROM WORD ORDER BY RAND() LIMIT 5";
        List<Word> resultList = entityManager.createNativeQuery(sql, Word.class).getResultList();

        WordDefense wordDefense;

        wordDefense = new WordDefense(this, resultList);
        currentLearnView = wordDefense;

        setProgress(100);

        bp.setCenter(wordDefense.initView());
    }

    @Override
    protected void nextLearningView() {
        super.learnSessionFinished();
    }
}
