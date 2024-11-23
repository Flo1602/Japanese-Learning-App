package at.primetshofer.view.learning.learnViews;

import at.primetshofer.logic.drawing.ILineDrawer;
import at.primetshofer.logic.drawing.SmoothLineDrawer;
import at.primetshofer.logic.tracing.ITraceLogic;
import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;
import at.primetshofer.model.util.PolygonUtil;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

public class KanjiTracerLearnView extends LearnView implements ITraceLogic.ITraceLineListener {

    private final BorderPane rootView;
    private final Canvas userCanvas;
    private final Canvas hintCanvas;
    private final ILineDrawer lineDrawer;
    private final ITraceLogic<?> logic;
    private Point lastPoint;

    public KanjiTracerLearnView(LearnSessionManager learnSessionManager, ITraceLogic<?> logic, ILineDrawer lineDrawer) {
        super(learnSessionManager, false);
        this.logic = logic;
        this.rootView = new BorderPane();
        this.userCanvas = createSizedCanvas();
        this.hintCanvas = new Canvas(logic.getOptions().fieldWidth(), logic.getOptions().fieldHeight());
        this.lineDrawer = lineDrawer;

        double width = logic.getOptions().lineWidth();
        GraphicsContext gc = this.userCanvas.getGraphicsContext2D();
        gc.setStroke(Color.WHITE);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineWidth(width);

        this.userCanvas.setOnMousePressed(event ->
                this.lastPoint = new Point(event.getX(), event.getY()));

        this.userCanvas.setOnMouseDragged(event -> {
            Point currentPoint = new Point(event.getX(), event.getY());
            this.drawLineSmoothly(gc, this.lastPoint, currentPoint);
            this.lastPoint = currentPoint;
        });

        this.logic.addTraceLineListener(this);
    }

    private Canvas createSizedCanvas() {
        return new Canvas(this.logic.getOptions().fieldWidth(), this.logic.getOptions().fieldHeight());
    }

    private void drawLineSmoothly(GraphicsContext gc, Point a, Point b) {
        double distance = PolygonUtil.calculateDistanceBetweenPoints(a, b);

        if (distance > 1) {
            int numSteps = (int) distance;
            double deltaX = (b.getX() - a.getX()) / numSteps;
            double deltaY = (b.getY() - a.getY()) / numSteps;

            for (int i = 0; i < numSteps; i++) {
                double startX = a.getX() + i * deltaX;
                double startY = a.getY() + i * deltaY;
                double endX = startX + deltaX;
                double endY = startY + deltaY;
                gc.strokeLine(startX, startY, endX, endY);
            }
        } else {
            gc.strokeLine(a.getX(), a.getY(), b.getX(), b.getY());
        }
    }

    @Override
    public Pane initView() {
        StackPane canvasStack = new StackPane();
        canvasStack.getChildren().add(this.hintCanvas);
        canvasStack.getChildren().add(this.userCanvas);
        this.rootView.setCenter(canvasStack);
        return this.rootView;
    }

    @Override
    public void checkComplete() {
        // do nothing
    }

    @Override
    public void onBeginTracing(Polygon polygonToTracy, boolean showHint) {
        this.lineDrawer.drawPolygon(this.hintCanvas.getGraphicsContext2D(), polygonToTracy);
    }

    @Override
    public void onResetProgress() {
        this.userCanvas.getGraphicsContext2D().clearRect(
                0,
                0,
                this.logic.getOptions().fieldWidth(),
                this.logic.getOptions().fieldHeight()
        );
    }
}
