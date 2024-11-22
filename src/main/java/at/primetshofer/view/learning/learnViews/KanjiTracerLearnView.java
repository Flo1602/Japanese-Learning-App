package at.primetshofer.view.learning.learnViews;

import at.primetshofer.logic.tracing.ITraceLogic;
import at.primetshofer.model.Polygon;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public class KanjiTracerLearnView extends LearnView implements ITraceLogic.ITraceLineListener {

    private final BorderPane rootView;
    private final Canvas userCanvas;
    private final Canvas verificationCanvas;
    private final ITraceLogic logic;

    public KanjiTracerLearnView(LearnSessionManager learnSessionManager, ITraceLogic logic) {
        super(learnSessionManager, false);
        this.rootView = new BorderPane();
        this.userCanvas = new Canvas(logic.getOptions().fieldWidth(), logic.getOptions().fieldHeight());
        this.verificationCanvas = new Canvas(logic.getOptions().fieldWidth(), logic.getOptions().fieldHeight());

        this.logic = logic;
        this.logic.addTraceLineListener(this);
    }

    @Override
    public Pane initView() {
        this.rootView.setCenter(this.userCanvas);
        return this.rootView;
    }

    @Override
    public void checkComplete() {
        // do nothing
    }

    @Override
    public void onBeginTracing(Polygon polygonToTracy, boolean showHint) {
        this.logic.getHintLineDrawer().drawPolygon(this.userCanvas.getGraphicsContext2D(), polygonToTracy);
    }
}
