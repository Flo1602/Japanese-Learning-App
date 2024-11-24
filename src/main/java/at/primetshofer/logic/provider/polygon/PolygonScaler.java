package at.primetshofer.logic.provider.polygon;

import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;

import java.util.List;

public class PolygonScaler implements IPolygonConverter {

    private final double scaleFactor;

    public PolygonScaler(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    @Override
    public void convert(List<Polygon> toConvert) {
        for (Polygon polygon : toConvert) {
            this.scalePolygon(polygon);
        }
    }

    private void scalePolygon(Polygon polygon) {
        for (Point vertex : polygon.getVertices()) {
            vertex.setX(vertex.getX() * this.scaleFactor);
            vertex.setY(vertex.getY() * this.scaleFactor);
        }
    }
}
