package at.primetshofer.services;

import at.primetshofer.model.Controller;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class LoadLearningDataService extends Service<Void> {
    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                Controller.getInstance().updateLists();
                return null;
            }
        };
    }
}
