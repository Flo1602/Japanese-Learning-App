package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnViews.sentenceLearnViews.WordBuilderView;
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
        String sql = "SELECT * FROM WORD ORDER BY RAND() LIMIT 1";
        List<Word> resultList = entityManager.createNativeQuery(sql, Word.class).getResultList();

        WordBuilderView wordBuilderView;

        wordBuilderView = new WordBuilderView(this, resultList.getFirst());
        currentLearnView = wordBuilderView;

        setProgress(100);

        bp.setCenter(wordBuilderView.initView());
        wordBuilderView.playSentenceTTS();
    }

    @Override
    protected void nextLearningView() {
        super.learnSessionFinished();
    }
}
