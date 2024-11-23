package at.primetshofer.logic.provider.polygon;

import at.primetshofer.model.Polygon;

import java.util.List;

abstract class ChainedPolygonConverter implements IPolygonConverter {

    private final IPolygonConverter parent;

    protected ChainedPolygonConverter(IPolygonConverter parent) {
        this.parent = parent;
    }

    @Override
    public final void convert(List<Polygon> toConvert) {
        if (this.parent != null)
            this.parent.convert(toConvert);

        this.doConvert(toConvert);
    }

    protected abstract void doConvert(List<Polygon> toConvert);
}
