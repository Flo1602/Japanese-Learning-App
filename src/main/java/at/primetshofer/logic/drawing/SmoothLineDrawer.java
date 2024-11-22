package at.primetshofer.logic.drawing;

import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class SmoothLineDrawer extends ContextPreservingLineDrawer {

    private final Color strokeColor;

    public SmoothLineDrawer(double lineWidth, Color strokeColor) {
        super(lineWidth);
        this.strokeColor = strokeColor;
    }

    @Override
    protected void doDrawPolygon(GraphicsContext gc, Polygon polygon) {
        List<Point> vertices = polygon.getVertices();
        gc.moveTo(vertices.get(0).getX(), vertices.get(0).getY());
        gc.setStroke(this.strokeColor);
        gc.setLineWidth(super.lineWidth);
        gc.beginPath();

        for (int i = 1; i < polygon.getVerticesCount() - 2; i++) {
            Point currentVertex = vertices.get(i);
            Point nextVertex = vertices.get(i + 1);

            double intermediateX = (currentVertex.getX() + nextVertex.getX()) / 2.0;
            double intermediateY = (currentVertex.getY() + nextVertex.getY()) / 2.0;
            gc.quadraticCurveTo(
                    currentVertex.getX(), currentVertex.getY(),
                    intermediateX, intermediateY);
        }

        // Draw the last segment
        Point lastVertex = vertices.get(polygon.getVerticesCount() - 1);
        Point secondToLastVertex = vertices.get(polygon.getVerticesCount() - 2);
        gc.quadraticCurveTo(
                secondToLastVertex.getX(), secondToLastVertex.getY(),
                lastVertex.getX(), lastVertex.getY());

        gc.stroke();
    }
}
