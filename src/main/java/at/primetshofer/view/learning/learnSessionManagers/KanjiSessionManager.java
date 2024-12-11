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
import at.primetshofer.model.Controller;
import at.primetshofer.model.Polygon;
import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.view.learning.learnViews.KanjiTracerLearnView;
import at.primetshofer.view.learning.learnViews.WordKanjiSelectLearnView;
import at.primetshofer.view.learning.learnViews.matchLearnViews.JapaneseToKanaMatch;
import at.primetshofer.view.learning.learnViews.sentenceLearnViews.WordBuilderView;
import jakarta.persistence.EntityManager;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KanjiSessionManager extends LearnSessionManager {

    private static final int MAX_COUNTER = 9;

    private ITraceLogic<Character> traceLogic;
    private int currentCounter = 0;
    private Kanji kanji;
    private KanjiTracerLearnView kanjiTracerLearnView;
    private boolean wordBuilder = false;

    public KanjiSessionManager(Scene scene) {
        super(scene);
        this.kanji = Controller.getInstance().getNextLearningKanji();
        this.initLogic();
        this.traceLogic.changeTarget(kanji.getSymbol().charAt(0));
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
        kanjiTracerLearnView = new KanjiTracerLearnView(
                this,
                traceLogic,
                hintLineDrawer,
                correctingLineDrawer,
                kanji
        );
    }

    @Override
    protected void startLearning() {
        kanjiTracerLearnView.initView();
        nextLearningView();
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
        Random random = new Random();

        if(currentCounter == 0){
            Word word = kanji.getWords().get(random.nextInt(kanji.getWords().size()));
            currentLearnView = new WordKanjiSelectLearnView(this, word);
            bp.setCenter(currentLearnView.initView());
        } else if(currentCounter <= 2){
            this.traceLogic.startTracing(TraceMode.ALL_HINTS);
            currentLearnView = kanjiTracerLearnView;
            bp.setCenter(kanjiTracerLearnView.getPane());
        } else if(currentCounter <= 6){
            int rand = random.nextInt(6);
            if(rand == 0 && wordBuilder){
                rand++;
            }
            switch (rand){
                case 0 -> {
                    Word word = kanji.getWords().get(random.nextInt(kanji.getWords().size()));
                    currentLearnView = new WordBuilderView(this, word);
                    bp.setCenter(currentLearnView.initView());
                    wordBuilder = true;
                }
                case 1,2,3 -> {
                    this.traceLogic.startTracing(TraceMode.NEXT_HINT);
                    currentLearnView = kanjiTracerLearnView;
                    bp.setCenter(kanjiTracerLearnView.getPane());
                }
                case 4,5 -> {
                    List<Word> words = new ArrayList<>(5);
                    words.add(kanji.getWords().get(random.nextInt(kanji.getWords().size())));
                    words.addAll(getRandomWords());
                    currentLearnView = new JapaneseToKanaMatch(this, words);
                    bp.setCenter(currentLearnView.initView());
                }
            }
        } else if(currentCounter <= 8){
            this.traceLogic.startTracing(TraceMode.NO_HINTS);
            currentLearnView = kanjiTracerLearnView;
            bp.setCenter(kanjiTracerLearnView.getPane());
        } else {
            learnSessionFinished();
        }

        this.currentCounter++;
        setProgress((double) this.currentCounter / MAX_COUNTER);
    }

    private List<Word> getRandomWords() {
        EntityManager entityManager = HibernateUtil.getEntityManager();
        String jpql = "SELECT w FROM Word w WHERE SIZE(w.kanjis) >= 1 ORDER BY FUNCTION('RAND')";
        return entityManager.createQuery(jpql, Word.class).setMaxResults(4).getResultList();
    }
}
