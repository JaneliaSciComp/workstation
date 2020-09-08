package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.model.TmViewState;
import java.util.ArrayList;
import java.util.List;

public class AnimationEvent {
    List<TmViewState> animationSteps = new ArrayList<>();
    boolean autoRotation;
    int speed;
    int stepScale;

    public List<TmViewState> getAnimationSteps() {
        return animationSteps;
    }

    public void setAnimationSteps(List<TmViewState> animationSteps) {
        this.animationSteps = animationSteps;
    }

    public boolean isAutoRotation() {
        return autoRotation;
    }

    public void setAutoRotation(boolean autoRotation) {
        this.autoRotation = autoRotation;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getStepScale() {
        return stepScale;
    }

    public void setStepScale(int stepScale) {
        this.stepScale = stepScale;
    }

}

