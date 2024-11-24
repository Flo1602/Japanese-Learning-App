package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.logic.drawing.IPolygonDrawer;
import at.primetshofer.logic.drawing.SmoothPolygonDrawer;
import at.primetshofer.logic.parser.ISVGPathParser;
import at.primetshofer.logic.parser.SVGPathParser;
import at.primetshofer.logic.provider.file.UnicodeFilenameFileProvider;
import at.primetshofer.logic.provider.polygon.*;
import at.primetshofer.logic.provider.polygon.SVGPolygonProvider.SVGPolyProviderOptions;
import at.primetshofer.logic.tracing.*;
import at.primetshofer.logic.tracing.verification.GradientVerificationLogic;
import at.primetshofer.logic.tracing.verification.ITraceVerificationLogic;
import at.primetshofer.logic.tracing.verification.VerificationOptions;
import at.primetshofer.model.Polygon;
import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnViews.KanjiTracerLearnView;
import jakarta.persistence.EntityManager;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.util.List;

public class TestSessionManager extends LearnSessionManager {

    private static final int MAX_COUNTER = 5;

    private ITraceLogic<Character> traceLogic;
    int currentCounter = 0;

    public TestSessionManager(Scene scene) {
        super(scene);
        this.initLogic();
        this.traceLogic.changeTarget(this.getRandomKanji());
    }

    private void initLogic() {
        TraceLineOptions traceOptions = new TraceLineOptions(
                Color.WHITE,
                Color.gray(0.3),
                500.0D,
                500.0D,
                15.0D
        );

        VerificationOptions verificationOptions = new VerificationOptions(
                10,
                traceOptions.lineWidth() * 2.0D,
                traceOptions.lineWidth() * 10.0D,
                traceOptions.lineWidth(),
                0.2D,
                0.5D,
                3,
                traceOptions.fieldWidth(),
                traceOptions.fieldHeight()
        );

        this.traceLogic = buildLogic(traceOptions, verificationOptions);
        IPolygonDrawer hintLineDrawer = new SmoothPolygonDrawer(traceOptions.lineWidth(), traceOptions.hintColor());
        IPolygonDrawer correctingLineDrawer = new SmoothPolygonDrawer(traceOptions.lineWidth(), traceOptions.drawingColor());
        super.currentLearnView = new KanjiTracerLearnView(
                this,
                traceLogic,
                hintLineDrawer,
                correctingLineDrawer
        );
    }

    @Override
    protected void startLearning() {
        super.bp.setCenter(super.currentLearnView.initView());
        this.traceLogic.startTracing(TraceMode.ALL_HINTS);
        setProgress(0);
    }

    private Character getRandomKanji() {
        EntityManager entityManager = HibernateUtil.getEntityManager();
        String sql = "SELECT * FROM KANJI ORDER BY RAND() LIMIT 1";
        return ((Kanji) entityManager.createNativeQuery(sql, Kanji.class).getResultList().getFirst())
                .getSymbol().charAt(0);
    }

    private static ITraceLogic<Character> buildLogic(TraceLineOptions traceOptions, VerificationOptions verificationOptions) {
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
        IPolygonConverter fixedDistanceSetter = new VertexFixedDistanceSetter(5);

        IPolygonProvider convertingPolygonProvider = () -> {
            List<Polygon> polygons = sourcePolygonProvider.getAllPolygons();
            scaler.convert(polygons);
            fixedDistanceSetter.convert(polygons);
            return polygons;
        };

        ITraceVerificationLogic verificationLogic = new GradientVerificationLogic(
                verificationOptions,
                fixedDistanceSetter
        );

        ITraceTargetChanger<Character> targetChanger = fileProvider::setCharForFilename;
        return new UnicodeTraceLineLogic(traceOptions, verificationLogic, convertingPolygonProvider, targetChanger);
    }

    @Override
    protected void nextLearningView() {
        if (this.currentCounter >= MAX_COUNTER) {
            super.learnSessionFinished();
        } else if (this.currentCounter >= 3) {
            this.traceLogic.startTracing(TraceMode.NO_HINTS);
        } else {
            this.traceLogic.startTracing(TraceMode.NEXT_HINT);
        }

        this.currentCounter++;
        setProgress((double) this.currentCounter / MAX_COUNTER);
    }
}
