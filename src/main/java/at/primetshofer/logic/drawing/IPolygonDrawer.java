package at.primetshofer.logic.drawing;

import at.primetshofer.model.ColoredPolygon;
import at.primetshofer.model.Polygon;
import javafx.scene.canvas.GraphicsContext;

public interface IPolygonDrawer {
    ColoredPolygon drawPolygon(GraphicsContext gc, Polygon polygon);
}
