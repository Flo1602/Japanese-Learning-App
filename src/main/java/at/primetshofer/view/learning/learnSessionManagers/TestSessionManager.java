package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.logic.drawing.ILineDrawer;
import at.primetshofer.logic.drawing.SmoothLineDrawer;
import at.primetshofer.logic.drawing.VerificationLineDrawer;
import at.primetshofer.logic.parser.ISVGPathParser;
import at.primetshofer.logic.parser.SVGPathParser;
import at.primetshofer.logic.provider.file.UnicodeFilenameFileProvider;
import at.primetshofer.logic.provider.polygon.*;
import at.primetshofer.logic.provider.polygon.SVGPolygonProvider.SVGPolyProviderOptions;
import at.primetshofer.logic.tracing.ITraceLogic;
import at.primetshofer.logic.tracing.ITraceTargetChanger;
import at.primetshofer.logic.tracing.UnicodeTraceLineLogic;
import at.primetshofer.logic.tracing.TraceLineOptions;
import at.primetshofer.model.Polygon;
import at.primetshofer.view.learning.learnViews.KanjiTracerLearnView;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.util.List;

public class TestSessionManager extends LearnSessionManager{

    public TestSessionManager(Scene scene) {
        super(scene);
    }

    @Override
    protected void startLearning() {
        TraceLineOptions options = new TraceLineOptions(
                Color.gray(0.3),
                500.0D,
                500.0D,
                15.0D,
                10,
                120.0D
        );

        ITraceLogic<Character> traceLogic = buildTraceLogic(options);
        ILineDrawer visibleLineDrawer = new VerificationLineDrawer(options.lineWidth(), options.transitionLines(), options.maxTransitionLineWidth());
                //= new SmoothLineDrawer(options.lineWidth(), options.hintColor());
        super.currentLearnView = new KanjiTracerLearnView(this, traceLogic, visibleLineDrawer);

        setProgress(100);

        super.bp.setCenter(super.currentLearnView.initView());
        traceLogic.changeTarget('挨');
        traceLogic.drawAllHintLines();
    }

    private static ITraceLogic<Character> buildTraceLogic(TraceLineOptions options) {
        UnicodeFilenameFileProvider fileProvider = new UnicodeFilenameFileProvider(
                "kanjivg-20240807-all",
                '0',
                5,
                ".svg"
        );

        ISVGPathParser svgPathParser = new SVGPathParser();
        SVGPolyProviderOptions polyProviderOptions = new SVGPolyProviderOptions(0.001, 5);

        IPolygonProvider sourcePolygonProvider = new SVGPolygonProvider(
                fileProvider,
                svgPathParser,
                polyProviderOptions
        );

        IPolygonConverter scaler = new PolygonScaler(4);
        IPolygonConverter fixedDistanceSetter = new VertexFixedDistanceSetter(scaler, 5);

        IPolygonProvider convertingPolygonProvider = () -> {
            List<Polygon> polygons = sourcePolygonProvider.getAllPolygons();
            fixedDistanceSetter.convert(polygons);
            return polygons;
        };

        ITraceTargetChanger<Character> targetChanger = fileProvider::setCharForFilename;
        return new UnicodeTraceLineLogic(options, convertingPolygonProvider, targetChanger);
    }

    @Override
    protected void nextLearningView() {
        super.learnSessionFinished();
    }
}
