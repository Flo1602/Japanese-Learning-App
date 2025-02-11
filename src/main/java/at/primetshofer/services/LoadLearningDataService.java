package at.primetshofer.services;

import at.primetshofer.model.Controller;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class LoadLearningDataService extends Service<Void> {

    private static boolean startup = true;

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws InterruptedException {
                if(startup) {
                    Thread.sleep(200);
                    startup = false;
                }

                Controller.getInstance().updateLists();

                return null;
            }
        };
    }
}
