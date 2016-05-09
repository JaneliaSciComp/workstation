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
package org.janelia.horta;

import org.janelia.horta.render.NeuronMPRenderer;
import java.io.IOException;
import org.janelia.scenewindow.SceneWindow;
import org.janelia.geometry.util.PerformanceTimer;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.GL3Actor;
import org.janelia.horta.volume.BrickActor;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.horta.volume.BrickInfoSet;
import org.janelia.horta.volume.StaticVolumeBrickSource;
import org.openide.awt.StatusDisplayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to Neuron tracing viewer.  Carries out load.
 *
 * @author fosterl
 */
public class NeuronTraceLoader {
    private Logger logger = LoggerFactory.getLogger(NeuronTraceLoader.class);
    
    private final NeuronTracerTopComponent nttc;
    private final NeuronMPRenderer neuronMPRenderer;
    private final SceneWindow sceneWindow;
    private int defaultColorChannel = 0;
    // private TracingInteractor tracingInteractor;
    
    public NeuronTraceLoader(
            NeuronTracerTopComponent nttc, 
            NeuronMPRenderer neuronMPRenderer, 
            SceneWindow sceneWindow 
            // TracingInteractor tracingInteractor
            ) {
        this.nttc = nttc;
        this.neuronMPRenderer = neuronMPRenderer;
        this.sceneWindow = sceneWindow;
        // this.tracingInteractor = tracingInteractor;
    }


    /**
     * Animates to next point in 3D space TODO - run this in another thread
     *
     * @param xyz
     * @param vantage
     */
    public boolean animateToFocusXyz(Vector3 xyz, Vantage vantage, int milliseconds) 
    {
        // Disable auto loading during move
        boolean wasCacheEnabled = nttc.doesUpdateVolumeCache();
        nttc.setUpdateVolumeCache(false);
        
        Vector3 startPos = new Vector3(vantage.getFocusPosition());
        Vector3 endPos = new Vector3(xyz);
        long startTime = System.nanoTime();
        long targetTime = milliseconds * 1000000;
        final int stepCount = 40;
        boolean didMove = false;
        for (int s = 0; s < stepCount - 1; ++s) {
            // skip frames to match expected time
            float alpha = s / (float) (stepCount - 1);
            double deltaTime = (System.nanoTime() - startTime) / 1e6;
            double desiredTime = (alpha * targetTime) / 1e6;
            // logger.info("Elapsed = "+deltaTime+" ms; desired = "+desiredTime+" ms");
            if (deltaTime > desiredTime) {
                continue; // skip this frame
            }
            Vector3 a = new Vector3(startPos).multiplyScalar(1.0f - alpha);
            Vector3 b = new Vector3(endPos).multiplyScalar(alpha);
            a = a.add(b);
            if (vantage.setFocusPosition(a)) {
                didMove = true;
                vantage.notifyObservers();
                sceneWindow.getGLAutoDrawable().display();
                // sceneWindow.getInnerComponent().repaint();
            }
        }
        double elapsed = (System.nanoTime() - startTime) * 1e-6;
        // logger.info("Animation took " + elapsed + " ms");
        // never skip the final frame
        if (vantage.setFocusPosition(endPos)) {
            didMove = true;
        }
        if (didMove) {
            vantage.notifyObservers();
            sceneWindow.getGLAutoDrawable().display();
            // sceneWindow.getInnerComponent().repaint();
        }
        
        nttc.setUpdateVolumeCache(wasCacheEnabled);
        return didMove;
    }

    public BrickInfo loadTileAtCurrentFocus( StaticVolumeBrickSource volumeSource ) throws IOException {
        return loadTileAtCurrentFocus( volumeSource, defaultColorChannel );
    }
    
    public static BrickInfoSet getBricksForCameraResolution(StaticVolumeBrickSource volumeSource, PerspectiveCamera camera) 
    {
        double screenPixelResolution = camera.getVantage().getSceneUnitsPerViewportHeight()
                / camera.getViewport().getHeightPixels();
        double minDist = Double.MAX_VALUE;
        Double bestRes = null;
        for (Double res : volumeSource.getAvailableResolutions()) {
            double dist = Math.abs(Math.log(res) - Math.log(screenPixelResolution));
            if (dist < minDist) {
                bestRes = res;
                minDist = dist;
            }
        }
        Double brickResolution = bestRes;
        assert brickResolution != null : "No best-resolution found.  Volume Source=" + volumeSource;

        BrickInfoSet brickInfoSet = volumeSource.getAllBrickInfoForResolution(brickResolution); 
        return brickInfoSet;
    }
    
    /**
     * Helper method toward automatic tile loading
     */
    public BrickInfo loadTileAtCurrentFocus( StaticVolumeBrickSource volumeSource, int colorChannel ) throws IOException {
        PerformanceTimer timer = new PerformanceTimer();
        
        // Remember most recently loaded color channel for next time
        defaultColorChannel = colorChannel;

        PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
        BrickInfoSet brickInfoSet = getBricksForCameraResolution(volumeSource, pCam);
        
        BrickInfo brickInfo = brickInfoSet.getBestContainingBrick(pCam.getVantage().getFocusPosition());

        // Check for existing brick already loaded here
        BrainTileInfo brainTileInfo = (BrainTileInfo) brickInfo;
        String brickName = brainTileInfo.getLocalPath();
        boolean tileAlreadyLoaded = false;
        for (GL3Actor actor : neuronMPRenderer.getVolumeActors()) {
            if (!(actor instanceof BrickActor))
                continue;
            BrickActor ba = (BrickActor)actor;
            if ( ba.getBrainTile().isSameBrick(brainTileInfo) 
                    && (colorChannel == brainTileInfo.getColorChannelIndex()) ) // reload if color changed
            {
                tileAlreadyLoaded = true;
                break;
            }
        }
        
        if ( (! tileAlreadyLoaded) && (! nttc.doesUpdateVolumeCache())) {
            GL3Actor boxMesh = nttc.createBrickActor((BrainTileInfo) brickInfo, colorChannel);

            StatusDisplayer.getDefault().setStatusText(
                    "One TIFF file loaded and processed in "
                    + String.format("%1$,.2f", timer.reportMsAndRestart() / 1000.0)
                    + " seconds."
            );

            nttc.registerLoneDisplayedTile((BrickActor)boxMesh);
            
            // Clear, so only one tiles is shown at a time (two tiles are in memory during transition)
            neuronMPRenderer.clearVolumeActors();
            neuronMPRenderer.addVolumeActor(boxMesh);
        }
        
        return brickInfo;
    }
}
