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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.ViewerLocationAcceptor;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import static org.janelia.horta.NeuronTracerTopComponent.BASE_YML_FILE;
import org.janelia.horta.volume.StaticVolumeBrickSource;
import org.janelia.scenewindow.SceneWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleLocationAcceptor implements ViewerLocationAcceptor {

    private String currentSource;
    private NeuronTraceLoader loader;
    private YamlStreamLoader yamlLoader;
    private SceneWindow sceneWindow;
    private StaticVolumeBrickSource volumeSource;

    private static final Logger logger = LoggerFactory.getLogger(SampleLocationAcceptor.class);
    
    public SampleLocationAcceptor(
            String currentSource, 
            NeuronTraceLoader loader, 
            YamlStreamLoader yamlLoader, 
            SceneWindow sceneWindow, 
            StaticVolumeBrickSource volumeSource) {
        this.currentSource = currentSource;
        this.loader = loader;
        this.yamlLoader = yamlLoader;
        this.sceneWindow = sceneWindow;
        this.volumeSource = volumeSource;
    }
    
    @Override
    public void acceptLocation(SampleLocation sampleLocation) throws Exception {
        // First ensure that this component uses same sample.
        URL focusUrl = sampleLocation.getSampleUrl();
        if (focusUrl != null) {
            String urlStr = focusUrl.toString();
            // Check: if same as current source, no need to change that,
            // unless the volume source has not been populated.
            if (volumeSource != null && volumeSource.getAvailableResolutions().isEmpty()) {
                logger.warn("Volume source may not have been populated. " + volumeSource);
            }
            if (!urlStr.equals(currentSource)  || 
                volumeSource == null  ||
                volumeSource.getAvailableResolutions().isEmpty()) {

                URI uri = focusUrl.toURI();
                URI yamlUri = new URI(
                        uri.getScheme(),
                        uri.getAuthority(),
                        uri.getPath() + "/" + BASE_YML_FILE,
                        uri.getFragment()
                );
                logger.info("Constructed URI: {}.", uri);
                URL yamlUrl = yamlUri.toURL();
                InputStream stream1 = yamlUrl.openStream();
                InputStream stream2 = yamlUrl.openStream();
                volumeSource = yamlLoader.loadYaml(stream1, loader, stream2, false);
                currentSource = urlStr;
                logger.info("Making NEW volume source {}.", volumeSource);                
            }

            // Now, position this component over other component's
            // focus.
            Vector3 focusVector3 = null;
            PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
            Vantage v = pCam.getVantage();
            double [] focusCoords = new double[] {
                sampleLocation.getFocusXUm(),
                sampleLocation.getFocusYUm(),
                sampleLocation.getFocusZUm()
            };
            if (focusCoords == null) {
                logger.info("No focus coords provided.");
                focusVector3 = sceneWindow.getCamera().getVantage().getFocusPosition();
            }
            else {
                //Vantage v = sceneWindow.getVantage();
                focusVector3 = new Vector3(
                        (float) focusCoords[0],
                        (float) focusCoords[1],
                        (float) focusCoords[2]
                );
            }
//            if (!loader.animateToFocusXyz(focusVector3, v, 150)) {
//                logger.warn("Did not change focus as directed.");
//            }
            if (!v.setFocusPosition(focusVector3)) {
                logger.warn("Did not change focus as directed.");
            }
            v.setDefaultFocus(focusVector3);
            logger.info("Set focus, default focus, to " + focusVector3);
            
            double zoom = sampleLocation.getMicrometersPerWindowHeight();
            if (zoom > 0) {
                v.setSceneUnitsPerViewportHeight((float)zoom);
                logger.info("Set micrometers per view height to " + zoom);
            }

            v.notifyObservers();

            // Load up the tile.
            loader.loadTileAtCurrentFocus(volumeSource);
            sceneWindow.getGLAutoDrawable().display();
        } else {
            logger.warn("No URL location provided.");
        }

    }

}
