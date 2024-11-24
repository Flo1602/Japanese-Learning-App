package at.primetshofer.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Polygon {
    private final List<Point> vertices;

    public Polygon() {
        this(new ArrayList<>());
    }

    public Polygon(List<Point> vertices) {
        this.vertices = vertices;
    }

    public List<Point> getVertices() {
        return this.vertices;
    }

    public int getVerticesCount() {
        return this.vertices.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Polygon polygon = (Polygon) o;
        return Objects.equals(this.vertices, polygon.vertices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.vertices);
    }
}
