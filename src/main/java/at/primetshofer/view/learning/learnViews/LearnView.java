package at.primetshofer.view.learning.learnViews;

import at.primetshofer.view.learning.learnSessionManagers.LearnSessionManager;
import javafx.scene.layout.Pane;

public abstract class LearnView {

    private final LearnSessionManager learnSessionManager;

    public LearnView (LearnSessionManager learnSessionManager, boolean checkButtonVisible) {
        this.learnSessionManager = learnSessionManager;
        if (checkButtonVisible) {
            learnSessionManager.changeContinueToCheck();
        }
    }

    public abstract Pane initView();

    protected void finished(boolean success) {
        learnSessionManager.learnViewFinished(success);
    }

    protected void finished(boolean success, String message) {
        learnSessionManager.learnViewFinished(success, message);
    }

    public abstract void checkComplete();

    protected void activateCheckButton(boolean active) {
        learnSessionManager.activateCheckButton(active);
    }

    protected void setDisableOverwrite(boolean disable) {
        learnSessionManager.setDisableOverwrite(disable);
    }

    public void correctnessOverwritten(){

    }

    public abstract Pane resetView();

    protected void setCheckButtonVisible(boolean visible) {
        if (visible) {
            learnSessionManager.changeContinueToCheck();
        }
    }
}
