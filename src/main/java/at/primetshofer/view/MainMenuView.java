package at.primetshofer.view;

import at.primetshofer.model.Controller;
import at.primetshofer.model.entities.Word;
import at.primetshofer.model.util.DiscordActivityUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.catalog.*;
import at.primetshofer.view.learning.menu.LearningMenuView;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;

import java.util.List;

public class MainMenuView extends View {

    private static final Logger logger = Logger.getLogger(MainMenuView.class);
    private SettingsView settingsView;
    private CatalogView catalogView;
    private LearningMenuView learningMenuView;
    private Button warning;

    public MainMenuView(Scene scene) {
        super(scene);
    }

    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("MainMenuHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        Button learn = new Button(LangController.getText("LearnButton"));
        learn.getStyleClass().add("menuButton");
        learn.setOnAction(e -> startLearning());

        Button catalog = new Button(LangController.getText("CatalogButton"));
        catalog.getStyleClass().add("menuButton");
        catalog.setOnAction(e -> {
            if (catalogView == null) {
                catalogView = new CatalogView(scene);
            }
            catalogView.display(this);
        });
        Button exit = new Button(LangController.getText("ExitButton"));
        exit.getStyleClass().add("menuButton");
        exit.setOnAction(e -> {
            logger.info("Closing application");
            System.exit(0);
        });

        VBox vb = new VBox();
        vb.getStyleClass().add("menuVBox");
        vb.getChildren().addAll(learn, catalog, exit);
        BorderPane.setAlignment(vb, Pos.CENTER);

        Image settingsImage = new Image("settings.png");

        ImageView settingsImageView = new ImageView(settingsImage);
        settingsImageView.setFitHeight(50);
        settingsImageView.setFitWidth(50);

        Button settings = new Button();
        settings.getStyleClass().add("settingsButton");
        settings.setGraphic(settingsImageView);
        settings.setOnAction(e -> {
            if (settingsView == null) {
                settingsView = new SettingsView(scene);
            }
            settingsView.display(this);
        });

        HBox hb = new HBox(settings);
        hb.getStyleClass().add("container");
        hb.setAlignment(Pos.BOTTOM_CENTER);

        Image warningImage = new Image("warning.png");

        ImageView warningView = new ImageView(warningImage);
        warningView.setFitHeight(50);
        warningView.setFitWidth(50);

        warning = new Button();
        warning.getStyleClass().add("settingsButton");
        warning.setGraphic(warningView);

        HBox hbWarning = new HBox(warning);
        hbWarning.getStyleClass().add("container");
        hbWarning.setAlignment(Pos.BOTTOM_CENTER);

        bp.setTop(headline);
        bp.setCenter(vb);
        bp.setRight(hb);

        hb.widthProperty().addListener((observableValue, oldValue, newValue) -> hbWarning.setPrefWidth(newValue.doubleValue()));

        bp.setLeft(hbWarning);
    }

    private void startLearning() {
        if (learningMenuView == null) {
            learningMenuView = new LearningMenuView(scene);
        }

        learningMenuView.display(this);
    }

    @Override
    public void display(View origin) {
        super.display(origin);

        DiscordActivityUtil.setActivityDetails("Main Menu");

        Controller controller = Controller.getInstance();
        List<Word> wordsWithoutSentences = controller.getWordsWithoutSentences();
        List<Word> wordsWithoutQuestions = controller.getWordsWithoutQuestions();

        if (!wordsWithoutSentences.isEmpty()) {
            warning.setOnAction(e -> {
                ImportSentencesView importSentencesView = new ImportSentencesView(scene, wordsWithoutSentences);
                importSentencesView.display(this);
            });
            warning.setVisible(true);
        } else if (!wordsWithoutQuestions.isEmpty()) {
            warning.setOnAction(e -> {
                ImportQuestionsView importQuestionsView = new ImportQuestionsView(scene, wordsWithoutQuestions);
                importQuestionsView.display(this);
            });
            warning.setVisible(true);
        } else {
            warning.setOnAction(e -> {
            });
            warning.setVisible(false);
        }
    }
}
