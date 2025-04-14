package at.primetshofer.services;

import at.primetshofer.model.Controller;
import at.primetshofer.view.MainMenuView;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;

public class LoadLearningDataService extends Service<Void> {

    private static final Logger logger = Logger.getLogger(MainMenuView.class);

    @Override
    protected void succeeded() {
        super.succeeded();
        logger.info("Loading Learning Data Finished");
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                logger.info("Loading Learning Data Started");

                Controller.getInstance().updateLists();

                return null;
            }
        };
    }
}
