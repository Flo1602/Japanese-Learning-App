package at.primetshofer.view.catalog;

import at.primetshofer.Main;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public abstract class PopUp {

    protected BorderPane bp;
    private Stage stage;
    private Scene scene;

    public PopUp() {
        bp = new BorderPane();
        stage = new Stage();
        scene = new Scene(bp);
        scene.getStylesheets().add("myStyle.css");

        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(Main.primaryStage);
        stage.getIcons().add(new Image("icon.png"));
        stage.setScene(scene);
        stage.setWidth(500);
        stage.setHeight(500);

        initView();
    }

    protected abstract void initView();

    public void showAndWait(){
        stage.showAndWait();
    }

    public void close(){
        stage.close();
    }

    protected void setTitle(String title){
        stage.setTitle(title);
    }

    protected void setStageSize(double width, double height){
        stage.setWidth(width);
        stage.setHeight(height);
    }

    public void setStageOwner(Stage stage){
        this.stage.initOwner(stage);
    }

    protected Stage getStage(){
        return stage;
    }
}
