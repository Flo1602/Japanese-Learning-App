package at.primetshofer.view.catalog;

import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class CatalogView extends View {

    public CatalogView(Scene scene) {
        super(scene);
    }

    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("CatalogHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        Button wordsButton = new Button(LangController.getText("WordsButton"));
        wordsButton.getStyleClass().add("menuButton");
        wordsButton.setOnAction(e -> {
            WordListView wordListView = new WordListView(scene);
            wordListView.display(this);
        });

        Button sentencesButton = new Button(LangController.getText("SentencesButton"));
        sentencesButton.getStyleClass().add("menuButton");
        sentencesButton.setOnAction(e -> {
            SentenceListView sentenceListView = new SentenceListView(scene);
            sentenceListView.display(this);
        });

        Button kanjiButton = new Button(LangController.getText("KanjiButton"));
        kanjiButton.getStyleClass().add("menuButton");
        kanjiButton.setOnAction(e -> {
            KanjiListView kanjiListView = new KanjiListView(scene);
            kanjiListView.display(this);
        });

        Button questionButton = new Button(LangController.getText("QuestionButton"));
        questionButton.getStyleClass().add("menuButton");
        questionButton.setOnAction(e -> {
            QuestionListView questionListView = new QuestionListView(scene);
            questionListView.display(this);
        });

        VBox vb = new VBox();
        vb.getStyleClass().add("menuVBox");
        vb.getChildren().addAll(wordsButton, kanjiButton, sentencesButton, questionButton);
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
