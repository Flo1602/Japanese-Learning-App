package at.primetshofer.logic.drawing;

import at.primetshofer.model.ColoredPolygon;
import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class LayeredGradientPolygonDrawer extends ContextPreservingPolygonDrawer {

    private final int gradientLines;
    private final double maxGradientLineWidth;
    private final double maxHue;

    public LayeredGradientPolygonDrawer(double minGradientLineWidth,
                                        int gradientLines,
                                        double maxGradientLineWidth,
                                        double maxHue) {
        super(minGradientLineWidth);
        this.maxHue = maxHue;
        this.gradientLines = gradientLines;
        this.maxGradientLineWidth = maxGradientLineWidth;
    }

    @Override
    protected ColoredPolygon doDrawPolygon(GraphicsContext gc, Polygon polygon) {
        double gradientZoneSize = this.maxGradientLineWidth - super.lineWidth;
        double gradientLineStepSize = gradientZoneSize / this.gradientLines;
        double currentGradientLineWidth = this.maxGradientLineWidth;

        List<Color> colors = new ArrayList<>();
        for (double currGradientLine = 0; currGradientLine <= this.gradientLines; currGradientLine++) {
            double saturation = (currGradientLine + 1) / (this.gradientLines + 1);
            gc.setLineWidth(currentGradientLineWidth);
            colors = this.drawGradientLine(gc, polygon, saturation);
            currentGradientLineWidth -= gradientLineStepSize;
        }

        if (colors.isEmpty())
            return null;

        return new ColoredPolygon(polygon.getVertices(), colors);
    }

    private List<Color> drawGradientLine(GraphicsContext gc, Polygon polygon, double saturation) {
        List<Color> colors = new ArrayList<>(polygon.getVerticesCount());

        for (int i = 1; i < polygon.getVerticesCount(); i++) {
            Point fromVertex = polygon.getVertices().get(i - 1);
            Point toVertex = polygon.getVertices().get(i);

            double progress = (double) (i - 1) / polygon.getVerticesCount();
            Color color = Color.hsb(this.maxHue * progress, saturation, 1.0);
            colors.add(color);
            gc.setStroke(color);
            gc.strokeLine(fromVertex.getX(), fromVertex.getY(), toVertex.getX(), toVertex.getY());
        }

        colors.add(colors.getLast());
        return colors;
    }
}
