package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnViews.WordKanjiSelectLearnView;
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

        WordKanjiSelectLearnView wordKanjiSelectLearnView;

        wordKanjiSelectLearnView = new WordKanjiSelectLearnView(this, resultList.getFirst());
        currentLearnView = wordKanjiSelectLearnView;

        setProgress(100);

        bp.setCenter(wordKanjiSelectLearnView.initView());
        wordKanjiSelectLearnView.playWordTTS();
    }

    @Override
    protected void nextLearningView() {
        super.learnSessionFinished();
    }
}
