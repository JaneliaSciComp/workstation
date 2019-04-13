package org.janelia.horta.movie;

import com.google.gson.JsonObject;

/**
 *
 * @author brunsc
 */
public class BasicKeyFrame implements KeyFrame
{
    private final ViewerState viewerState;
    private float followingIntervalDuration;

    public BasicKeyFrame(ViewerState viewerState, float followingIntervalDuration) 
    {
        this.viewerState = viewerState;
        this.followingIntervalDuration = followingIntervalDuration;
    }

    @Override
    public ViewerState getViewerState() {
        return viewerState;
    }

    @Override
    public float getFollowingIntervalDuration() {
        return followingIntervalDuration;
    }

    @Override
    public void setFollowingIntervalDuration(float seconds) {
        this.followingIntervalDuration = seconds;
    }

    @Override
    public JsonObject serializeJson() {
        JsonObject result = getViewerState().serialize();
        result.addProperty("followingInterval", followingIntervalDuration);
        return result;
    }

}
