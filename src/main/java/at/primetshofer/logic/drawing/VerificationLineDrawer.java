package at.primetshofer.logic.drawing;

import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class VerificationLineDrawer extends ContextPreservingLineDrawer {

    private static final int MAX_HUE = 270;

    private final int transitionLines;
    private final double maxTransitionLineWidth;

    public VerificationLineDrawer(double lineWidth, int transitionLines, double maxTransitionLineWidth) {
        super(lineWidth);
        this.transitionLines = transitionLines;
        this.maxTransitionLineWidth = maxTransitionLineWidth;
    }

    @Override
    protected void doDrawPolygon(GraphicsContext gc, Polygon polygon) {
        double transitionZoneSize = this.maxTransitionLineWidth - super.lineWidth;
        double transitionLineStepSize = transitionZoneSize / this.transitionLines;
        double currentTransitionLineWidth = this.maxTransitionLineWidth;

        for (double currTransitionLine = 0; currTransitionLine <= this.transitionLines; currTransitionLine++) {
            double saturation = (currTransitionLine + 1) / (this.transitionLines + 1);
            System.out.println(saturation);
            gc.setLineWidth(currentTransitionLineWidth);
            this.drawTransitionLine(gc, polygon, saturation);
            currentTransitionLineWidth -= transitionLineStepSize;
        }
    }

    private void drawTransitionLine(GraphicsContext gc, Polygon polygon, double saturation) {
        for (int i = 1; i < polygon.getVerticesCount(); i++) {
            Point fromVertex = polygon.getVertices().get(i - 1);
            Point toVertex = polygon.getVertices().get(i);

            double progress = (double) i / polygon.getVerticesCount();
            gc.setStroke(Color.hsb(MAX_HUE * progress, saturation, 1.0));
            gc.strokeLine(fromVertex.getX(), fromVertex.getY(), toVertex.getX(), toVertex.getY());
        }
    }
}
