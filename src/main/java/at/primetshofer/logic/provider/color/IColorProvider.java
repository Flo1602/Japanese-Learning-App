package at.primetshofer.logic.provider.color;

import at.primetshofer.model.Polygon;
import javafx.scene.paint.Color;

public interface IColorProvider {
    Color getColor(int currentVertex, Polygon polygon);
}