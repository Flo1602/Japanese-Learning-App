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

    public static double getVertexVectorDirectionDiff(Point sourceA, Point sourceB, Point toCheckA, Point toCheckB) {
        double v1x = sourceB.getX() - sourceA.getX();
        double v1y = sourceB.getY() - sourceA.getY();
        double v2x = toCheckB.getX() - toCheckA.getX();
        double v2y = toCheckB.getY() - toCheckA.getY();

        double dotProduct = (v1x * v2x) + (v1y * v2y);
        double magnitudeV1 = Math.sqrt(v1x * v1x + v1y * v1y);
        double magnitudeV2 = Math.sqrt(v2x * v2x + v2y * v2y);
        double cosTheta = dotProduct / (magnitudeV1 * magnitudeV2);
        cosTheta = Math.max(-1.0, Math.min(1.0, cosTheta));
        double thetaRadians = Math.acos(cosTheta);
        double thetaDegrees = Math.toDegrees(thetaRadians);
        return thetaDegrees / 180.0;
    }
}
