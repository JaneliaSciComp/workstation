package org.janelia.horta.movie;

import java.util.Deque;

/**
 *
 * @author brunsc
 */
public interface Timeline extends Deque<KeyFrame>
{
    ViewerState viewerStateForTime(float timeInSeconds, boolean doLoop);

    // Returns duration of entire animation, in seconds.
    // If "doLoop" is true, the duration of the final key frame is included in the total.
    public float getTotalDuration(boolean doLoop);
}
