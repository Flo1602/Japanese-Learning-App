package at.primetshofer.view.catalog;

import at.primetshofer.model.util.LangController;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;

public class LoadingView extends View {

    private DoubleProperty progressProperty;

    public LoadingView(Scene scene) {
        super(scene);
    }

    @Override
    protected void initView() {
        this.progressProperty = new SimpleDoubleProperty();
        bp = new BorderPane();

        Label title = new Label(LangController.getText("LoadingLabel"));
        title.getStyleClass().add("headline");

        ProgressBar progress = new ProgressBar();
        progress.progressProperty().bind(progressProperty);
        progress.setPrefWidth(500);

        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setAlignment(progress, Pos.CENTER);

        bp.setCenter(progress);
        bp.setTop(title);
    }

    public void bindProgress(DoubleProperty progressProperty) {
        this.progressProperty.bind(progressProperty);
    }
}
