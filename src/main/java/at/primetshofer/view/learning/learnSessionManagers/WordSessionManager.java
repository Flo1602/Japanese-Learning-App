package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnViews.LearnView;
import at.primetshofer.view.learning.learnViews.matchLearnViews.JapaneseToKanaMatch;
import at.primetshofer.view.learning.learnViews.matchLearnViews.VocabAudioToEnglishMatch;
import at.primetshofer.view.learning.learnViews.matchLearnViews.VocabAudioToJapaneseMatch;
import at.primetshofer.view.learning.learnViews.matchLearnViews.VocabEnglishToJapaneseMatch;
import jakarta.persistence.EntityManager;
import javafx.scene.Scene;

import java.util.List;
import java.util.Random;

public class WordSessionManager extends LearnSessionManager {

    int count = 0;

    public WordSessionManager(Scene scene) {
        super(scene);
        setMaxViews(5);
    }

    @Override
    protected void startLearning() {
        nextLearningView();
    }

    @Override
    protected void nextLearningView() {
        count++;

        if(count <= 5){
            EntityManager entityManager = HibernateUtil.getEntityManager();
            String sql = "SELECT * FROM WORD ORDER BY RAND() LIMIT 5";
            List<Word> resultList = entityManager.createNativeQuery(sql, Word.class).getResultList();

            LearnView learnView = null;
            Random rand = new Random();

            switch (rand.nextInt(5)){
                case 0 -> learnView = new VocabEnglishToJapaneseMatch(this, resultList);
                case 1 -> learnView = new VocabAudioToJapaneseMatch(this, resultList);
                case 2 -> learnView = new JapaneseToKanaMatch(this, resultList);
                case 3 -> {
                    learnView = new VocabEnglishToJapaneseMatch(this, resultList);
                    ((VocabEnglishToJapaneseMatch)learnView).setReverse(true);
                }
                case 4 -> learnView = new VocabAudioToEnglishMatch(this, resultList);
            }

            currentLearnView = learnView;

            bp.setCenter(learnView.initView());
        } else {
            super.learnSessionFinished();
        }

    }

    @Override
    protected void updateProgresses(int percent) {

    }
}
