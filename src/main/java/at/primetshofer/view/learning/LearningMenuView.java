package at.primetshofer.view.learning;

import at.primetshofer.model.util.LangController;
import at.primetshofer.view.catalog.View;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.learning.learnSessionManagers.*;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class LearningMenuView extends View {

    public LearningMenuView(Scene scene) {
        super(scene);
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("LearningHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        Button wordsButton = new Button(LangController.getText("WordsButton"));
        wordsButton.getStyleClass().add("menuButton");
        wordsButton.setOnAction(e -> {
            WordSessionManager wordSessionManager = new WordSessionManager(scene);
            wordSessionManager.initView();
            wordSessionManager.display(this);
        });

        Button questionButton = new Button("Questions");
        questionButton.getStyleClass().add("menuButton");
        questionButton.setOnAction(e -> {
            QuestionSessionManager questionSessionManager = new QuestionSessionManager(scene);
            questionSessionManager.initView();
            questionSessionManager.display(this);
        });

        Button sentenceButton = new Button("Sentences");
        sentenceButton.getStyleClass().add("menuButton");
        sentenceButton.setOnAction(e -> {
            SentenceSessionManager sentenceSessionManager = new SentenceSessionManager(scene);
            sentenceSessionManager.initView();
            sentenceSessionManager.display(this);
        });

        Button speakingButton = new Button("Speaking");
        speakingButton.getStyleClass().add("menuButton");
        speakingButton.setOnAction(e -> {
            SpeakingSessionManager speakingSessionManager = new SpeakingSessionManager(scene);
            speakingSessionManager.initView();
            speakingSessionManager.display(this);
        });

        Button testButton = new Button("Test");
        testButton.getStyleClass().add("menuButton");
        testButton.setOnAction(e -> {
            KanjiSessionManager kanjiSessionManager = new KanjiSessionManager(scene);
            kanjiSessionManager.initView();
            kanjiSessionManager.display(this);
        });

        VBox vb = new VBox();
        vb.getStyleClass().add("menuVBox");
        vb.getChildren().addAll(wordsButton, questionButton, sentenceButton, speakingButton, testButton);
        BorderPane.setAlignment(vb, Pos.CENTER);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        bp.setTop(headline);
        bp.setLeft(hb);
        bp.setCenter(vb);

        Region spacer = new Region();

        hb.widthProperty().addListener((observableValue, oldValue, newValue) -> spacer.setPrefWidth(newValue.doubleValue()));

        bp.setRight(spacer);
    }
}
