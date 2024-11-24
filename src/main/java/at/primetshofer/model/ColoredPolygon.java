package at.primetshofer.model;

import javafx.scene.paint.Color;

import java.util.List;

public class ColoredPolygon extends Polygon {
    private final List<Color> colors;

    public ColoredPolygon(List<Point> vertices, List<Color> colors) {
        super(vertices);
        this.colors = colors;
    }

    public List<Color> getColors() {
        return this.colors;
    }
}
