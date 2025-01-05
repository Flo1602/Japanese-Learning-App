package at.primetshofer.view.learning.learnSessionManagers;

import at.primetshofer.logic.drawing.IPolygonDrawer;
import at.primetshofer.logic.drawing.SmoothPolygonDrawer;
import at.primetshofer.logic.parser.ISVGPathParser;
import at.primetshofer.logic.parser.SVGPathParser;
import at.primetshofer.logic.provider.file.UnicodeFilenameFileProvider;
import at.primetshofer.logic.provider.polygon.*;
import at.primetshofer.logic.provider.polygon.SVGPolygonProvider.SVGPolyProviderOptions;
import at.primetshofer.logic.tracing.*;
import at.primetshofer.logic.tracing.verification.ITraceVerificationLogic;
import at.primetshofer.logic.tracing.verification.VerificationLogic;
import at.primetshofer.logic.tracing.verification.VerificationOptions;
import at.primetshofer.model.Controller;
import at.primetshofer.model.Polygon;
import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.KanjiProgress;
import at.primetshofer.model.entities.Word;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.learning.learnViews.KanjiTracerLearnView;
import at.primetshofer.view.learning.learnViews.WordKanjiSelectLearnView;
import at.primetshofer.view.learning.learnViews.matchLearnViews.JapaneseToKanaMatch;
import at.primetshofer.view.learning.learnViews.matchLearnViews.VocabAudioToJapaneseMatch;
import at.primetshofer.view.learning.learnViews.sentenceLearnViews.WordBuilderView;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;

import java.util.*;

public class KanjiSessionManager extends LearnSessionManager {

    private final int EASY_CAP = 3;
    private final int MID_CAP = 6;

    private ITraceLogic<Character> traceLogic;
    private int currentCounter;
    private Kanji kanji;
    private KanjiTracerLearnView kanjiTracerLearnView;
    private boolean wordBuilder;
    private int difficulty;
    private Random random;
    private int builderChanceIncrease;
    private boolean specificKanji;

    public KanjiSessionManager(Scene scene) {
        super(scene);
        random = new Random();
        specificKanji = false;
    }

    public void setSpecificKanji(Kanji kanji) {
        this.kanji = kanji;
        this.specificKanji = true;
    }

    @Override
    public void initSessionManager() {
        super.initSessionManager();
        currentCounter = 0;
        wordBuilder = false;
        builderChanceIncrease = 0;
        if(!specificKanji) {
            this.kanji = Controller.getInstance().getNextLearningKanji();
        }
        specificKanji = false;
        calcDifficulty();

        if(kanjiTracerLearnView == null) {
            this.initLogic();
        }

        kanjiTracerLearnView.setKanji(kanji);
        traceLogic.changeTarget(kanji.getSymbol().charAt(0));

        for (Word word : kanji.getWords()) {
            if(word.getJapanese().length() > 1){
                builderChanceIncrease = 1;
                break;
            }
        }
    }

    private void calcDifficulty(){
        difficulty = 0;
        for (KanjiProgress progress : kanji.getProgresses()) {
            difficulty += progress.getCompressedEntries();
        }
        System.out.println("difficulty: " + difficulty);
    }

    private void initLogic() {
        TraceLineOptions traceOptions = new TraceLineOptions(
                Color.WHITE,
                Color.gray(0.3),
                500.0D,
                500.0D,
                15.0D
        );

        boolean debug = false;

        VerificationOptions verificationOptions = new VerificationOptions(
                10,
                traceOptions.lineWidth() * 2D,
                traceOptions.lineWidth() * 7D,
                traceOptions.lineWidth(),
                0.6D,
                0.5D,
                180,
                20,
                3,
                0.6,
                0.4,
                0.6,
                traceOptions.fieldWidth(),
                traceOptions.fieldHeight(),
                debug
        );

        traceLogic = buildLogic(verificationOptions);
        IPolygonDrawer hintLineDrawer = new SmoothPolygonDrawer(traceOptions.lineWidth(), traceOptions.hintColor());
        IPolygonDrawer correctingLineDrawer = new SmoothPolygonDrawer(traceOptions.lineWidth(), traceOptions.drawingColor());
        kanjiTracerLearnView = new KanjiTracerLearnView(
                this,
                traceOptions,
                traceLogic,
                hintLineDrawer,
                correctingLineDrawer,
                debug
        );
    }

    @Override
    protected void startLearning() {
        kanjiTracerLearnView.initView();

        if(kanji.getWords().isEmpty()){
            ViewUtils.showAlert(Alert.AlertType.ERROR, "No words for current Kanji found!", "No words for: " + kanji.getSymbol());
            return;
        }

        if(difficulty < EASY_CAP){
            super.setMaxViews(9);
        } else if(difficulty < MID_CAP){
            super.setMaxViews(8);
        } else {
            super.setMaxViews(7);
        }

        nextLearningView();
    }

