package at.primetshofer.view.learning.learnViews;

import at.primetshofer.logic.drawing.IPolygonDrawer;
import at.primetshofer.logic.tracing.ITraceLogic;
import at.primetshofer.model.Controller;
import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;
import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.Word;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.StrokeLineCap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KanjiTracerLearnView extends LearnView implements ITraceLogic.ITraceLineListener {

    private final BorderPane rootView;
    private final Canvas userCanvas;
    private final Canvas hintArrowCanvas;
    private final Canvas hintCanvas;
    private final IPolygonDrawer hintLineDrawer;
    private final IPolygonDrawer correctingLineDrawer;
    private final ITraceLogic<?> logic;
    private final List<Point> drawnPoints;

    private ITraceLogic.ITraceFinishedCallback callback;
    private Point lastPoint;
    private Image hintArrowImage;

    private Kanji kanji;
    private Word wordWithKanji;
    private Label topText;
    private HBox top;

    public KanjiTracerLearnView(LearnSessionManager learnSessionManager, ITraceLogic<?> logic, IPolygonDrawer hintLineDrawer, IPolygonDrawer correctingLineDrawer, Kanji kanji) {
        super(learnSessionManager, false);
        this.logic = logic;
        this.rootView = new BorderPane();
        this.userCanvas = this.createSizedCanvas();
        this.hintArrowCanvas = this.createSizedCanvas();
        this.hintCanvas = this.createSizedCanvas();
        this.hintLineDrawer = hintLineDrawer;
        this.correctingLineDrawer = correctingLineDrawer;
        this.drawnPoints = new ArrayList<>();
        this.logic.addTraceLineListener(this);
        this.kanji = kanji;
        initGraphicsContext();
    }

    private void initGraphicsContext() {
        double width = this.logic.getOptions().lineWidth();
        GraphicsContext gc = this.userCanvas.getGraphicsContext2D();
        gc.setStroke(this.logic.getOptions().drawingColor());
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineWidth(width);
    }

    private Canvas createSizedCanvas() {
        return new Canvas(this.logic.getOptions().fieldWidth(), this.logic.getOptions().fieldHeight());
    }

    @Override
    public Pane initView() {
        StackPane canvasStack = new StackPane();
        canvasStack.getChildren().add(this.hintCanvas);
        canvasStack.getChildren().add(this.hintArrowCanvas);
        canvasStack.getChildren().add(this.userCanvas);
        this.hintArrowImage = new Image("hint_arrow.png");
        this.rootView.setCenter(canvasStack);

        top = new HBox();
        top.setAlignment(Pos.CENTER);
        top.setSpacing(10);
        top.setPadding(new Insets(10, 0, 0, 0));

        Image audioImage = new Image("audio.png");

        ImageView audioImageView = new ImageView(audioImage);
        audioImageView.setFitHeight(30);
        audioImageView.setFitWidth(30);

        Button audioButton = new Button();
        audioButton.setStyle("-fx-background-radius: 18; -fx-font-size: 14pt; -fx-background-color: transparent;");
        audioButton.setGraphic(audioImageView);
        audioButton.setOnAction(e -> playWordWithKanjiTTS());

        topText = new Label();
        topText.setStyle("-fx-font-size: 18pt");

        top.getChildren().addAll(audioButton, topText);

        rootView.setTop(top);

        return this.rootView;
    }

    @Override
    public void checkComplete() {
        throw new IllegalStateException("checkComplete should never be called");
    }

    @Override
    public void onShowHint(Polygon polygonToShowHintFor) {
        this.hintLineDrawer.drawPolygon(this.hintCanvas.getGraphicsContext2D(), polygonToShowHintFor);
    }

    @Override
    public void onDrawCorrectedLines(List<Polygon> correctedLine) {
        GraphicsContext gc = this.userCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, this.userCanvas.getWidth(), this.userCanvas.getHeight());

        for (Polygon polygon : correctedLine) {
            this.correctingLineDrawer.drawPolygon(gc, polygon);
        }
    }

    @Override
    public void onBeginTracing(ITraceLogic.ITraceFinishedCallback callback) {
        this.callback = callback;
        this.enableUserDrawing();
    }

    @Override
    public void onShowHintArrow(Point from, Point to) {
        GraphicsContext gc = this.hintArrowCanvas.getGraphicsContext2D();
        this.clearCanvas(this.hintArrowCanvas);

        if (from == null || to == null)
            return;

        double scaleFactor = 0.3D;
        double xOffset = this.hintArrowImage.getWidth() * scaleFactor;
        double yOffset = this.hintArrowImage.getHeight() * scaleFactor;
        double angle = Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());

        gc.save();
        gc.translate(from.getX(), from.getY());
        gc.rotate(Math.toDegrees(angle));
        gc.scale(scaleFactor, scaleFactor);
        gc.drawImage(this.hintArrowImage, -xOffset, -yOffset);
        gc.restore();
    }

    private void enableUserDrawing() {
        GraphicsContext gc = this.userCanvas.getGraphicsContext2D();
        this.userCanvas.setOnMousePressed(event -> this.drawNewPoint(gc, event.getX(), event.getY()));
        this.userCanvas.setOnMouseDragged(event -> this.drawNewPoint(gc, event.getX(), event.getY()));
        this.userCanvas.setOnMouseDragExited(event -> this.disableUserDrawingFinishPolygon());
        this.userCanvas.setOnMouseReleased(event -> this.disableUserDrawingFinishPolygon());
    }

    private void drawNewPoint(GraphicsContext gc, double x, double y) {
        Point currentPoint = new Point(x, y);

        if (this.lastPoint != null) {
            Polygon polygon = new Polygon();
            polygon.getVertices().add(this.lastPoint);
            polygon.getVertices().add(currentPoint);

            this.correctingLineDrawer.drawPolygon(gc, polygon);
        }

        this.lastPoint = currentPoint;
        this.drawnPoints.add(currentPoint);
    }

    private void disableUserDrawingFinishPolygon() {
        this.userCanvas.setOnMousePressed(null);
        this.userCanvas.setOnMouseDragged(null);
        this.userCanvas.setOnMouseDragExited(null);
        this.userCanvas.setOnMouseReleased(null);
        this.lastPoint = null;

        if (this.callback != null) {
            List<Point> drawnPoints = new ArrayList<>(this.drawnPoints);
            this.drawnPoints.clear();
            this.callback.onTraceFinished(new Polygon(drawnPoints));
        }
    }

    @Override
    public void onResetProgress() {
        updateWordWithKanji();
        this.clearCanvas(this.userCanvas);
        this.clearCanvas(this.hintArrowCanvas);
        this.clearCanvas(this.hintCanvas);
    }

    private void clearCanvas(Canvas canvas) {
        canvas.getGraphicsContext2D().clearRect(
                0,
                0,
                this.logic.getOptions().fieldWidth(),
                this.logic.getOptions().fieldHeight()
        );
    }

    @Override
    public void onFinished(boolean correct) {
        super.finished(correct);
    }

    private void updateWordWithKanji(){
        if(kanji.getWords().isEmpty()){
            top.setVisible(false);
            return;
        }

        Random random = new Random();
        wordWithKanji = kanji.getWords().get(random.nextInt(kanji.getWords().size()));

        String text = wordWithKanji.getJapanese() + " (" + wordWithKanji.getKana() + ")";
        text = text.replace(kanji.getSymbol(), "_");

        topText.setText(text);

        top.setVisible(true);

        playWordWithKanjiTTS();
    }

    public void playWordWithKanjiTTS(){
        Controller.getInstance().playAudio(wordWithKanji.getTtsPath());
    }

    public Pane getPane(){
        return this.rootView;
    }
}
