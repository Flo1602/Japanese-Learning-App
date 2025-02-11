package at.primetshofer.view.catalog;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
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
