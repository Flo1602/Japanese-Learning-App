package at.primetshofer.logic.tracing;

import at.primetshofer.logic.provider.polygon.IPolygonProvider;
import at.primetshofer.logic.tracing.verification.ITraceVerificationLogic;
import at.primetshofer.logic.tracing.verification.VerifyResult;
import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnicodeTraceLineLogic implements ITraceLogic<Character> {

    private final Set<ITraceLineListener> listeners;
    private final TraceLineOptions options;
    private final ITraceVerificationLogic verificationLogic;
    private final IPolygonProvider polygonProvider;
    private final ITraceTargetChanger<Character> targetChanger;
    private final List<Polygon> loadedPolygons;

    private int nextPolygonToDraw = 0;
    private TraceMode currentTraceMode;

    public UnicodeTraceLineLogic(TraceLineOptions options, ITraceVerificationLogic verificationLogic, IPolygonProvider polygonProvider, ITraceTargetChanger<Character> targetChanger) {
        this.listeners = new HashSet<>();
        this.options = options;
        this.verificationLogic = verificationLogic;
        this.polygonProvider = polygonProvider;
        this.targetChanger = targetChanger;
        this.loadedPolygons = new ArrayList<>();
    }

    private void loadPolygons() {
        //this.iterateListeners(ITraceLineListener::onResetProgress);
        this.loadedPolygons.clear();
        this.loadedPolygons.addAll(this.polygonProvider.getAllPolygons());
        this.nextPolygonToDraw = this.loadedPolygons.isEmpty() ? -1 : 0;
    }

    @Override
    public void startTracing(TraceMode traceMode) {
        this.reset();
        this.currentTraceMode = traceMode;
        this.loadPolygons();

        if (traceMode == TraceMode.ALL_HINTS) {
            for (Polygon polygon : this.loadedPolygons) {
                this.iterateListeners(l -> l.onShowHint(polygon));
            }
        }

        this.traceNextPolygon();
    }

    private void traceNextPolygon() {
        if (this.loadedPolygons.size() <= this.nextPolygonToDraw) {
            this.iterateListeners(l -> l.onShowHintArrow(null, null));
            this.iterateListeners(l -> l.onFinished(true));
            return;
        }

        Polygon source = this.loadedPolygons.get(this.nextPolygonToDraw);
        if (this.currentTraceMode == TraceMode.NEXT_HINT)
            this.iterateListeners(l -> l.onShowHint(source));

        if (source.getVerticesCount() > 3) {
            Point hintArrowFrom = source.getVertices().get(2);
            Point hintArrowTo = source.getVertices().get((int) Math.ceil(source.getVerticesCount() * 0.3D));
            if (this.currentTraceMode != TraceMode.NO_HINTS || this.nextPolygonToDraw == 0)
                this.iterateListeners(l -> l.onShowHintArrow(hintArrowFrom, hintArrowTo));
        }

        this.iterateListeners(l -> l.onBeginTracing(this::onTraceFinished));
    }

    private void onTraceFinished(Polygon tracedPolygon) {
        Polygon source = this.loadedPolygons.get(this.nextPolygonToDraw);
        VerifyResult verifyResult = this.verificationLogic.verify(source, tracedPolygon);

        if (verifyResult == VerifyResult.NO_MORE_TRIES) {
            this.iterateListeners(l -> l.onFinished(false));
            return;
        }

        if (verifyResult != VerifyResult.INCORRECT)
            this.nextPolygonToDraw++;

        if (verifyResult == VerifyResult.CORRECT)
            this.verificationLogic.resetTries();

        List<Polygon> correctedPolys = this.loadedPolygons.stream().limit(this.nextPolygonToDraw).toList();
        this.iterateListeners(l -> l.onDrawCorrectedLines(correctedPolys));
        this.traceNextPolygon();
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
    }

    private void reset() {
        this.iterateListeners(ITraceLineListener::onResetProgress);
        this.nextPolygonToDraw = 0;
        this.verificationLogic.resetTries();
    }

    private interface Consumer<T> {
        void consume(T val);
    }
}
