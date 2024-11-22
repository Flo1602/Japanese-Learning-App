package at.primetshofer.logic.drawing;

import at.primetshofer.model.Polygon;
import javafx.scene.canvas.GraphicsContext;

public interface ILineDrawer {
    void drawPolygon(GraphicsContext gc, Polygon polygon);
}
