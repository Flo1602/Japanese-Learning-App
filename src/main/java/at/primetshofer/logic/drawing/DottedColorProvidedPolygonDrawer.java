package at.primetshofer.logic.drawing;

import at.primetshofer.logic.provider.color.IColorProvider;
import at.primetshofer.model.ColoredPolygon;
import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class DottedColorProvidedPolygonDrawer extends ContextPreservingPolygonDrawer {

    private final IColorProvider colorProvider;

    public DottedColorProvidedPolygonDrawer(double lineWidth, IColorProvider colorProvider) {
        super(lineWidth);
        this.colorProvider = colorProvider;
    }

    @Override
    protected ColoredPolygon doDrawPolygon(GraphicsContext gc, Polygon polygon) {
        List<Point> vertices = polygon.getVertices();
        List<Color> colors = new ArrayList<>(polygon.getVerticesCount());
        for (int i = 0; i < polygon.getVerticesCount(); i++) {
            Color color = this.colorProvider.getColor(i, polygon);
            gc.setFill(color);
            colors.add(color);

            double pointOffset = super.lineWidth / 2.0D;
            Point vertex = vertices.get(i);
            gc.fillOval(
                    vertex.getX() - pointOffset,
                    vertex.getY() - pointOffset,
                    super.lineWidth,
                    super.lineWidth
            );
        }

        return new ColoredPolygon(vertices, colors);
    }
}
