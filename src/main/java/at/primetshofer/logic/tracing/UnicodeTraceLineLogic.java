package at.primetshofer.logic.tracing;

import at.primetshofer.logic.drawing.ILineDrawer;
import at.primetshofer.logic.drawing.SmoothLineDrawer;
import at.primetshofer.logic.provider.polygon.IPolygonProvider;
import at.primetshofer.model.Polygon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnicodeTraceLineLogic implements ITraceLogic<Character> {

    private final Set<ITraceLineListener> listeners;
    private final TraceLineOptions options;
    private final IPolygonProvider polygonProvider;
    private final ITraceTargetChanger<Character> targetChanger;
    private final List<Polygon> loadedPolygons;
    private int nextPolygonToDraw = 0;

    public UnicodeTraceLineLogic(TraceLineOptions options, IPolygonProvider polygonProvider, ITraceTargetChanger<Character> targetChanger) {
        this.listeners = new HashSet<>();
        this.options = options;
        this.polygonProvider = polygonProvider;
        this.targetChanger = targetChanger;
        this.loadedPolygons = new ArrayList<>();
    }

    private void loadPolygons() {
        this.iterateListeners(ITraceLineListener::onResetProgress);
        this.loadedPolygons.clear();
        this.loadedPolygons.addAll(this.polygonProvider.getAllPolygons());
        this.nextPolygonToDraw = this.loadedPolygons.isEmpty() ? -1 : 0;
    }

    @Override
    public void drawAllHintLines() {
        boolean shouldDrawNext = true;
        while (shouldDrawNext) {
            shouldDrawNext = this.drawNextPolygon(true);
        }
    }

    @Override
    public void drawNextHintLine() {
        this.drawNextPolygon(true);
    }

    private boolean drawNextPolygon(boolean showHint) {
        if (this.loadedPolygons.size() <= this.nextPolygonToDraw)
            return false;

        Polygon toDraw = this.loadedPolygons.get(this.nextPolygonToDraw);
        this.iterateListeners(l -> l.onBeginTracing(toDraw, showHint));
        this.nextPolygonToDraw++;
        return true;
    }

    @Override
    public void addTraceLineListener(ITraceLineListener listenerToAdd) {
        this.listeners.add(listenerToAdd);
    }

    @Override
    public void removeTraceLineListener(ITraceLineListener listenerToRemove) {
        this.listeners.remove(listenerToRemove);
    }

    @Override
    public TraceLineOptions getOptions() {
        return this.options;
    }

    private void iterateListeners(Consumer<ITraceLineListener> consumer) {
        for (ITraceLineListener listener : this.listeners) {
            consumer.consume(listener);
        }
    }

    @Override
    public void changeTarget(Character targetIdentifier) {
        this.targetChanger.changeTarget(targetIdentifier);
        this.loadPolygons();
    }

    private interface Consumer<T> {
        void consume(T val);
    }
}
