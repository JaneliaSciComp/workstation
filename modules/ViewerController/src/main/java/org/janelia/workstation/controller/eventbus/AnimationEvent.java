package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.model.TmViewState;
import java.util.ArrayList;
import java.util.List;

public class AnimationEvent {

    List<TmViewState> animationSteps = new ArrayList<>();
    boolean autoRotation;
    int speed;
    int stepScale;

    public AnimationEvent(List<TmViewState> steps, boolean autoRotation, int speed, int stepScale) {
        this.animationSteps = steps;
        this.autoRotation = autoRotation;
        this.speed = speed;
        this.stepScale = stepScale;
    }

    public List<TmViewState> getAnimationSteps() {
        return animationSteps;
    }

    public boolean isAutoRotation() {
        return autoRotation;
    }

    public int getSpeed() {
        return speed;
    }

    public int getStepScale() {
        return stepScale;
    }
}

