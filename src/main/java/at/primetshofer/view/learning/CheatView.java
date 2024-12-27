package at.primetshofer.view.learning;

import at.primetshofer.model.Controller;
import at.primetshofer.model.entities.Kanji;
import at.primetshofer.model.entities.KanjiProgress;
import at.primetshofer.model.util.HibernateUtil;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.catalog.View;
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

import java.time.LocalDate;

public class CheatView extends View {

    public CheatView(Scene scene) {
        super(scene);
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("CheatHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        HBox center = new HBox();
        center.setAlignment(Pos.CENTER);
        center.setSpacing(30);

        TextField kanji = new TextField();
        kanji.setPromptText(LangController.getText("KanjiButton"));
        kanji.getStyleClass().add("normalText");

        Button addProgress = new Button(LangController.getText("AddProgress"));
        addProgress.getStyleClass().add("normalButton");
        addProgress.setOnAction(e -> {
            String symbol = kanji.getText();
            Kanji kanji1 = null;

            try {
                String jpql = "SELECT k FROM Kanji k WHERE k.symbol = :symbol";
                kanji1 = HibernateUtil.getEntityManager()
                        .createQuery(jpql, Kanji.class)
                        .setParameter("symbol", symbol)
                        .getSingleResult();
            } catch (NoResultException ex) {
                ViewUtils.showAlert(Alert.AlertType.WARNING, "No Kanji found for symbol: " + symbol, "Unable to add progress!");
            }

            if (kanji1 != null) {
                kanji1 = Controller.getInstance().addKanjiProgress(kanji1, 100);
                displayKanjiProgressInfo(kanji1);
            }
        });

        center.getChildren().addAll(kanji, addProgress);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        bp.setTop(headline);
        bp.setLeft(hb);
        bp.setCenter(center);

        Region spacer = new Region();

        hb.widthProperty().addListener((observableValue, oldValue, newValue) -> spacer.setPrefWidth(newValue.doubleValue()));

        bp.setRight(spacer);
    }

    private void displayKanjiProgressInfo(Kanji kanji) {
        Label pointsLabel = new Label(LangController.getText("KanjiPointsLabel") + " " + getTodaysPoints(kanji));
        pointsLabel.getStyleClass().add("normalText");

        BorderPane.setAlignment(pointsLabel, Pos.CENTER);

        bp.setBottom(pointsLabel);
    }

    private int getTodaysPoints(Kanji kanji) {
        LocalDate today = LocalDate.now();

        for (KanjiProgress progress : kanji.getProgresses()) {
            if(progress.getLearned().toLocalDate().equals(today)) {
                return progress.getPoints();
            }
        }

        return 0;
    }
}
