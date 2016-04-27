/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.horta.movie;

import java.util.Deque;
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
public class BasicMoviePlayState<T extends ViewerState> implements MoviePlayState<T>
{
    private final Timeline<T> timeline;
    private boolean doLoop = false;
    private float framesPerSecond;
    private final MovieSource<T> movieSource;
    
    private double previousFrameStartTimeInLab = System.nanoTime() / 1.0e9;
    private double currentFrameTimeInVideo = 0;
    private boolean bIsRunning = false;
    
    private double minFrameDuration = 1.0 / 5.0;
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public BasicMoviePlayState(Timeline<T> timeline, MovieSource<T> movieSource) {
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
        Deque<KeyFrame<T>> f = timeline;
        float totalDuration = 0;
        for (KeyFrame<T> keyFrame : f) {
            totalDuration += keyFrame.getFollowingIntervalDuration();
        }
        // Don't include final frame duration, unless movie is a loop
        if ( (! isLoop()) && (f.size() > 0) ) {
            KeyFrame<T> finalFrame = f.getLast();
            totalDuration -= finalFrame.getFollowingIntervalDuration();
        }
        return totalDuration;
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void playEveryFrame(float framesPerSecond) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    public void setChanged() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clearChanged() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addObserver(GenericObserver<T> observer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int countObservers() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteObserver(GenericObserver<T> observer) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteObservers() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasChanged() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void notifyObservers(ViewerState data) {
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
    
    public void reset() {
        bIsRunning = false;
        currentFrameTimeInVideo = 0;
        previousFrameStartTimeInLab = System.nanoTime() / 1.0e9;
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
            
            logger.info("initial elapsed time since last frame = " + elapsed + " seconds");

            if (elapsed < minFrameDuration) {
                long sleepTimeMs = (long)((minFrameDuration - elapsed) * 1000);
                logger.info("sleeping for = " + sleepTimeMs + " milliseconds");            
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
            final T state = timeline.viewerStateForTime((float)nextFrameTime, doLoop);

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
