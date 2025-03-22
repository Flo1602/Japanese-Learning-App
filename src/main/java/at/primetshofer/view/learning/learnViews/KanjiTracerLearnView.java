package at.primetshofer.view.learning.learnViews;

import at.primetshofer.logic.drawing.IPolygonDrawer;
import at.primetshofer.logic.tracing.ITraceLogic;
import at.primetshofer.logic.tracing.TraceLineOptions;
import at.primetshofer.logic.tracing.verification.VerificationLogic;
import at.primetshofer.model.Controller;
import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;
import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.Word;
import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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
    private final TraceLineOptions options;
    private final ITraceLogic<?> logic;
    private final List<Point> drawnPoints;
    private final List<Polygon> correctedPolygons = new ArrayList<>();
    private final List<Polygon> correctlyDrawnPolygons = new ArrayList<>();
    private final boolean debug;

    private ITraceLogic.ITraceFinishedCallback callback;
    private Point lastPoint;
    private Image hintArrowImage;

    private Word wordWithKanji;
    private Kanji kanji;
    private Label topText;
    private HBox top;

    public KanjiTracerLearnView(LearnSessionManager learnSessionManager, TraceLineOptions options, ITraceLogic<?> logic, IPolygonDrawer hintLineDrawer, IPolygonDrawer correctingLineDrawer, boolean debug) {
        super(learnSessionManager, false);
        this.options = options;
        this.logic = logic;
        this.rootView = new BorderPane();
        this.userCanvas = this.createSizedCanvas();
        this.hintArrowCanvas = this.createSizedCanvas();
        this.hintCanvas = this.createSizedCanvas();
        this.hintLineDrawer = hintLineDrawer;
        this.correctingLineDrawer = correctingLineDrawer;
        this.drawnPoints = new ArrayList<>();
        this.logic.addTraceLineListener(this);
        this.debug = debug;
        initGraphicsContext();
    }

    private void initGraphicsContext() {
        double width = this.options.lineWidth();
        GraphicsContext gc = this.userCanvas.getGraphicsContext2D();
        gc.setStroke(this.options.drawingColor());
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineWidth(width);
    }

    private Canvas createSizedCanvas() {
        return new Canvas(this.options.fieldWidth(), this.options.fieldHeight());
    }

    @Override
    public Pane initView() {
        StackPane canvasStack = new StackPane();
        canvasStack.getChildren().add(this.hintCanvas);
        canvasStack.getChildren().add(this.hintArrowCanvas);
        canvasStack.getChildren().add(this.userCanvas);
        this.hintArrowImage = new Image("hint_arrow.png");
        this.rootView.setCenter(canvasStack);

        this.top = new HBox();
        this.top.setAlignment(Pos.CENTER);
        this.top.setSpacing(10);
        this.top.setPadding(new Insets(10, 0, 0, 0));

        Image audioImage = new Image("audio.png");

        ImageView audioImageView = new ImageView(audioImage);
        audioImageView.setFitHeight(30);
        audioImageView.setFitWidth(30);

        Button audioButton = new Button();
        audioButton.setStyle("-fx-background-radius: 18; -fx-font-size: 14pt; -fx-background-color: transparent;");
        audioButton.setGraphic(audioImageView);
        audioButton.setOnAction(e -> playWordWithKanjiTTS());

        this.topText = new Label();
        this.topText.setStyle("-fx-font-size: 18pt");

        this.top.getChildren().addAll(audioButton, this.topText);
        this.rootView.setTop(this.top);

        return this.rootView;
    }

    @Override
    public void checkComplete() {
        throw new IllegalStateException("checkComplete should never be called");
    }

    @Override
    public Pane resetView() {
        throw new UnsupportedOperationException("ResetView should not be called on KanjiTracerLearnView!");
    }

    @Override
    public void onShowHint(Polygon polygonToShowHintFor) {
        this.hintLineDrawer.drawPolygon(this.hintCanvas.getGraphicsContext2D(), polygonToShowHintFor);
    }

    @Override
    public void onDrawCorrectedLines(List<Polygon> correctedLine) {
        this.correctedPolygons.clear();
        this.correctedPolygons.addAll(correctedLine);
        this.drawUserCanvasPolygons(this.correctedPolygons);
    }

    private void drawUserCanvasPolygons(List<Polygon> polygonsToDraw) {
        this.clearCanvas(this.userCanvas);
        for (Polygon polygon : polygonsToDraw) {
            this.correctingLineDrawer.drawPolygon(this.userCanvas.getGraphicsContext2D(), polygon);
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
        this.userCanvas.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY)
                this.drawNewPoint(gc, event.getX(), event.getY());

            if (event.getButton() == MouseButton.MIDDLE)
                this.showDebugInfos();
        });

        this.userCanvas.setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.PRIMARY)
                this.drawNewPoint(gc, event.getX(), event.getY());
        });

        this.userCanvas.setOnMouseDragExited(event -> {
            if (event.getButton() == MouseButton.PRIMARY)
                this.disableUserDrawingFinishPolygon();
        });

        this.userCanvas.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY)
                this.disableUserDrawingFinishPolygon();

            if (event.getButton() == MouseButton.MIDDLE)
                this.drawUserCanvasPolygons(this.correctedPolygons);
        });
    }

    private void showDebugInfos() {
        VerificationLogic verificationLogic =
                this.logic.getVerificationLogic() instanceof VerificationLogic ?
                        ((VerificationLogic) this.logic.getVerificationLogic()) :
                        null;

        if (verificationLogic != null && this.debug) {
            Canvas canvas = verificationLogic.getDebugCanvas();
            if (canvas != null)
                this.overwriteCanvas(canvas, this.userCanvas);
        }
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
            Polygon drawnPolygon = new Polygon(new ArrayList<>(this.drawnPoints));
            this.drawnPoints.clear();
            if (this.callback.onTraceFinished(drawnPolygon))
                this.correctlyDrawnPolygons.add(drawnPolygon);
        }
    }

    @Override
    public void onResetProgress() {
        updateWordWithKanji();
        this.clearCanvas(this.userCanvas);
        this.clearCanvas(this.hintArrowCanvas);
        this.clearCanvas(this.hintCanvas);
        this.correctlyDrawnPolygons.clear();
        this.correctedPolygons.clear();
        this.setIsFinished(false);
    }

    private void clearCanvas(Canvas canvas) {
        canvas.getGraphicsContext2D().clearRect(
                0,
                0,
                this.options.fieldWidth(),
                this.options.fieldHeight()
        );
    }

    @Override
    public void onFinished(boolean correct) {
        super.finished(correct);
        this.setIsFinished(true);
    }

    private void setIsFinished(boolean isFinished) {
        if (isFinished) {
            this.userCanvas.setOnMousePressed(event -> {
                if (event.getButton() == MouseButton.SECONDARY)
                    this.drawUserCanvasPolygons(this.correctlyDrawnPolygons);

                if (event.getButton() == MouseButton.MIDDLE)
                    this.showDebugInfos();
            });

            this.userCanvas.setOnMouseReleased(event -> {
                if (event.getButton() == MouseButton.SECONDARY || event.getButton() == MouseButton.MIDDLE)
                    this.drawUserCanvasPolygons(this.correctedPolygons);
            });
        } else {
            this.userCanvas.setOnMousePressed(null);
            this.userCanvas.setOnMouseReleased(null);
        }
    }

    private void overwriteCanvas(Canvas source, Canvas target) {
        this.clearCanvas(target);

        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);

        target.getGraphicsContext2D().drawImage(
                source.snapshot(snapshotParameters, null),
                0.0D,
                0.0D
        );
    }

    private void updateWordWithKanji() {
        if (this.kanji.getWords().isEmpty()) {
            this.top.setVisible(false);
            return;
        }

        Random random = new Random();
        this.wordWithKanji = this.kanji.getWords().get(random.nextInt(kanji.getWords().size()));

        String text = this.wordWithKanji.getJapanese() + " (" + this.wordWithKanji.getKana() + ")";
        text = text.replace(this.kanji.getSymbol(), "_");

        this.topText.setText(text);
        this.top.setVisible(true);

        Tooltip tooltip = new Tooltip(this.wordWithKanji.getEnglish());
        tooltip.setStyle("-fx-font-size: 16pt");
        Tooltip.install(this.topText, tooltip);

        playWordWithKanjiTTS();
    }

    public void playWordWithKanjiTTS() {
        Controller.getInstance().playAudio(this.wordWithKanji.getTtsPath());
    }

    public Pane getPane() {
        return this.rootView;
    }

    public Kanji getKanji() {
        return kanji;
    }

    public void setKanji(Kanji kanji) {
        this.kanji = kanji;
    }
}
