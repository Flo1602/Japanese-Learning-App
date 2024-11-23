package at.primetshofer.logic.provider.polygon;

import at.primetshofer.model.Polygon;

import java.util.List;

public interface IPolygonConverter {
    void convert(List<Polygon> toConvert);
}
