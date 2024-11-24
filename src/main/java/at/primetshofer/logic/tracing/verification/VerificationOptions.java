package at.primetshofer.logic.tracing.verification;

public record VerificationOptions(
        int gradientLines,
        double minGradientLineWidth,
        double maxGradientLineWidth,
        double toVerifyDotSize,
        double colorCorrectnessExp,
        double polygonCorrectnessExp,
        int maxTries,
        double fieldWidth,
        double fieldHeight
) { }
