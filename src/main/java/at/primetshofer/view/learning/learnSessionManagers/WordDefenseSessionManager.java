package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnViews.WordDefense;
import jakarta.persistence.EntityManager;
import javafx.scene.Scene;

import java.util.List;

public class WordDefenseSessionManager extends LearnSessionManager {

    public WordDefenseSessionManager(Scene scene) {
        super(scene);
    }

    @Override
    protected void startLearning() {
        EntityManager entityManager = HibernateUtil.getEntityManager();
        String sql = "SELECT * FROM WORD ORDER BY RAND() LIMIT 20";
        List<Word> resultList = entityManager.createNativeQuery(sql, Word.class).getResultList();

        WordDefense wordDefense = new WordDefense(this, resultList);

        currentLearnView = wordDefense;

        setMaxViews(1);

        bp.setCenter(wordDefense.initView());
    }

    @Override
    protected void nextLearningView() {
        super.learnSessionFinished();
    }

    @Override
    protected void updateProgresses(int percent) {

    }

}
