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

import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.ViewerLocationAcceptor;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.blocks.BlockTileSource;
import org.janelia.horta.blocks.JadeKtxOctreeBlockTileSource;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.horta.volume.BrickInfoSet;
import org.janelia.horta.volume.StaticVolumeBrickSource;
import org.janelia.it.jacs.shared.lvv.HttpDataSource;
import org.janelia.it.workstation.browser.api.web.JadeServiceClient;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.scenewindow.SceneWindow;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import jersey.repackaged.com.google.common.base.Objects;

import static org.janelia.horta.NeuronTracerTopComponent.BASE_YML_FILE;
import org.janelia.horta.blocks.KtxOctreeBlockTileSource;

public class SampleLocationAcceptor implements ViewerLocationAcceptor {
    private static final Logger LOG = LoggerFactory.getLogger(SampleLocationAcceptor.class);

    private String currentSource;
    private NeuronTraceLoader loader;
    private NeuronTracerTopComponent nttc;
    private SceneWindow sceneWindow;

    public SampleLocationAcceptor(
            String currentSource,
            NeuronTraceLoader loader,
            NeuronTracerTopComponent nttc,
            SceneWindow sceneWindow) {
        this.currentSource = currentSource;
        this.loader = loader;
        this.nttc = nttc;
        this.sceneWindow = sceneWindow;
    }

