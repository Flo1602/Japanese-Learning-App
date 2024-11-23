package at.primetshofer.model.util;

import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;

import java.util.List;

public class PolygonUtil {

    private PolygonUtil() { }

    public static double calculatePolylineLength(Polygon polygon) {
        if (polygon.getVerticesCount() < 2)
            return 0.0D;

        double distanceSum = 0.0D;
        for (int i = 1; i < polygon.getVerticesCount(); i++) {
            distanceSum += calculateDistanceBetweenPoints(
                    polygon.getVertices().get(i - 1),
                    polygon.getVertices().get(i));
        }

        return distanceSum;
    }

    public static double calculateDistanceBetweenPoints(Point a, Point b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
    }

    public static double getQuadraticBoundingBoxSize(List<Polygon> polygons) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Polygon polygon : polygons) {
            for (Point vertex : polygon.getVertices()) {
                if (vertex.getX() < minX)
                    minX = vertex.getX();

                if (vertex.getY() < minY)
                    minY = vertex.getY();

                if (vertex.getX() > maxX)
                    maxX = vertex.getX();

                if (vertex.getY() > maxY)
                    maxY = vertex.getY();
            }
        }

        double width = maxX - minX;
        double height = maxY - minY;
        return Math.max(width, height);
    }
}
