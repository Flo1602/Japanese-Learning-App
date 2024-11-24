package at.primetshofer.logic.tracing.verification;

import at.primetshofer.logic.drawing.DottedColorProvidedPolygonDrawer;
import at.primetshofer.logic.drawing.IPolygonDrawer;
import at.primetshofer.logic.drawing.LayeredGradientPolygonDrawer;
import at.primetshofer.logic.provider.polygon.IPolygonConverter;
import at.primetshofer.model.ColoredPolygon;
import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;
import at.primetshofer.model.util.PolygonUtil;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GradientVerificationLogic implements ITraceVerificationLogic {

    private static final Color BASE_COLOR = Color.WHITE;
    private static final Double MAX_GRADIENT_HUE = 270.0D;

    private final IPolygonDrawer sourceDrawer;
    private final IPolygonDrawer toVerifyDrawer;
    private final IPolygonConverter toVerifyPolygonConverter;
    private final Canvas sourceCanvas;
    private final Canvas toVerifyCanvas;
    private final VerificationOptions options;


    private ColoredPolygon coloredSourcePolygon;
    private double currentMaxHue;
    private int currentTry = 0;

    public GradientVerificationLogic(
            VerificationOptions options,
            IPolygonConverter toVerifyPolygonConverter
    ) {
        this.sourceDrawer = new LayeredGradientPolygonDrawer(
                options.minGradientLineWidth(),
                options.gradientLines(),
                options.maxGradientLineWidth(),
                MAX_GRADIENT_HUE
        );

        this.toVerifyDrawer = new DottedColorProvidedPolygonDrawer(
                options.toVerifyDotSize(),
                this::provideVerificationColor
        );

        this.toVerifyPolygonConverter = toVerifyPolygonConverter;
        this.sourceCanvas = new Canvas(options.fieldWidth(), options.fieldHeight());
        this.toVerifyCanvas = new Canvas(options.fieldWidth(), options.fieldHeight());
        this.options = options;
    }

    private Color provideVerificationColor(int currentVertex, Polygon polygon) {
        if (this.coloredSourcePolygon == null)
            return BASE_COLOR;

        Point vertexToCompare = polygon.getVertices().get(currentVertex);
        int index = PolygonUtil.getVertexIndexOfClosestVertex(vertexToCompare, this.coloredSourcePolygon);
        Color closestVerificationColor = this.coloredSourcePolygon.getColors().get(index);
        double verificationHue = closestVerificationColor.getHue();

        if (verificationHue > this.currentMaxHue)
            this.currentMaxHue = verificationHue;
        else
            verificationHue = this.currentMaxHue;

        return Color.hsb(verificationHue, 1.0D, 1.0D);
    }

    @Override
    public VerifyResult verify(Polygon source, Polygon toVerify) {
        List<Polygon> polygonsToConvert = new ArrayList<>();
        polygonsToConvert.add(toVerify);
        polygonsToConvert.add(source);
        this.toVerifyPolygonConverter.convert(polygonsToConvert);

        this.currentMaxHue = Double.MIN_VALUE;
        this.clearCanvas(this.sourceCanvas);
        this.clearCanvas(this.toVerifyCanvas);

        this.coloredSourcePolygon = this.sourceDrawer.drawPolygon(this.sourceCanvas.getGraphicsContext2D(), source);
        this.toVerifyDrawer.drawPolygon(this.toVerifyCanvas.getGraphicsContext2D(), toVerify);
        return this.calculateVerificationResult(source, toVerify);
    }

    @Override
    public void resetTries() {
        this.currentTry = 0;
    }

    private VerifyResult calculateVerificationResult(Polygon source, Polygon toVerify) {
        double imageSimilarity = this.getImageSimilarity();
        double polygonSimilarity = this.getPolygonSimilarity(source, toVerify);
        double minSimilarity = Math.min(imageSimilarity, polygonSimilarity);
        return this.getVerifyResult(Math.max(0.0D, Math.min(1.0D, minSimilarity)));
    }

    private VerifyResult getVerifyResult(double result) {
        this.currentTry++;

        VerifyResult incorrectResult = VerifyResult.INCORRECT;
        if (this.currentTry >= this.options.maxTries())
            incorrectResult = VerifyResult.NO_MORE_TRIES;

        return result < 0.5D ? incorrectResult : VerifyResult.CORRECT;
    }

    private double getPolygonSimilarity(Polygon source, Polygon toVerify) {
        double verticesCountDiff = Math.abs(source.getVerticesCount() - toVerify.getVerticesCount());
        return 1.0D - Math.pow(verticesCountDiff / source.getVerticesCount(), 1.0 / this.options.polygonCorrectnessExp());
    }

    private double getImageSimilarity() {
        PixelReader sourceReader = this.getPixelReader(this.sourceCanvas);
        PixelReader toVerifyReader = this.getPixelReader(this.toVerifyCanvas);

        int comparedPixels = 0;
        double totalColorDiff = 0.0D;

        for (int x = 0; x < this.toVerifyCanvas.getWidth(); x++) {
            for (int y = 0; y < this.toVerifyCanvas.getHeight(); y++) {
                Color toVerfiyColor = toVerifyReader.getColor(x, y);
                if (toVerfiyColor.equals(BASE_COLOR))
                    continue;

                comparedPixels++;
                double pixelDiff = calculateAvgColorDiff(toVerfiyColor, sourceReader.getColor(x, y));
                totalColorDiff += pixelDiff;
            }
        }

        if (comparedPixels == 0)
            return 1.0D;

        return 1.0D - totalColorDiff / comparedPixels;
    }

    private double calculateAvgColorDiff(Color a, Color b) {
        double redDiff = this.calculateColorChannelDiff(a.getRed(), b.getRed());
        double greenDiff = this.calculateColorChannelDiff(a.getGreen(), b.getGreen());
        double blueDiff = this.calculateColorChannelDiff(a.getBlue(), b.getBlue());

        return (redDiff + greenDiff + blueDiff);
    }

    private double calculateColorChannelDiff(double channelA, double channelB) {
        return Math.pow(Math.abs(channelA - channelB), 1.0 / this.options.colorCorrectnessExp());
    }

    private PixelReader getPixelReader(Canvas toVerifyCanvas) {
        return toVerifyCanvas.snapshot(null, null).getPixelReader();
    }

    private void clearCanvas(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(BASE_COLOR);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
}
