package at.primetshofer.logic.drawing;

import at.primetshofer.model.Polygon;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;

abstract class ContextPreservingLineDrawer implements ILineDrawer {

    protected final double lineWidth;

    protected ContextPreservingLineDrawer(double lineWidth) {
        this.lineWidth = lineWidth;
    }

    @Override
    public final void drawPolygon(GraphicsContext gc, Polygon polygon) {
        if (polygon.getVerticesCount() < 2)
            return;

        Paint prevStrokeColor = gc.getStroke();
        double prevLineWidth = gc.getLineWidth();

        this.doDrawPolygon(gc, polygon);

        gc.setStroke(prevStrokeColor);
        gc.setLineWidth(prevLineWidth);
    }

    protected abstract void doDrawPolygon(GraphicsContext gc, Polygon polygon);
}
