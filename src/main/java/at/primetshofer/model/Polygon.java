package at.primetshofer.model;

import java.util.ArrayList;
import java.util.List;

public class Polygon {
    private final List<Point> vertices;

    public Polygon() {
        this.vertices = new ArrayList<>();
    }

    public List<Point> getVertices() {
        return this.vertices;
    }

    public int getVerticesCount() {
        return this.vertices.size();
    }
}
