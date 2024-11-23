package at.primetshofer.logic.provider.polygon;

import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;

import java.util.List;

public class PolygonScaler extends ChainedPolygonConverter {

    private final double scaleFactor;

    public PolygonScaler(IPolygonConverter parent, double scaleFactor) {
        super(parent);
        this.scaleFactor = scaleFactor;
    }

    public PolygonScaler(double scaleFactor) {
        this(null, scaleFactor);
    }

    @Override
    protected void doConvert(List<Polygon> toConvert) {
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
