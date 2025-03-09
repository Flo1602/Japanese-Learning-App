package at.primetshofer.view.learning.menu;

import at.primetshofer.model.AudioRecorder;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.catalog.CreateEditWordWindow;
import at.primetshofer.view.catalog.View;
import at.primetshofer.view.learning.learnSessionManagers.QuestionSessionManager;
import at.primetshofer.view.learning.learnSessionManagers.SentenceSessionManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;

public class LearningMenuView extends View {

    private static final Logger logger = Logger.getLogger(LearningMenuView.class);

    private KanjiMenuLearningView kanjiMenuLearningView;
    private WordMenuLearningView wordMenuLearningView;

    public LearningMenuView(Scene scene) {
        super(scene);
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("LearningHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);
        // TODO use lang
        Button questionButton = new Button("Questions");
        questionButton.getStyleClass().add("smallMenuButton");
        questionButton.setOnAction(e -> {
            //QuestionSessionManager questionSessionManager = new QuestionSessionManager(scene);
            //questionSessionManager.initView();
            //questionSessionManager.display(this);
            logger.warn(QuestionSessionManager.class.getName() + " is not fully implemented yet");
            // TODO: use lang
            ViewUtils.showAlert(Alert.AlertType.WARNING, "This learning-mode is not fully implemented yet!", "NOT AVAILABLE");
        });
        // TODO: use lang
        Button sentenceButton = new Button("Sentences");
        sentenceButton.getStyleClass().add("smallMenuButton");
        sentenceButton.setOnAction(e -> {
            //SentenceSessionManager sentenceSessionManager = new SentenceSessionManager(scene);
            //sentenceSessionManager.initView();
            //sentenceSessionManager.display(this);
            logger.warn(SentenceSessionManager.class.getName() + " is not fully implemented yet");
            // TODO: use lang
            ViewUtils.showAlert(Alert.AlertType.WARNING, "This learning-mode is not fully implemented yet!", "NOT AVAILABLE");
        });

        Button vocabButton = new Button(LangController.getText("VocabButton"));
        vocabButton.getStyleClass().add("smallMenuButton");
        vocabButton.setOnAction(e -> {
            if (wordMenuLearningView == null) {
                wordMenuLearningView = new WordMenuLearningView(scene);
                wordMenuLearningView.initView();
            }

            wordMenuLearningView.display(this);
        });

        Button kanjiButton = new Button(LangController.getText("KanjiButton"));
        kanjiButton.getStyleClass().add("smallMenuButton");
        kanjiButton.setOnAction(e -> {
            if (kanjiMenuLearningView == null) {
                kanjiMenuLearningView = new KanjiMenuLearningView(scene);
                kanjiMenuLearningView.initView();
            }

            kanjiMenuLearningView.display(this);
        });

        VBox vb = new VBox();
        vb.getStyleClass().add("menuVBox");
        vb.getChildren().addAll(questionButton, sentenceButton, vocabButton, kanjiButton);
        BorderPane.setAlignment(vb, Pos.CENTER);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        HBox bottom = new HBox();
        bottom.setSpacing(20);
        bottom.setAlignment(Pos.CENTER);

        Image mobileImage = new Image("tablet.png");

        ImageView mobileImageView = new ImageView(mobileImage);
        mobileImageView.setFitHeight(50);
        mobileImageView.setFitWidth(50);

        bp.setTop(headline);
        bp.setLeft(hb);
        bp.setCenter(vb);
        bp.setBottom(bottom);

        Region spacer = new Region();

        hb.widthProperty().addListener((observableValue, oldValue, newValue) -> spacer.setPrefWidth(newValue.doubleValue()));

        bp.setRight(spacer);
    }

    @Override
    public void display(View origin) {
        super.display(origin);

        AudioRecorder.stopRecordingThread();
    }
}
