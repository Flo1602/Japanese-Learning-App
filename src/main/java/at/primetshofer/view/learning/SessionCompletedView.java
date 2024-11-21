package at.primetshofer.view.learning;

import at.primetshofer.model.util.LangController;
import at.primetshofer.view.catalog.View;
import javafx.animation.*;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.Duration;

public class SessionCompletedView extends View {

    protected Duration duration;
    private int percent;
    private SequentialTransition animation;

    public SessionCompletedView(Scene scene, Duration duration, int percent) {
        this.scene = scene;
        origin = new SimpleObjectProperty<>();
        this.duration = duration;
        this.percent = percent;
        initView();
    }

    @Override
    protected void initView() {
        bp = new BorderPane();

        Label headline = new Label(LangController.getText("SessionCompletedHeadline"));
        headline.getStyleClass().add("headline");
        headline.setTextFill(Color.rgb(63, 255, 56));
        BorderPane.setAlignment(headline, Pos.CENTER);

        Label percentLabel = new Label();
        percentLabel.getStyleClass().add("sessionCompleteText");

        long seconds = duration.getSeconds();
        long minutes = seconds/60;
        seconds = seconds%60;

        Label durationLabel = new Label();
        durationLabel.getStyleClass().add("sessionCompleteText");

        Button finishButton = new Button(LangController.getText("FinishButton"));
        finishButton.getStyleClass().add("menuButton");
        finishButton.setOnAction(e -> origin.get().popToView());

        HBox buttons = new HBox(finishButton);
        buttons.setPadding(new Insets(0, 0, 40, 0));
        buttons.setAlignment(Pos.CENTER);

        VBox center = new VBox(percentLabel, durationLabel);
        center.setAlignment(Pos.CENTER);
        center.setSpacing(30);

        bp.setTop(headline);
        bp.setCenter(center);
        bp.setBottom(buttons);

        applyAnimations(headline, percentLabel, durationLabel, finishButton, percent, minutes, seconds);
    }

    private void applyAnimations(
            Label headline, Label percentLabel, Label durationLabel, Button finishButton,
            int percent, long minutes, long seconds) {
        // Fade-in animation for headline
        FadeTransition fadeInHeadline = new FadeTransition(javafx.util.Duration.seconds(0.3), headline);
        fadeInHeadline.setFromValue(0);
        fadeInHeadline.setToValue(1);

        // Scale animation for percentage label
        ScaleTransition scalePercent = new ScaleTransition(javafx.util.Duration.seconds(0.7), percentLabel);
        scalePercent.setFromX(0.2);
        scalePercent.setFromY(0.2);
        scalePercent.setToX(1);
        scalePercent.setToY(1);

        // Scale animation for duration label
        ScaleTransition scaleDuration = new ScaleTransition(javafx.util.Duration.seconds(0.7), durationLabel);
        scaleDuration.setFromX(0.3);
        scaleDuration.setFromY(0.3);
        scaleDuration.setToX(1);
        scaleDuration.setToY(1);

        // Button animation
        FadeTransition fadeInButton = new FadeTransition(javafx.util.Duration.seconds(0.3), finishButton);
        fadeInButton.setFromValue(0);
        fadeInButton.setToValue(1);

        // Create the counting animation for percent label
        Timeline percentCounter = new Timeline();
        percentCounter.getKeyFrames().add(new KeyFrame(
                javafx.util.Duration.millis(700), // Total duration
                new KeyValue(new IntegerPropertyBase(0) {
                    @Override
                    protected void invalidated() {
                        String percentText = get() + "";
                        if(get() < 10){
                            percentText = "0" + percentText;
                        }
                        if(get() < 100){
                            percentText = "0" + percentText;
                        }
                        percentLabel.setText(LangController.getText("PercentLabel") + ": " + percentText + "%");
                    }

                    @Override
                    public Object getBean() {
                        return null;
                    }

                    @Override
                    public String getName() {
                        return null;
                    }
                }, percent)
        ));

        // Create the counting animation for duration label
        Timeline durationCounter = new Timeline();
        durationCounter.getKeyFrames().add(new KeyFrame(
                javafx.util.Duration.millis(700), // Total duration
                new KeyValue(new IntegerPropertyBase(0) {
                    @Override
                    protected void invalidated() {
                        long currentMinutes = get() / 60;
                        long currentSeconds = get() % 60;
                        String seconds = currentSeconds + "";
                        String minutes = currentSeconds + "";
                        if(currentSeconds < 10){
                            seconds = "0" + currentSeconds;
                        }
                        if(currentMinutes < 10){
                            minutes = "0" + currentMinutes;
                        }
                        durationLabel.setText(LangController.getText("DurationLabel") + ": " + minutes + ":" + seconds);
                    }

                    @Override
                    public Object getBean() {
                        return null;
                    }

                    @Override
                    public String getName() {
                        return null;
                    }
                }, (int) (minutes * 60 + seconds))
        ));

        // Play animations sequentially
        animation = new SequentialTransition(
                fadeInHeadline,
                new ParallelTransition(scalePercent, percentCounter),
                new ParallelTransition(scaleDuration, durationCounter),
                fadeInButton
        );
    }

    @Override
    public void display(View origin) {
        super.display(origin);
        animation.play();
    }
}
