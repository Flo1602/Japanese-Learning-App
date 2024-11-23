package at.primetshofer.logic.drawing;

import at.primetshofer.model.Polygon;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;

abstract class ContextPreservingLineDrawer implements ILineDrawer {

    protected final double lineWidth;
    private final StrokeLineCap lineCap;

    protected ContextPreservingLineDrawer(double lineWidth) {
        this(lineWidth, StrokeLineCap.ROUND);
    }

    protected ContextPreservingLineDrawer(double lineWidth, StrokeLineCap lineCap) {
        this.lineWidth = lineWidth;
        this.lineCap = lineCap;
    }

    @Override
    public final void drawPolygon(GraphicsContext gc, Polygon polygon) {
        if (polygon.getVerticesCount() < 2)
            return;

        StrokeLineCap prevLineCap = gc.getLineCap();
        Paint prevStrokeColor = gc.getStroke();
        double prevLineWidth = gc.getLineWidth();

        gc.setLineCap(this.lineCap);
        this.doDrawPolygon(gc, polygon);

        gc.setLineCap(prevLineCap);
        gc.setStroke(prevStrokeColor);
        gc.setLineWidth(prevLineWidth);
    }

    protected abstract void doDrawPolygon(GraphicsContext gc, Polygon polygon);
}