    @Override
    public void acceptLocation(final SampleLocation sampleLocation) throws Exception {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                ProgressHandle progress
                        = ProgressHandleFactory.createHandle("Loading View in Horta...");
                progress.start();
                try {
                    progress.setDisplayName("Loading brain specimen...");
                    // TODO - ensure that Horta viewer is open
                    // First ensure that this component uses same sample.
                    URL url = sampleLocation.getSampleUrl();
                    TmSample sample = sampleLocation.getSample();

                    // trying to run down a bug:
                    if (sample == null) {
                        LOG.info("found null sample for sample url " + url + " at coordinates "
                                + sampleLocation.getFocusXUm() + ", "
                                + sampleLocation.getFocusYUm() + ", "
                                + sampleLocation.getFocusZUm());
                    }

                    // First check to see if ktx tiles are available
                    if (nttc.isPreferKtx()) {
                        KtxOctreeBlockTileSource ktxSource = loadKtxSource(sample, url, progress);
                        if (ktxSource != null) {
                            nttc.setKtxSource(ktxSource);
                        }
                    } else {
                        nttc.setKtxSource(null);
                    }

                    progress.setDisplayName("Centering on location...");
                    setCameraLocation(sampleLocation);

                    if (nttc.getKtxSource() == null) { // Use obsolete single channel raw file loading
                        StaticVolumeBrickSource volumeSource = setSampleUrl(url, progress);
                        if (volumeSource == null) {
                            throw new IOException("Loading volume source failed");
                        }
                        progress.switchToIndeterminate(); // TODO - enhance tile loading with a progress listener
                        progress.setDisplayName("Loading brain tile image...");
                        loader.loadTileAtCurrentFocus(volumeSource, sampleLocation.getDefaultColorChannel());
                    } else { // Load ktx files here
                        progress.switchToIndeterminate(); // TODO: enhance tile loading with a progress listener
                        progress.setDisplayName("Loading KTX brain tile image...");
                        if (nttc.doesUpdateVolumeCache()) {
                            loader.loadTransientKtxTileAtCurrentFocus(nttc.getKtxSource());
                        } else {
                            loader.loadPersistentKtxTileAtCurrentFocus(nttc.getKtxSource());
                        }
                    }
                    nttc.redrawNow();
                } catch (final IOException ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(
                                    sceneWindow.getOuterComponent(),
                                    "Error loading brain specimen: " + ex.getMessage(),
                                    "Brain Raw Data Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    });
                } finally {
                    progress.finish();
                }
            }
        };
        RequestProcessor.getDefault().post(task);
    }

    private KtxOctreeBlockTileSource loadKtxSource(TmSample sample, URL renderedOctreeUrl, ProgressHandle progress) {
        progress.setDisplayName("Checking for ktx rendered tiles");
        KtxOctreeBlockTileSource previousSource = nttc.getKtxSource();
        if (previousSource != null) {
            LOG.trace("previousUrl: {}", previousSource.getOriginatingSampleURL());
            if (Objects.equal(renderedOctreeUrl, previousSource.getOriginatingSampleURL()))
                return previousSource; // Source did not change
        }
        return new JadeKtxOctreeBlockTileSource(new JadeServiceClient(), renderedOctreeUrl, sample);
    }

    private StaticVolumeBrickSource setSampleUrl(URL renderedOctreeUrl, ProgressHandle progress) {
        String urlStr = renderedOctreeUrl.toString();
        // Check: if same as current source, no need to change that.
        if (urlStr.equals(currentSource))
            return nttc.getVolumeSource();
        URI uri;
        // First check whether the yaml file exists at all
        StaticVolumeBrickSource volumeSource = null;
        progress.setDisplayName("Loading brain specimen (tilebase.cache.yml)...");
        try {
            uri = renderedOctreeUrl.toURI();
            String yamlUrlString = new URL(renderedOctreeUrl, BASE_YML_FILE).getPath();
            URI yamlUri = new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    yamlUrlString,
                    uri.getFragment()
            );
            LOG.info("Constructed URI: {}.", uri);
            URL yamlUrl = yamlUri.toURL();
            try (InputStream stream1 = yamlUrl.openStream()) {
                volumeSource = nttc.loadYaml(stream1, loader, progress);
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(nttc,
                        "Problem Loading Raw Tile Information from " + yamlUrlString +
                                "\n  Does the transform contain barycentric coordinates?",
                        "Tilebase File Problem", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException | URISyntaxException ex) {
            // Something went wrong with loading the Yaml file
            // Exceptions.printStackTrace(ex);
            JOptionPane.showMessageDialog(nttc,
                    "Problem Loading Raw Tile Information from " + renderedOctreeUrl.getPath() +
                            "\n  Is the render folder drive mounted?"
                            + "\n  Does the render folder contain a " + BASE_YML_FILE + " file ?"
                    ,
                    "Tilebase File Problem",
                    JOptionPane.ERROR_MESSAGE);
        }

        LOG.info("Finished creating volumeSource from yaml file");

        if (null == volumeSource)
            return volumeSource;

        // Test for location of first tile, as a sanity check
        if (!HttpDataSource.useHttp()) {
            Double res = volumeSource.getAvailableResolutions().iterator().next();
            BrickInfoSet bricks = volumeSource.getAllBrickInfoForResolution(res);
            BrickInfo b = bricks.iterator().next();
            BrainTileInfo bti = (BrainTileInfo) b;
            String path = bti.getParentPath() + "/" + bti.getLocalPath();
            File brickFolder = new File(bti.getParentPath(), bti.getLocalPath());
            if (!brickFolder.exists()) {
                JOptionPane.showMessageDialog(nttc,
                        "Problem Finding First Raw Tile Folder at " + brickFolder.getAbsolutePath()
                                + "\n  Is the raw tile folder drive mounted?"
                                + "\n  Did someone change the folder structure of the raw tiles?"
                        ,
                        "Raw Tile Location Problem",
                        JOptionPane.ERROR_MESSAGE);
                volumeSource = null; // Don't use this data source
            }
        }

        return volumeSource;
    }

    private boolean setCameraLocation(SampleLocation sampleLocation) {
        // Now, position this component over other component's
        // focus.
        PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
        Vantage v = pCam.getVantage();
        Vector3 focusVector3 = new Vector3(
                (float) sampleLocation.getFocusXUm(),
                (float) sampleLocation.getFocusYUm(),
                (float) sampleLocation.getFocusZUm());

        if (!v.setFocusPosition(focusVector3)) {
            LOG.info("New focus is the same as previous focus");
        }
        v.setDefaultFocus(focusVector3);

        double zoom = sampleLocation.getMicrometersPerWindowHeight();
        if (zoom > 0) {
            v.setSceneUnitsPerViewportHeight((float) zoom);
            // logger.info("Set micrometers per view height to " + zoom);
            v.setDefaultSceneUnitsPerViewportHeight((float) zoom);
        }

        v.notifyObservers();
        return true;
    }

}
