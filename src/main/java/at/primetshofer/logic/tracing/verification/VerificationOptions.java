package at.primetshofer.logic.tracing.verification;


/**
 * @param gradientLines amount of drawn overlapping lines, higher values improve result smoothness
 * @param minGradientLineWidth size of the line within a perfect score can be achieved (main line width)
 * @param maxGradientLineWidth size of the line within any score can be achieved
 * @param toVerifyDotSize size of the control line, should be smaller then minGradientLineWidth
 * @param colorCorrectnessExp exponent to punish wrong values<br/>
 *      1.0 means that all vertices have to be in minGradientLineWidth to get a high score<br/>
 *      values below 1.0 decrease difficulty<br/>
 *      values above 1.0 increase difficulty (wrong pixels get more punished, not recommended)<br/>
 *      Examples:<br/><pre>
 *          2.0 get ~ <8% accuracy for 5mm beside main line<br/>
 *          1.5 get ~ <20% accuracy for 5mm beside main line<br/>
 *          1.2 get ~ <33% accuracy for 5mm beside main line<br/>
 *          1.0 get ~ >55% accuracy for 5mm beside main line<br/>
 *          0.8 get ~ >65% accuracy for 5mm beside main line<br/>
 *          0.6 get ~ >85% accuracy for 5mm beside main line<br/>
 *          0.4 get ~ >92% accuracy for 5mm beside main line<br/>
 *          0.2 get ~ >98% accuracy for 5mm, 90% for 15mm beside main line<br/><pre/>
 *
 * @param lengthCorrectnessExp exponent to punish wrong values<br/>
 *      1.0 means that for 100% the length has to match perfectly<br/>
 *      values below 1.0 decrease difficulty<br/>
 *      values above 1.0 increase difficulty<br/>
 *      Examples:<br/><pre>
 *      &nbsp;2.0 get <20% for 0.5 | 1.5 times the polygon length<br/>
 *      &nbsp;1.8 get <28% for 0.5 | 1.5 times the polygon length<br/>
 *      &nbsp;1.5 get <36% for 0.5 | 1.5 times the polygon length<br/>
 *      &nbsp;1.2 get <42% for 0.5 | 1.5 times the polygon length<br/>
 *      &nbsp;1.0 get >50% for 0.5 | 1.5 times the polygon length<br/>
 *      &nbsp;0.8 get >58% for 0.5 | 1.5 times the polygon length<br/>
 *      &nbsp;0.6 get >68% for 0.5 | 1.5 times the polygon length<br/>
 *      &nbsp;0.4 get >80% for 0.5 | 1.5 times the polygon length<br/>
 *      &nbsp;0.2 get >88% for 0.5 | 1.5 times the polygon length<br/><pre/>
 *
 * @param maxAngleRangeToScore max angle range to score points (values: 1 - 360)<br/>
 *      Examples:<br/><pre>
 *      &nbsp;360° gets 0% for opposite directions, 50% for a line normal to the polygon to draw<br/>
 *      &nbsp;180° 0% for a line normal to the polygon to draw and 50% for 45°<br/><pre/>
 * @param angularDiffMaxCheckSamples max amount of angles compared within the polygon
 * @param maxTries max tries to verify a polygon before no more tries gets returned
 * @param minImageSimilarity min percentage of image similarity to be correct (0.0 - 1.0)
 * @param minLengthSimilarity min percentage of length similarity to be correct (0.0 - 1.0)
 * @param minAngularSimilarity min percentage of angular similarity to be correct (0.0 - 1.0)
 * @param fieldWidth width of the verification field, polygons x values should be within this value
 * @param fieldHeight height of the verification field, polygons y values should be within this value
 * @param debug adds debug print statements for verification / creates debug canvas
 */
public record VerificationOptions(
        int gradientLines,
        double minGradientLineWidth,
        double maxGradientLineWidth,
        double toVerifyDotSize,
        double colorCorrectnessExp,
        double lengthCorrectnessExp,
        int maxAngleRangeToScore,
        int angularDiffMaxCheckSamples,
        int maxTries,
        double minImageSimilarity,
        double minLengthSimilarity,
        double minAngularSimilarity,
        double fieldWidth,
        double fieldHeight,
        boolean debug
) { }
