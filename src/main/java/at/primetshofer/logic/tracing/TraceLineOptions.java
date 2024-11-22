package at.primetshofer.logic.tracing;

import javafx.scene.paint.Color;

public record TraceLineOptions(
        Color hintColor,
        double fieldWidth,
        double fieldHeight,
        double lineWidth,
        int transitionLines,
        double maxTransitionLineWidth
) { }
