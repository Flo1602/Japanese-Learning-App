package at.primetshofer.model.util;

import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;

public class MathUtil {

    private MathUtil() { }

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
}
