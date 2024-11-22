package at.primetshofer.logic.tracing;

import at.primetshofer.logic.drawing.ILineDrawer;
import at.primetshofer.logic.drawing.SmoothLineDrawer;
import at.primetshofer.logic.drawing.VerificationLineDrawer;

import java.util.HashSet;
import java.util.Set;

public class TraceLineLogic implements ITraceLogic {

    private final Set<ITraceLineListener> listeners;
    private final TraceLineOptions options;
    private final ILineDrawer hintLineDrawer;
    private final ILineDrawer verificationLineDrawer;

    public TraceLineLogic(TraceLineOptions options) {
        this.listeners = new HashSet<>();
        this.options = options;
        this.hintLineDrawer = new SmoothLineDrawer(options.lineWidth(), options.hintColor());
        this.verificationLineDrawer = new VerificationLineDrawer(
                options.lineWidth(),
                options.transitionLines(),
                options.maxTransitionLineWidth());
    }

    @Override
    public void drawAllHintLines() {

    }

    @Override
    public void drawNextHintLine() {

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

    @Override
    public ILineDrawer getHintLineDrawer() {
        return this.hintLineDrawer;
    }

    @Override
    public ILineDrawer getVerificationLineDrawer() {
        return this.verificationLineDrawer;
    }

    private void iterateListeners(Consumer<ITraceLineListener> consumer) {
        for (ITraceLineListener listener : this.listeners) {
            consumer.consume(listener);
        }
    }

    private interface Consumer<T> {
        void consume(T val);
    }
}
