package at.primetshofer.logic.provider.polygon;

import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;
import at.primetshofer.model.util.CollectionUtil;
import at.primetshofer.model.util.PolygonUtil;

import java.util.ArrayList;
import java.util.List;

public class VertexFixedDistanceSetter implements IPolygonConverter {

    private final double fixedDistance;

    public VertexFixedDistanceSetter(double fixedDistance) {
        this.fixedDistance = fixedDistance;
    }

    @Override
    public void convert(List<Polygon> toConvert) {
        for (Polygon polygon : toConvert) {
            this.setFixedDistanceBetweenPoints(polygon);
        }
    }

    public void setFixedDistanceBetweenPoints(Polygon polygon) {
        double totalDistance = PolygonUtil.calculatePolylineLength(polygon);
        int expectedPointAmount = (int) Math.round(totalDistance / this.fixedDistance);

        if (polygon.getVerticesCount() <= 1)
            return;

        if (polygon.getVerticesCount() == expectedPointAmount) {
            this.evenlySpacePoints(polygon);
            return;
        }

        List<Point> vertices = polygon.getVertices();
        int initialSize = polygon.getVerticesCount();
        if (initialSize > expectedPointAmount) {
            for (int i = 0; i < (initialSize - (expectedPointAmount - 1)); i++) {
                CollectionUtil.deleteMiddleElement(vertices);
                this.evenlySpacePoints(polygon);
            }
        } else {
            for (int i = 0; i < (expectedPointAmount - initialSize); i++) {
                vertices.add(vertices.get(polygon.getVerticesCount() - 1));
                this.evenlySpacePoints(polygon);
            }
        }
    }

    private void evenlySpacePoints(Polygon polygon) {
        if (polygon.getVerticesCount() < 2)
            return;

        double totalLength = PolygonUtil.calculatePolylineLength(polygon);
        double targetSpacing = totalLength / (polygon.getVerticesCount() - 1);
        List<Point> vertices = polygon.getVertices();
        List<Double> segmentLengths = this.calculateSegmentLengths(polygon, vertices);

        List<Point> evenlySpacedPoints = interpolateVertices(polygon, vertices, targetSpacing, segmentLengths);
        polygon.getVertices().clear();
        polygon.getVertices().addAll(evenlySpacedPoints);
    }

    private List<Point> interpolateVertices(Polygon polygon, List<Point> vertices, double targetSpacing, List<Double> segmentLengths) {
        List<Point> evenlySpacedPoints = new ArrayList<>();
        evenlySpacedPoints.add(vertices.get(0));

        double accumulatedDistance = 0;
        int currentSegment = 0;

        for (int i = 1; i < polygon.getVerticesCount() - 1; i++) {
            double targetDistance = i * targetSpacing;

            while (currentSegment < segmentLengths.size() &&
                    accumulatedDistance + segmentLengths.get(currentSegment) < targetDistance) {
                accumulatedDistance += segmentLengths.get(currentSegment);
                currentSegment++;
            }

            if (currentSegment < segmentLengths.size()) {
                double remainingDistance = targetDistance - accumulatedDistance;
                double segmentLength = segmentLengths.get(currentSegment);
                double t = remainingDistance / segmentLength;
                Point p1 = vertices.get(currentSegment);
                Point p2 = vertices.get(currentSegment + 1);
                evenlySpacedPoints.add(this.interpolate(p1, p2, t));
            }
        }

        evenlySpacedPoints.add(vertices.get(polygon.getVerticesCount() - 1));
        return evenlySpacedPoints;
    }

    private List<Double> calculateSegmentLengths(Polygon polygon, List<Point> vertices) {
        List<Double> segmentLengths = new ArrayList<>();

        for (int i = 1; i < polygon.getVerticesCount(); i++) {
            double length = PolygonUtil.calcDistanceBetweenPoints(vertices.get(i - 1), vertices.get(i));
            segmentLengths.add(length);
        }
        return segmentLengths;
    }

    private Point interpolate(Point a, Point b, double t) {
        double x = a.getX() + t * (b.getX() - a.getX());
        double y = a.getY() + t * (b.getY() - a.getY());
        return new Point(x, y);
    }
}
