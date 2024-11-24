package at.primetshofer.logic.tracing;

import javafx.scene.paint.Color;

public record TraceLineOptions(
        Color drawingColor,
        Color hintColor,
        double fieldWidth,
        double fieldHeight,
        double lineWidth
) { }
