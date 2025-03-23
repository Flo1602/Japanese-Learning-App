package at.primetshofer.view.learning.menu;

import at.primetshofer.model.StatsManager;
import at.primetshofer.model.entities.LearnTimeStats;
import at.primetshofer.model.util.LangController;
import at.primetshofer.view.ViewUtils;
import at.primetshofer.view.catalog.View;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.apache.log4j.Logger;

import java.time.Duration;
import java.util.List;

public class StatsView extends View {

    private static final Logger logger = Logger.getLogger(KanjiCheatView.class);

    public StatsView(Scene scene) {
        super(scene);
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("StatsHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        HBox hb = ViewUtils.getBackButtonBox(origin);

        bp.setTop(headline);
        bp.setLeft(hb);

        Region spacer = new Region();

        hb.widthProperty().addListener((observableValue, oldValue, newValue) -> spacer.setPrefWidth(newValue.doubleValue()));

        bp.setRight(spacer);

        new Thread(this::loadStats).start();
    }

    private void loadStats() {
        long totalKanji = StatsManager.getTotalKanjiCount();
        long totalWord = StatsManager.getTotalWordCount();
        LearnTimeStats today = StatsManager.getTodayStats();
        List<LearnTimeStats> allStats = StatsManager.getAllTimeStats();

        if(today == null){
            today = new LearnTimeStats();
            today.setDuration(Duration.ZERO);
            today.setExercisesCount(0);
        }

        VBox statsBox = new VBox();
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setSpacing(25);

        statsBox.getChildren().addAll(getTotalKanjiBox(totalKanji), getTotalWordBox(totalWord), getTodayTime(today), getAllTime(allStats), getTodayExercises(today), getAllExercises(allStats));

        Platform.runLater(() -> bp.setCenter(statsBox));
    }

    private HBox getAllExercises(List<LearnTimeStats> allStats) {
        int totalExercises = 0;
        for (LearnTimeStats stat : allStats) {
            totalExercises += stat.getExercisesCount();
        }

        HBox hb = new HBox();
        hb.getStyleClass().add("statsHB");

        Label desc = new Label(LangController.getText("TotalExercises"));
        desc.getStyleClass().add("normalText");
        Label amount = new Label(totalExercises + "");
        amount.getStyleClass().add("normalText");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hb.getChildren().addAll(desc, spacer, amount);

        return hb;
    }

    private HBox getTodayExercises(LearnTimeStats today) {
        HBox hb = new HBox();
        hb.getStyleClass().add("statsHB");

        Label desc = new Label(LangController.getText("TodayExercises"));
        desc.getStyleClass().add("normalText");
        Label amount = new Label(today.getExercisesCount() + "");
        amount.getStyleClass().add("normalText");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hb.getChildren().addAll(desc, spacer, amount);

        return hb;
    }

    private HBox getAllTime(List<LearnTimeStats> allStats) {
        Duration totalDuration = Duration.ZERO;
        for (LearnTimeStats stat : allStats) {
            totalDuration = totalDuration.plus(stat.getDuration());
        }

        HBox hb = new HBox();
        hb.getStyleClass().add("statsHB");

        Label desc = new Label(LangController.getText("TotalLearnTime"));
        desc.getStyleClass().add("normalText");
        Label amount = new Label(formatDuration(totalDuration));
        amount.getStyleClass().add("normalText");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hb.getChildren().addAll(desc, spacer, amount);

        return hb;
    }

    private HBox getTodayTime(LearnTimeStats today) {
        HBox hb = new HBox();
        hb.getStyleClass().add("statsHB");

        Label desc = new Label(LangController.getText("TodayLearnTime"));
        desc.getStyleClass().add("normalText");
        Label amount = new Label(formatDuration(today.getDuration()));
        amount.getStyleClass().add("normalText");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hb.getChildren().addAll(desc, spacer, amount);

        return hb;
    }

    private HBox getTotalWordBox(long totalWord) {
        HBox hb = new HBox();
        hb.getStyleClass().add("statsHB");

        Label desc = new Label(LangController.getText("TotalWords"));
        desc.getStyleClass().add("normalText");
        Label amount = new Label(totalWord + "");
        amount.getStyleClass().add("normalText");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hb.getChildren().addAll(desc, spacer, amount);

        return hb;
    }

    private HBox getTotalKanjiBox(long totalKanji) {
        HBox hb = new HBox();
        hb.getStyleClass().add("statsHB");

        Label desc = new Label(LangController.getText("TotalKanji"));
        desc.getStyleClass().add("normalText");
        Label amount = new Label(totalKanji + "");
        amount.getStyleClass().add("normalText");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        hb.getChildren().addAll(desc, spacer, amount);

        return hb;
    }

    private static String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
}
