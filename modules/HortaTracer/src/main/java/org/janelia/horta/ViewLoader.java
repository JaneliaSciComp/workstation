package org.janelia.horta;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.google.common.base.Objects;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.blocks.KtxOctreeBlockTileSource;
import org.janelia.horta.blocks.OmeZarrBlockTileSource;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.scenewindow.SceneWindow;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.geom.Vec3;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ViewLoader.class);

    private final NeuronTraceLoader loader;
    private final NeuronTracerTopComponent nttc;
    private final SceneWindow sceneWindow;


    ViewLoader(NeuronTraceLoader loader,
               NeuronTracerTopComponent nttc,
               SceneWindow sceneWindow) {
        this.loader = loader;
        this.nttc = nttc;
        this.sceneWindow = sceneWindow;
    }

    public void loadView(Vec3 syncLocation, double syncZoom) throws Exception {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                ProgressHandle progress
                        = ProgressHandleFactory.createHandle("Loading View in Horta...", null, null);
                progress.start();
                try {
                    progress.setDisplayName("Loading brain specimen...");
                    // TODO - ensure that Horta viewer is open
                    // First ensure that this component uses same sample.
                    //URL url = sampleLocation.getSampleUrl();
                    TmSample sample = TmModelManager.getInstance().getCurrentSample();
                    URL url = TmModelManager.getInstance().getTileLoader().getUrl();
                    // trying to run down a bug:
                    if (sample == null) {
                        LOG.info("found null sample for sample url " + url + " at coordinates "
                                + syncLocation.getX() + ", "
                                + syncLocation.getY() + ", "
                                + syncLocation.getZ());
                    }

                    if (nttc.isPreferKtx()
                            /** the next condition is to allow switching to a KTX sample after a ZARR sample */
                            || sample != null && sample.getLargeVolumeZarrFilepath() == null) {
                        // for KTX tile the camera must be set before the tiles are loaded in order for them to be displayed first time
                        progress.setDisplayName("Centering on location...");
                        setCameraLocation(syncZoom, syncLocation);
                        // use ktx tiles
                        KtxOctreeBlockTileSource ktxSource = createKtxSource(url, sample);
                        nttc.setKtxSource(ktxSource);
                        // start loading ktx tiles
                        progress.switchToIndeterminate(); // TODO: enhance tile loading with a progress listener
                        progress.setDisplayName("Loading KTX brain tile image...");
                        if (nttc.doesUpdateVolumeCache()) {
                            loader.loadTransientKtxTileAtCurrentFocus(nttc.getKtxSource());
                        } else {
                            loader.loadPersistentKtxTileAtCurrentFocus(nttc.getKtxSource());
                        }
                        progress.finish();
                    } else {
                        progress.setDisplayName("Centering on location...");
                        setCameraLocation(syncZoom, syncLocation);
                        // use Ome Zarr tiles
                        progress.setDisplayName("Loading Ome Zarr brain metadata...");
                        OmeZarrBlockTileSource omeZarrSource = createOmeZarrSource(url, sample, progress);
                        nttc.setOmeZarrSource(omeZarrSource);
                        // start loading Ome Zarr tiles
                        progress.switchToIndeterminate();
                        progress.setDisplayName("Loading Ome Zarr brain tile image...");
                    }
                    nttc.redrawNow();
                } catch (final Exception ex) {
                    LOG.error("Error setting up the tile source", ex);
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

                    progress.finish();
                }
            }
        };
        RequestProcessor.getDefault().post(task);
    }

    private KtxOctreeBlockTileSource createKtxSource(URL renderedOctreeUrl, TmSample sample) {
        KtxOctreeBlockTileSource previousSource = nttc.getKtxSource();
        if (previousSource != null) {
            LOG.trace("previousUrl: {}", previousSource.getOriginatingSampleURL());
            if (Objects.equal(renderedOctreeUrl, previousSource.getOriginatingSampleURL())) {
                return previousSource; // Source did not change
            }
        }
        return new KtxOctreeBlockTileSource(renderedOctreeUrl, nttc.getTileLoader(sample)).init(sample);
    }

    private OmeZarrBlockTileSource createOmeZarrSource(URL renderedOmeZarrUrl, TmSample sample, ProgressHandle progress) throws IOException {
        OmeZarrBlockTileSource previousSource = nttc.getOmeZarrSource();

        if (previousSource != null) {
            LOG.trace("previousUrl: {}", previousSource.getOriginatingSampleURL());
            if (Objects.equal(renderedOmeZarrUrl, previousSource.getOriginatingSampleURL())) {
                return previousSource; // Source did not change
            }
        }

        final boolean[] haveFirstDataset = {false};

        return new OmeZarrBlockTileSource(renderedOmeZarrUrl, nttc.getImageColorModel()).init(sample,
                (source, update) -> SwingUtilities.invokeLater(() -> {
                    if (!haveFirstDataset[0] && !source.getResolutions().isEmpty()) {
                        haveFirstDataset[0] = true;
                        try {
                            // OME-Zarr metadata is now loaded, so the channel count is known.
                            // Size the shared color model to match, plus one extra slot for the
                            // synthetic tracing/unmix channel (mirrors the legacy 2-channel
                            // TetVolumeActor convention of a dedicated slider for that channel),
                            // then re-apply the persisted/workspace color model at the correct
                            // channel count (this also rebuilds the slider UI via ColorModelUpdateEvent).
                            // Floor at 3 total channels: RemapColorActor always reads channels
                            // 0/1/2 to remap the composited RGB screen texture, so a 1-channel
                            // source (2 total) would otherwise throw ArrayIndexOutOfBoundsException.
                            int totalChannels = Math.max(3, source.getChannelCount() + 1);
                            if (nttc.getImageColorModel().getChannelCount() != totalChannels) {
                                nttc.getImageColorModel().reset(65535, totalChannels);
                                nttc.getImageColorModel().getChannel(totalChannels - 1)
                                        .setColor(new Color(0f, 0.5f, 1.0f)); // unmixed channel in Economo blue
                                nttc.initColorModel();
                            }
                            if (nttc.doesUpdateVolumeCache()) {
                                loader.loadTransientOmeZarrTileAtCurrentFocus(nttc.getOmeZarrSource());
                            } else {
                                loader.loadPersistentOmeZarrTileAtCurrentFocus(nttc.getOmeZarrSource());
                            }
                            nttc.redrawNow();
                        } catch (Exception ex) {
                            LOG.error(ex.getMessage());
                        }
                    }

                    progress.setDisplayName(update);
                }),
                (source) -> SwingUtilities.invokeLater(progress::finish));
    }

    private void setCameraLocation(double zoom, Vec3 sampleLocation) {
        // Now, position this component over other component's focus.
        PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
        Vantage v = pCam.getVantage();
        Vector3 focusVector3 = new Vector3(
                (float) sampleLocation.getX(),
                (float) sampleLocation.getY(),
                (float) sampleLocation.getZ());

        if (!v.setFocusPosition(focusVector3)) {
            LOG.info("New focus is the same as previous focus");
        }
        v.setDefaultFocus(focusVector3);

        if (zoom > 0) {
            v.setSceneUnitsPerViewportHeight((float) zoom);
            v.setDefaultSceneUnitsPerViewportHeight((float) zoom);
        }

        v.notifyObservers();
    }

}
