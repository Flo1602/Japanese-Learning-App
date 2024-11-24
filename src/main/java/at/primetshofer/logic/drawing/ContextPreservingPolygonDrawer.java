package at.primetshofer.logic.drawing;

import at.primetshofer.model.ColoredPolygon;
import at.primetshofer.model.Polygon;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;

abstract class ContextPreservingPolygonDrawer implements IPolygonDrawer {

    protected final double lineWidth;
    private final StrokeLineCap lineCap;

    protected ContextPreservingPolygonDrawer(double lineWidth) {
        this(lineWidth, StrokeLineCap.ROUND);
    }

    protected ContextPreservingPolygonDrawer(double lineWidth, StrokeLineCap lineCap) {
        this.lineWidth = lineWidth;
        this.lineCap = lineCap;
    }

    @Override
    public final ColoredPolygon drawPolygon(GraphicsContext gc, Polygon polygon) {
        if (polygon.getVerticesCount() < 2)
            return null;

        StrokeLineCap prevLineCap = gc.getLineCap();
        Paint prevStrokeColor = gc.getStroke();
        double prevLineWidth = gc.getLineWidth();

        gc.setLineCap(this.lineCap);
        ColoredPolygon returnValue = this.doDrawPolygon(gc, polygon);

        gc.setLineCap(prevLineCap);
        gc.setStroke(prevStrokeColor);
        gc.setLineWidth(prevLineWidth);
        return returnValue;
    }

    protected abstract ColoredPolygon doDrawPolygon(GraphicsContext gc, Polygon polygon);
}
