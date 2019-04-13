package org.janelia.horta.movie;

import com.google.gson.JsonObject;

/**
 *
 * @author brunsc
 */
public interface KeyFrame
{
    ViewerState getViewerState();
    float getFollowingIntervalDuration(); // time in seconds between this frame and the next
    void setFollowingIntervalDuration(float seconds); //  time in seconds between this frame and the next
    JsonObject serializeJson();
}
