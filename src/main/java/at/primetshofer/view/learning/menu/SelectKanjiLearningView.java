package at.primetshofer.view.learning.menu;

import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.catalog.CreateEditWordWindow;
import at.primetshofer.view.catalog.View;
import at.primetshofer.view.learning.learnSessionManagers.KanjiSessionManager;
import jakarta.persistence.NoResultException;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.apache.log4j.Logger;

public class SelectKanjiLearningView extends View {

    private static final Logger logger = Logger.getLogger(SelectKanjiLearningView.class);

    private final KanjiSessionManager kanjiSessionManager;

    public SelectKanjiLearningView(Scene scene, KanjiSessionManager kanjiSessionManager) {
        super(scene);
        this.kanjiSessionManager = kanjiSessionManager;
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("SelectKanji"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        HBox center = new HBox();
        center.setAlignment(Pos.CENTER);
        center.setSpacing(30);

        TextField kanji = new TextField();
        kanji.setPromptText(LangController.getText("KanjiButton"));
        kanji.getStyleClass().add("normalText");

        Button learnButton = new Button(LangController.getText("LearnButton"));
        learnButton.getStyleClass().add("normalButton");
        learnButton.setOnAction(e -> {
            Kanji selectedKanji = getKanji(kanji);

            if (selectedKanji != null) {
                kanjiSessionManager.setSpecificKanji(selectedKanji);
                kanjiSessionManager.initSessionManager();
                kanjiSessionManager.initView();
                kanjiSessionManager.display(this);
            }
        });

        center.getChildren().addAll(kanji, learnButton);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        bp.setTop(headline);
        bp.setLeft(hb);
        bp.setCenter(center);

        Region spacer = new Region();

        hb.widthProperty().addListener((observableValue, oldValue, newValue) -> spacer.setPrefWidth(newValue.doubleValue()));

        bp.setRight(spacer);
    }

    private Kanji getKanji(TextField kanji) {
        String symbol = kanji.getText();
        Kanji kanji1 = null;

        try {
            String jpql = "SELECT k FROM Kanji k WHERE k.symbol = :symbol";
            kanji1 = HibernateUtil.getEntityManager()
                    .createQuery(jpql, Kanji.class)
                    .setParameter("symbol", symbol)
                    .getSingleResult();
        } catch (NoResultException ex) {
            logger.error("No Kanji found for symbol: " + symbol, ex);
            ViewUtils.showAlert(Alert.AlertType.WARNING,
                    LangController.getText("NoWordsForSymbolError") + " " + symbol,
                    LangController.getText("UnableToAddProgressError"));
        }

        return kanji1;
    }
}
