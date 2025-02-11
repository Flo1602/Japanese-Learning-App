package at.primetshofer.view.learning.menu;

import at.primetshofer.model.util.LangController;
import at.primetshofer.services.NetworkLearningService;
import at.primetshofer.view.catalog.View;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class NetworkLearningView extends View {

    private static NetworkLearningService networkLearningService;

    private BooleanProperty deviceConnected;

    public NetworkLearningView(Scene scene) {
        super(scene);
    }

    @Override
    protected void initView() {
        bp = new BorderPane();
        deviceConnected = new SimpleBooleanProperty(false);

        Label headline = new Label(LangController.getText("MobileLearningActiveHeading"));
        headline.getStyleClass().add("headline");
        BorderPane.setAlignment(headline, Pos.CENTER);

        VBox center = new VBox();
        center.setAlignment(Pos.CENTER);
        center.setSpacing(50);

        Label deviceConnectedLabel = new Label(LangController.getText("DeviceConnected") + " " + deviceConnected.get());
        deviceConnectedLabel.getStyleClass().add("normalText");
        deviceConnected.addListener((observableValue, oldValue, newValue) -> {
            Platform.runLater(() -> deviceConnectedLabel.setText(LangController.getText("DeviceConnected") + " " + deviceConnected.get()));
        });

        Button stop = new Button(LangController.getText("StopMobileLearning"));
        stop.getStyleClass().add("menuButton");
        stop.setOnAction(e -> {
            networkLearningService.cancel();
            deviceConnected.set(false);
            origin.get().popToView();
        });

        center.getChildren().addAll(deviceConnectedLabel, stop);

        bp.setTop(headline);
        bp.setCenter(center);
    }

    @Override
    public void display(View origin) {
        super.display(origin);
        startNetworkLearning();
    }

    private void startNetworkLearning() {
        if (networkLearningService == null) {
            networkLearningService = new NetworkLearningService(deviceConnected);
        }

        if (networkLearningService.getState() != Worker.State.READY) {
            networkLearningService.reset();
        }

        networkLearningService.start();
    }
}