    private static ITraceLogic<Character> buildLogic(VerificationOptions verificationOptions) {
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

        ITraceVerificationLogic verificationLogic = new VerificationLogic(
                verificationOptions,
                fixedDistanceSetter
        );

        ITraceTargetChanger<Character> targetChanger = fileProvider::setCharForFilename;
        return new UnicodeTraceLineLogic(verificationLogic, convertingPolygonProvider, targetChanger);
    }

    @Override
    protected void nextLearningView() {
        if(difficulty < EASY_CAP){
            lowDifficulty();
        } else if(difficulty < MID_CAP){
            midDifficulty();
        } else {
            highDifficulty();
        }

        this.currentCounter++;
    }

    private void highDifficulty() {
        if(currentCounter == 0){
            kanjiSelectExercise();
        } else if(currentCounter == 1){
            tracingExercise(TraceMode.ALL_HINTS);
        } else if(currentCounter <= 4){
            int rand = random.nextInt(8 + builderChanceIncrease);
            if((rand == 7 || rand == 6 || rand == 8) && wordBuilder){
                rand = 1;
            }
            switch (rand){
                case 0,7,8 -> wordBuilderExercise();
                case 1,2,3 -> tracingExercise(TraceMode.NEXT_HINT);
                case 4,5,6 -> matchExercise();
            }
        } else if(currentCounter <= 6){
            tracingExercise(TraceMode.NO_HINTS);
        } else {
            learnSessionFinished();
        }
    }

    private void midDifficulty(){
        if(currentCounter == 0){
            kanjiSelectExercise();
        } else if(currentCounter == 1){
            tracingExercise(TraceMode.ALL_HINTS);
        } else if(currentCounter <= 5){
            int rand = random.nextInt(6 + builderChanceIncrease);
            if((rand == 0 || rand == 6) && wordBuilder){
                rand = 1;
            }
            switch (rand){
                case 0,6 -> wordBuilderExercise();
                case 1,2 -> tracingExercise(TraceMode.NEXT_HINT);
                case 3,4,5 -> matchExercise();
            }
        } else if(currentCounter <= 7){
            tracingExercise(TraceMode.NO_HINTS);
        } else {
            learnSessionFinished();
        }
    }

    private void lowDifficulty(){
        if(currentCounter == 0){
            kanjiSelectExercise();
        } else if(currentCounter <= 2){
            tracingExercise(TraceMode.ALL_HINTS);
        } else if(currentCounter <= 6){
            int rand = random.nextInt(6 + builderChanceIncrease);
            if((rand == 0 || rand == 6) && wordBuilder){
                rand = 1;
            }
            switch (rand){
                case 0,6 -> wordBuilderExercise();
                case 1,2,3 -> tracingExercise(TraceMode.NEXT_HINT);
                case 4,5 -> matchExercise();
            }
        } else if(currentCounter <= 8){
            tracingExercise(TraceMode.NO_HINTS);
        } else {
            learnSessionFinished();
        }
    }

    private void matchExercise() {
        Stack<Word> allWords = new Stack<>();
        allWords.addAll(kanji.getWords());
        Collections.shuffle(allWords);
        List<Word> words = new ArrayList<>(5);

        int cntr = 0;
        int rnd = random.nextInt(1, 6);
        for ( ; cntr<rnd && cntr<kanji.getWords().size(); cntr++){
            words.add(allWords.pop());
        }

        words.addAll(getRandomWords(5-cntr));
        if(random.nextInt(4) == 0){
            currentLearnView = new VocabAudioToJapaneseMatch(this, words);
        } else {
            JapaneseToKanaMatch matchLearnView = new JapaneseToKanaMatch(this, words);
            if(random.nextBoolean()){
                matchLearnView.setReverse(true);
            }
            currentLearnView = matchLearnView;
        }

        bp.setCenter(currentLearnView.initView());
    }

    private void wordBuilderExercise() {
        Word word = kanji.getWords().get(random.nextInt(kanji.getWords().size()));
        currentLearnView = new WordBuilderView(this, word);
        bp.setCenter(currentLearnView.initView());
        wordBuilder = true;
    }

    private void tracingExercise(TraceMode allHints) {
        traceLogic.startTracing(allHints);
        currentLearnView = kanjiTracerLearnView;
        bp.setCenter(kanjiTracerLearnView.getPane());
    }

    private void kanjiSelectExercise() {
        Word word = kanji.getWords().get(random.nextInt(kanji.getWords().size()));
        currentLearnView = new WordKanjiSelectLearnView(this, word);
        bp.setCenter(currentLearnView.initView());
    }


    @Override
    protected void updateProgresses(int percent) {
        Controller.getInstance().addKanjiProgress(kanji, percent);
    }

    private List<Word> getRandomWords(int count) {
        return Controller.getInstance().getRandomWordsFromKanjiTrainer(count);
    }
}
