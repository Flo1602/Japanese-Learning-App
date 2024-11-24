package at.primetshofer.logic.tracing;

public interface ITraceTargetChanger<T> {
    void changeTarget(T targetIdentifier);
}
