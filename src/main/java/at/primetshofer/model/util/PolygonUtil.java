package at.primetshofer.model.util;

import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;

public class PolygonUtil {

    private PolygonUtil() { }

    public static double calculatePolylineLength(Polygon polygon) {
        if (polygon.getVerticesCount() < 2)
            return 0.0D;

        double distanceSum = 0.0D;
        for (int i = 1; i < polygon.getVerticesCount(); i++) {
            distanceSum += calcDistanceBetweenPoints(
                    polygon.getVertices().get(i - 1),
                    polygon.getVertices().get(i));
        }

        return distanceSum;
    }

    public static double calcDistanceBetweenPoints(Point a, Point b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
    }

    public static int getVertexIndexOfClosestVertex(Point vertexToCompare, Polygon polygonToSearchIn) {
        int closestVertexIndex = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < polygonToSearchIn.getVerticesCount(); i++) {
            double distance = calcDistanceBetweenPoints(vertexToCompare, polygonToSearchIn.getVertices().get(i));
            if (distance < minDistance) {
                minDistance = distance;
                closestVertexIndex = i;
            }
        }

        return closestVertexIndex;
    }
}
