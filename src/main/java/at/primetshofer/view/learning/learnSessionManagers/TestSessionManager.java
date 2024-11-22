package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.logic.tracing.ITraceLogic;
import at.primetshofer.logic.tracing.TraceLineLogic;
import at.primetshofer.logic.tracing.TraceLineOptions;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnViews.KanjiTracerLearnView;
import at.primetshofer.view.learning.learnViews.sentenceLearnViews.WordBuilderView;
import jakarta.persistence.EntityManager;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.util.List;

public class TestSessionManager extends LearnSessionManager{

    public TestSessionManager(Scene scene) {
        super(scene);
    }

    @Override
    protected void startLearning() {
        TraceLineOptions options = new TraceLineOptions(
                Color.GRAY,
                500.0D,
                500.0D,
                20.0D,
                5,
                100.0D
        );

        ITraceLogic traceLogic = new TraceLineLogic(options);
        super.currentLearnView = new KanjiTracerLearnView(this, traceLogic);

        setProgress(100);

        super.bp.setCenter(super.currentLearnView.initView());
    }

    @Override
    protected void nextLearningView() {
        super.learnSessionFinished();
    }
}
