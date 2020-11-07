package org.janelia.horta;

import java.util.List;
import org.janelia.workstation.controller.model.TmViewState;

/**
 * @author schauderd
 * convenience class storing the current playlist and progess through the playlist, so that
 * it's easy to pause, play reverse, and resume an exited state.
 */


public class PlayState {
    private List<TmViewState> playList;
    private int currentNode;
    private int currentStep;
    private int fps;

    /**
     * @return the playList
     */
    public List<TmViewState> getPlayList() {
        return playList;
    }

    /**
     * @param playList the playList to set
     */
    public void setPlayList(List<TmViewState> playList) {
        this.playList = playList;
    }

    
    /**
     * @return the currentNode
     */
    public int getCurrentNode() {
        return currentNode;
    }

    /**
     * @param currentNode the currentNode to set
     */
    public void setCurrentNode(int currentNode) {
        this.currentNode = currentNode;
    }
    
    /**
     * @return the currentStep
     */
    public int getCurrentStep() {
        return currentStep;
    }

    /**
     * @param currentStep the currentStep to set
     */
    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    /**
     * @return the fps
     */
    public int getFps() {
        return fps;
    }

    /**
     * @param fps the fps to set
     */
    public void setFps(int fps) {
        this.fps = fps;
    }
    
}
