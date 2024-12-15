package at.primetshofer.logic.tracing.verification;

import at.primetshofer.logic.drawing.DottedColorProvidedPolygonDrawer;
import at.primetshofer.logic.drawing.IPolygonDrawer;
import at.primetshofer.logic.drawing.LayeredGradientPolygonDrawer;
import at.primetshofer.logic.provider.polygon.IPolygonConverter;
import at.primetshofer.model.ColoredPolygon;
import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;
import at.primetshofer.model.util.PolygonUtil;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class VerificationLogic implements ITraceVerificationLogic {

    private static final Color BASE_COLOR = Color.WHITE;
    private static final Double MAX_GRADIENT_HUE = 270.0D;

    private final IPolygonDrawer sourceDrawer;
    private final IPolygonDrawer toVerifyDrawer;
    private final IPolygonConverter toVerifyPolygonConverter;
    private final Canvas sourceCanvas;
    private final Canvas toVerifyCanvas;
    private final Canvas debugCanvas;
    private final VerificationOptions options;


    private ColoredPolygon coloredSourcePolygon;
    private double currentMaxHue;
    private int currentTry = 0;

    public VerificationLogic(
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
        this.debugCanvas = new Canvas(options.fieldWidth(), options.fieldHeight());

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
        this.clearCanvas(this.sourceCanvas, true);
        this.clearCanvas(this.toVerifyCanvas, true);
        this.clearCanvas(this.debugCanvas, false);

        this.coloredSourcePolygon = this.sourceDrawer.drawPolygon(this.sourceCanvas.getGraphicsContext2D(), source);
        this.toVerifyDrawer.drawPolygon(this.toVerifyCanvas.getGraphicsContext2D(), toVerify);

        if (this.options.debug()) {
            this.copyContent(this.sourceCanvas, this.debugCanvas);
            this.copyContent(this.toVerifyCanvas, this.debugCanvas);
        }

        return this.calculateVerificationResult(source, toVerify);
    }

    @Override
    public void resetTries() {
        this.currentTry = 0;
    }

    public Canvas getDebugCanvas() {
        return this.debugCanvas;
    }

    private VerifyResult calculateVerificationResult(Polygon source, Polygon toVerify) {
        double minWrongSimilarity = 1.0D;

        Invalidator invalidator = Invalidator.NO;

        double imageSimilarity = this.getImageSimilarity();
        double lengthSimilarity = this.getLengthSimilarity(source, toVerify);
        double angularSimilarity = this.getAngularSimilarity(source, toVerify);

        if (imageSimilarity < minWrongSimilarity && imageSimilarity < this.options.minLengthSimilarity()) {
            minWrongSimilarity = imageSimilarity;
            invalidator = Invalidator.IMAGE;
        }

        if (lengthSimilarity < minWrongSimilarity && lengthSimilarity < this.options.minLengthSimilarity()) {
            minWrongSimilarity = lengthSimilarity;
            invalidator = Invalidator.LENGTH;
        }

        if (angularSimilarity < minWrongSimilarity && angularSimilarity < this.options.minAngularSimilarity()) {
            minWrongSimilarity = angularSimilarity;
            invalidator = Invalidator.ANGLE;
        }

        double clampedWrongSimilarity = Math.max(0.0D, Math.min(1.0D, minWrongSimilarity));

        if (this.options.debug()) {
            System.out.println("\nSUMMARY:");
            System.out.println("\tMin. result from:\t" + invalidator + " similarity");
            System.out.println("\tMin. result:\t\t" + minWrongSimilarity);
            System.out.println("\tClamped result:\t\t" + clampedWrongSimilarity);
        }

        VerifyResult verifyResult = this.getVerifyResult(clampedWrongSimilarity);

        if (options.debug()) {
            System.out.println("\tCurrent try:\t\t" + this.currentTry + " / " + this.options.maxTries());
            System.out.println("\tResult:\t\t\t\t" + verifyResult);
        }

        return verifyResult;
    }

    private VerifyResult getVerifyResult(double result) {
        this.currentTry++;

        VerifyResult incorrectResult = VerifyResult.INCORRECT;
        if (this.currentTry >= this.options.maxTries())
            incorrectResult = VerifyResult.NO_MORE_TRIES;

        return result < 1.0D ? incorrectResult : VerifyResult.CORRECT;
    }

    private double getAngularSimilarity(Polygon source, Polygon toVerify) {
        if (source.getVerticesCount() < 3 || toVerify.getVerticesCount() < 3)
            return 1.0D;

        int sampleSize = Math.min(Math.min(
                this.options.angularDiffMaxCheckSamples(),
                source.getVerticesCount() / 2),
                toVerify.getVerticesCount() / 2);

        int lastSourceStartIndex = -1;
        int lastToVerifyStartIndex = -1;

        int samplesTaken = 0;
        double totalAngularDiff = 0.0D;

        for (int i = 0; i < sampleSize; i++) {
            double percentage = (1.0D / sampleSize) * i;
            int sourceStartIndex = (int) (source.getVerticesCount() * percentage);
            int toVerifyStartIndex = (int) (toVerify.getVerticesCount() * percentage);

            if (lastSourceStartIndex == sourceStartIndex ||
                    lastToVerifyStartIndex == toVerifyStartIndex ||
                    sourceStartIndex + 1 >= source.getVerticesCount() ||
                    toVerifyStartIndex + 1 >= toVerify.getVerticesCount()) {
                continue;
            }

            Point sourceA = source.getVertices().get(sourceStartIndex);
            Point sourceB = source.getVertices().get(sourceStartIndex + 1);
            Point toVerifyA = toVerify.getVertices().get(toVerifyStartIndex);
            Point toVerifyB = toVerify.getVertices().get(toVerifyStartIndex + 1);

            double scaleFactor = 360.0D / this.options.maxAngleRangeToScore();
            totalAngularDiff += PolygonUtil.getVertexVectorDirectionDiff(sourceA, sourceB, toVerifyA, toVerifyB) * scaleFactor;

            samplesTaken++;
            lastSourceStartIndex = sourceStartIndex;
            lastToVerifyStartIndex = toVerifyStartIndex;
        }

        double result = 1.0 - (totalAngularDiff / samplesTaken);

        if (this.options.debug()) {
            System.out.println("\nANGULAR SIMILARITY:");
            System.out.println("\tExpected samples:\t" + sampleSize);
            System.out.println("\tActual samples:\t\t" + samplesTaken);
            System.out.println("\tResult:\t\t\t\t" + result);
        }

        return result;
    }

    private double getLengthSimilarity(Polygon source, Polygon toVerify) {
        double verticesCountDiff = Math.abs(source.getVerticesCount() - toVerify.getVerticesCount());
        double result = 1.0D - Math.pow(verticesCountDiff / source.getVerticesCount(), 1.0 / this.options.lengthCorrectnessExp());

        if (this.options.debug()) {
            System.out.println("\nLENGTH SIMILARITY:");
            System.out.println("\tSource vertices:\t" + source.getVerticesCount());
            System.out.println("\tTo verify vertices:\t" + toVerify.getVerticesCount());
            System.out.println("\tVertices diff:\t\t" + verticesCountDiff);
            double baseDiff = 1.0D - verticesCountDiff / source.getVerticesCount();
            System.out.println("\tBase diff:\t\t\t" + baseDiff);
            System.out.println("\tDiff factor:\t\t" + result / baseDiff);
            System.out.println("\tResult:\t\t\t\t" + result);
        }

        return result;
    }

    private void copyContent(Canvas sourceCanvas, Canvas targetCanvas) {
        PixelReader sourceReader = this.getPixelReader(sourceCanvas);
        GraphicsContext targetGc = targetCanvas.getGraphicsContext2D();
        for (int x = 0; x < sourceCanvas.getWidth(); x++) {
            for (int y = 0; y < sourceCanvas.getHeight(); y++) {
                Color color = sourceReader.getColor(x, y);
                if (!color.equals(BASE_COLOR)) {
                    targetGc.setFill(color);
                    targetGc.fillRect(x, y, 1.0D, 1.0D);
                }
            }
        }
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

        if (this.options.debug()) {
            System.out.println("\nIMAGE SIMILARITY:");
            System.out.println("\tTotal color diff:\t" + totalColorDiff);
            System.out.println("\tCompared Pixels:\t" + comparedPixels);
        }

        double result = 1.0D;

        if (comparedPixels != 0)
            result = 1.0D - totalColorDiff / comparedPixels;

        if (options.debug())
            System.out.println("\tResult:\t\t\t\t" + result);

        return result;
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
        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);
        return toVerifyCanvas.snapshot(snapshotParameters, null).getPixelReader();
    }

    private void clearCanvas(Canvas canvas, boolean setBackground) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        if (setBackground) {
            gc.setFill(BASE_COLOR);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        } else {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
    }

    private enum Invalidator {
        NO,
        IMAGE,
        LENGTH,
        ANGLE
    }
}
