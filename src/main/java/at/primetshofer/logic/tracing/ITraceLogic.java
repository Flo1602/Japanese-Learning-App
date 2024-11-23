package at.primetshofer.logic.tracing;


import at.primetshofer.logic.drawing.ILineDrawer;
import at.primetshofer.model.Polygon;

public interface ITraceLogic<T> extends ITraceTargetChanger<T> {
    void drawAllHintLines();
    void drawNextHintLine();

    void addTraceLineListener(ITraceLineListener listenerToAdd);
    void removeTraceLineListener(ITraceLineListener listenerToAdd);
    TraceLineOptions getOptions();

    interface ITraceLineListener {
        void onBeginTracing(Polygon polygonToTracy, boolean showHint);

        void onResetProgress();
    }
}
