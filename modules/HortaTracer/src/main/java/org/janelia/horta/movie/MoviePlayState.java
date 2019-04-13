package org.janelia.horta.movie;

import java.awt.image.BufferedImage;

/**
 *
 * @author brunsc
 */
public interface MoviePlayState
{
    BufferedImage getCurrentFrameImageNow(); // must be called from GUI thread
    float getFramesPerSecond();
    void setFramesPerSecond(float fps);
    float getTotalDuration();
    boolean isLoop();
    void setLoop(boolean doLoop);
    boolean isRunning();
    void skipToTime(float seconds);
    void playRealTime(float maxFramesPerSecond);
    void pause();
    void reset();
}
