package at.primetshofer.logic.tracing.verification;

import at.primetshofer.model.Polygon;

public interface ITraceVerificationLogic {
    VerifyResult verify(Polygon source, Polygon toVerify);

    void resetTries();
}
