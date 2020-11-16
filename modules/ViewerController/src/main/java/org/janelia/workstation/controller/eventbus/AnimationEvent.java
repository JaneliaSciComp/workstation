package org.janelia.workstation.controller.eventbus;

import org.janelia.workstation.controller.model.TmViewState;
import java.util.ArrayList;
import java.util.List;

public class AnimationEvent extends ViewerEvent {

    List<TmViewState> animationSteps = new ArrayList<>();
    boolean autoRotation;
    int speed;
    int stepScale;

    public AnimationEvent(Object source,
                          List<TmViewState> steps,
                          boolean autoRotation,
                          int speed, int stepScale) {
        super(source);
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

