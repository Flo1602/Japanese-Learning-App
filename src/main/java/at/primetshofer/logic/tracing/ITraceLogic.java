package at.primetshofer.logic.tracing;


import at.primetshofer.logic.tracing.verification.ITraceVerificationLogic;
import at.primetshofer.model.Point;
import at.primetshofer.model.Polygon;

import java.util.List;

public interface ITraceLogic<T> extends ITraceTargetChanger<T> {
    void startTracing(TraceMode traceMode);

    void addTraceLineListener(ITraceLineListener listenerToAdd);

    void removeTraceLineListener(ITraceLineListener listenerToAdd);

    ITraceVerificationLogic getVerificationLogic();

    interface ITraceLineListener {
        void onShowHint(Polygon polygonToShowHintFor);

        void onDrawCorrectedLines(List<Polygon> correctedLine);

        void onBeginTracing(ITraceFinishedCallback callback);

        void onShowHintArrow(Point from, Point to);

        void onResetProgress();

        void onFinished(boolean correct);
    }

    interface ITraceFinishedCallback {
        boolean onTraceFinished(Polygon tracedPolygon);
    }
}
