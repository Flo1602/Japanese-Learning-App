package at.primetshofer.view.catalog;

import at.primetshofer.model.Controller;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;

public abstract class View {

    protected Scene scene;
    protected BorderPane bp;
    protected ObjectProperty<View> origin;

    protected View() {
    }

    public View(Scene scene) {
        this.scene = scene;
        origin = new SimpleObjectProperty<>();

        scene.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ESCAPE) {
                Controller.getInstance().stopAudio();
            }
        });

        initView();
    }

    protected abstract void initView();

    public void display(View origin) {
        if (origin != null) {
            this.origin.set(origin);
        }
        scene.setRoot(bp);
    }

    public void popToView() {
        display(null);
    }
}
