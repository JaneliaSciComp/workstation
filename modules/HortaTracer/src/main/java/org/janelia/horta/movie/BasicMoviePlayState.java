package org.janelia.horta.movie;

import java.awt.image.BufferedImage;
import java.util.Observable;
import java.util.Observer;
import javax.swing.SwingUtilities;
import org.janelia.console.viewerapi.GenericObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brunsc
 */
public class BasicMoviePlayState implements MoviePlayState
{
    private final Timeline timeline;
    private boolean doLoop = false;
    private float framesPerSecond;
    private final MovieSource movieSource;
    
    private double previousFrameStartTimeInLab = System.nanoTime() / 1.0e9;
    private double currentFrameTimeInVideo = 0;
    private boolean bIsRunning = false;
    
    private double minFrameDuration = 1.0 / 5.0;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public BasicMoviePlayState(
            Timeline timeline, 
            MovieSource movieSource) 
    {
        this.timeline = timeline;
        this.movieSource = movieSource;
        
        // Here what we do when the viewer signals it has updated the previous frame:
        movieSource.getViewerStateUpdatedObservable().addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (bIsRunning)
                    advanceToNextFrame();
            }
        });
    }
    
    private synchronized void start() {
        if (bIsRunning)
            return; // already playing
        bIsRunning = true;
        previousFrameStartTimeInLab = System.nanoTime() / 1.0e9; // note start time
        (new NextFrameThread()).start();
    }
    
    private synchronized void advanceToNextFrame() {
        (new NextFrameThread()).start();
    }
    
    @Override
    public float getTotalDuration() {
        return timeline.getTotalDuration(isLoop());
    }
    
    @Override
    public boolean isLoop() {
        return doLoop;
    }

    @Override
    public void setLoop(boolean doLoop) {
        this.doLoop = doLoop;
    }

    @Override
    public boolean isRunning() {
        return bIsRunning;
    }

    @Override
    public void skipToTime(float seconds) {
        currentFrameTimeInVideo = seconds;
    }

    @Override
    public void playRealTime(float maxFramesPerSecond) {
        minFrameDuration = 1.0/maxFramesPerSecond;
        start();
    }

    @Override
    public void pause() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public float getFramesPerSecond() {
        return framesPerSecond;
    }

    @Override
    public void setFramesPerSecond(float framesPerSecond) {
        this.framesPerSecond = framesPerSecond;
    }
    
    @Override
    public void reset() {
        bIsRunning = false;
        currentFrameTimeInVideo = 0;
        previousFrameStartTimeInLab = System.nanoTime() / 1.0e9;
    }
    
    // Must be run in GUI thread
    @Override
    public BufferedImage getCurrentFrameImageNow() {
        ViewerState state = timeline.viewerStateForTime((float)currentFrameTimeInVideo, doLoop);
        BufferedImage result = movieSource.getRenderedFrame(state);
        return result;
    }
    
    class NextFrameThread extends Thread implements Runnable
    {
        @Override
        public void run() 
        {
            if (! bIsRunning)
                return; // paused or something

            // logger.info("previous movie frame play time was " + currentFrameTimeInVideo + " seconds");

            // Are we done playing?
            double totalDuration = getTotalDuration();
            
            if (totalDuration == 0) {
                // This is a one frame non-looping movie, so just go to that one frame
                bIsRunning = false;
                currentFrameTimeInVideo = 0; // where to set this, if not here?
                // don't return in this case, we want to show the one frame
            }
            else if ( (!doLoop) && (currentFrameTimeInVideo >= totalDuration) ) {
                bIsRunning = false;
                currentFrameTimeInVideo = 0; // where to set this, if not here?
                return; // movie ended
            }
            
            // How long since the previous frame, or start if this is the first?
            double now = System.nanoTime() / 1.0e9; // seconds
            double elapsed = now - previousFrameStartTimeInLab;
            
            // logger.info("initial elapsed time since last frame = " + elapsed + " seconds");

            if (elapsed < minFrameDuration) {
                long sleepTimeMs = (long)((minFrameDuration - elapsed) * 1000);
                // logger.info("sleeping for = " + sleepTimeMs + " milliseconds");            
                try {
                    Thread.sleep( (long)((minFrameDuration - elapsed) * 1000) );
                } catch (InterruptedException ex) {
                    // Exceptions.printStackTrace(ex);
                }
            }
            

            // Update after sleep
            now = System.nanoTime() / 1.0e9; // seconds
            elapsed = now - previousFrameStartTimeInLab;
            
            // logger.info("elapsed time since last frame = " + elapsed + " seconds");

            double nextFrameTime = currentFrameTimeInVideo + elapsed;
            if (doLoop) {
                if (nextFrameTime > totalDuration) {
                    nextFrameTime -= totalDuration;
                }
            }
            else {
                if (nextFrameTime > totalDuration) {
                    nextFrameTime = totalDuration;
                }
            }
            final ViewerState state = timeline.viewerStateForTime((float)nextFrameTime, doLoop);

            // Update internal state
            previousFrameStartTimeInLab = now;
            currentFrameTimeInVideo = nextFrameTime;
            // logger.info("playing movie frame at time " + nextFrameTime + " seconds");
            
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    movieSource.setViewerState(state);
                }
            });
        }
    };
    
}
