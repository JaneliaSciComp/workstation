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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JOptionPane;
import org.janelia.scenewindow.SceneWindow;
import org.janelia.geometry.util.PerformanceTimer;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.GL3Actor;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.horta.volume.BrickInfoSet;
import org.janelia.horta.volume.StaticVolumeBrickSource;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to Neuron tracing viewer.  Carries out load.
 *
 * @author fosterl
 */
public class NeuronTraceLoader {
    private Logger logger = LoggerFactory.getLogger(NeuronTraceLoader.class);
    
    private NeuronTracerTopComponent nttc;
    private NeuronMPRenderer neuronMPRenderer;
    private SceneWindow sceneWindow;
    private TracingInteractor tracingInteractor;
    
    public NeuronTraceLoader(
            NeuronTracerTopComponent nttc, 
            NeuronMPRenderer neuronMPRenderer, 
            SceneWindow sceneWindow, 
            TracingInteractor tracingInteractor) {
        this.nttc = nttc;
        this.neuronMPRenderer = neuronMPRenderer;
        this.sceneWindow = sceneWindow;
        this.tracingInteractor = tracingInteractor;
    }
    
    public void loadYamlFile(final InputStream yamlStream) throws IOException {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                ProgressHandle progress = ProgressHandleFactory.createHandle("Loading brain tiles");
                progress.start();
                progress.progress("Loading YAML tile information...");

                PerformanceTimer timer = new PerformanceTimer();
                BrainTileInfoList tileList = new BrainTileInfoList();
                try {
                    tileList.loadYamlFile(yamlStream);
                } catch (IOException ex) {
                    handleException(ex);
                }
                progress.progress("YAML tile information loaded");
                logger.info("yaml load took " + timer.reportMsAndRestart() + " ms");
                // TODO remove this testing hack
                if (! loadExampleTile(tileList, progress)) {
                    RuntimeException re = new RuntimeException(FAILED_TO_LOAD_EXAMPLE_TILE_MSG);
                    handleException(re);
                }
                progress.progress("Example tile loaded");

                progress.finish();
            }
            public static final String FAILED_TO_LOAD_EXAMPLE_TILE_MSG = "Failed to load example tile.";
        };
        RequestProcessor.getDefault().post(task);
    }

    /**
     * Animates to next point in 3D space TODO - run this in another thread
     *
     * @param xyz
     * @param vantage
     */
    public boolean animateToFocusXyz(Vector3 xyz, Vantage vantage, int milliseconds) {
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
        return didMove;
    }

    /**
     * Helper method toward automatic tile loading
     */
    public BrickInfo loadTileAtCurrentFocus( StaticVolumeBrickSource volumeSource ) throws IOException {
        PerformanceTimer timer = new PerformanceTimer();

        PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();

        // 2 - Load the brick at the center...
        // TODO - should happen automatically
        // Find the best resolution available
        double screenPixelResolution = pCam.getVantage().getSceneUnitsPerViewportHeight()
                / pCam.getViewport().getHeightPixels();
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
        BrickInfoSet brickInfoSet = volumeSource.getAllBrickInfoForResolution(brickResolution);
        BrickInfo brickInfo = brickInfoSet.getBestContainingBrick(pCam.getVantage().getFocusPosition());

        ProgressHandle progress
                = ProgressHandleFactory.createHandle(
                "Loading Tiff Volume...");
        progress.start();

        GL3Actor boxMesh = nttc.createBrickActor((BrainTileInfo) brickInfo);

        progress.finish();

        StatusDisplayer.getDefault().setStatusText(
                "One TIFF file loaded and processed in "
                + String.format("%1$,.2f", timer.reportMsAndRestart() / 1000.0)
                + " seconds."
        );

        // mprActor.addChild(boxMesh);
        neuronMPRenderer.clearVolumeActors();
        neuronMPRenderer.addVolumeActor(boxMesh);
        
        return brickInfo;
    }

    private boolean loadExampleTile(BrainTileInfoList tileList, ProgressHandle progress) {

        BrainTileInfo exampleTile = null;
        // Find first existing tile
        for (BrainTileInfo tile : tileList) {
            if (tile.folderExists()) {
                exampleTile = tile;
                break;
            }
        }
        
        if (exampleTile == null) {
            logger.error("No tiles found");
            StringBuilder bldr = new StringBuilder("Nonexistent Files List:");
            for (BrainTileInfo tile: tileList) {
                bldr.append(" ");
                bldr.append(
                    tile.getParentPath()).append("/").append(tile.getLocalPath()
                );
            }
            logger.info(bldr.toString());
            return false;
        }

        File tileFile = new File(exampleTile.getParentPath(), exampleTile.getLocalPath());
        logger.info(tileFile.getAbsolutePath());

        progress.progress("Loading tile file " + tileFile);
        try {
            GL3Actor brickActor = nttc.createBrickActor(exampleTile);
            // mprActor.addChild(brickActor);
            neuronMPRenderer.addVolumeActor(brickActor);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        // progress.finish();
        
        for (NeuriteActor tracingActor : tracingInteractor.createActors()) {
            sceneWindow.getRenderer().addActor(tracingActor);
            tracingActor.getModel().addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    sceneWindow.getInnerComponent().repaint();
                }
            });
        }

        Vantage v = sceneWindow.getVantage();
        v.centerOn(exampleTile.getBoundingBox());
        v.setDefaultBoundingBox(exampleTile.getBoundingBox());
        v.notifyObservers();

        return true;
    }

    private void handleException(Exception ex) {
        Exceptions.printStackTrace(ex);
        JOptionPane.showMessageDialog(nttc, ex);
    }
    

}
