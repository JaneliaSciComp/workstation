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
import org.janelia.geometry3d.Vantage;
import org.janelia.gltools.GL3Actor;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
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
    
    public NeuronTraceLoader(NeuronTracerTopComponent nttc, NeuronMPRenderer neuronMPRenderer, SceneWindow sceneWindow, TracingInteractor tracingInteractor) {
        this.nttc = nttc;
        this.neuronMPRenderer = neuronMPRenderer;
        this.sceneWindow = sceneWindow;
        this.tracingInteractor = tracingInteractor;
    }
    
    public void loadYamlFile(final InputStream yamlStream) throws IOException {
        Runnable task = new Runnable() {
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
