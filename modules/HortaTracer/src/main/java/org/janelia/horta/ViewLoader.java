package org.janelia.horta;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.janelia.horta.blocks.OmeZarrBlockTileSource;
import org.janelia.horta.omezarr.OmeZarrReaderCompletionObserver;
import org.janelia.horta.omezarr.OmeZarrReaderProgressObserver;
import org.janelia.workstation.controller.tileimagery.OsFilePathRemapper;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.blocks.KtxOctreeBlockTileSource;
import org.janelia.horta.util.HttpClientHelper;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.security.AppAuthorization;
import org.janelia.rendering.FileBasedRenderedVolumeLocation;
import org.janelia.rendering.JADEBasedRenderedVolumeLocation;
import org.janelia.rendering.RenderedVolume;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.rendering.RenderedVolumeMetadata;
import org.janelia.rendering.utils.ClientProxy;
import org.janelia.scenewindow.SceneWindow;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.geom.Vec3;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ViewLoader.class);
    private static final HttpClientHelper HTTP_HELPER = new HttpClientHelper();

    private final NeuronTraceLoader loader;
    private final NeuronTracerTopComponent nttc;
    private final SceneWindow sceneWindow;
    private final ObjectMapper objectMapper;


    ViewLoader(NeuronTraceLoader loader,
               NeuronTracerTopComponent nttc,
               SceneWindow sceneWindow) {
        this.loader = loader;
        this.nttc = nttc;
        this.sceneWindow = sceneWindow;
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

                    if (nttc.isPreferKtx()) {
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

    private RenderedVolume getVolumeInfo(URI renderedOctreeUri) {
        RenderedVolumeLocation renderedVolumeLocation;
        RenderedVolumeMetadata renderedVolumeMetadata;
        if (StringUtils.equalsIgnoreCase(renderedOctreeUri.getScheme(), "http") || StringUtils.equalsIgnoreCase(renderedOctreeUri.getScheme(), "https")) {
            String url = renderedOctreeUri.resolve("volume_info").toString();
            LOG.trace("Getting volume metadata from: {}", url);
            GetMethod getMethod = new GetMethod(url);
            getMethod.getParams().setParameter("http.method.retry-handler", new DefaultHttpMethodRetryHandler(3, false));
            AppAuthorization appAuthorization = AccessManager.getAccessManager().getAppAuthorization();
            try {
                int statusCode = HTTP_HELPER.executeMethod(getMethod, appAuthorization);
                if (statusCode != 200) {
                    throw new IllegalStateException("HTTP status " + statusCode + " (not OK) from url " + url);
                }
                renderedVolumeMetadata = objectMapper.readValue(getMethod.getResponseBodyAsStream(), RenderedVolumeMetadata.class);
                renderedVolumeLocation = new JADEBasedRenderedVolumeLocation(
                        renderedVolumeMetadata.getConnectionURI(),
                        renderedVolumeMetadata.getDataStorageURI(),
                        renderedVolumeMetadata.getVolumeBasePath(),
                        appAuthorization.getAuthenticationToken(),
                        null,
                        () -> new ClientProxy(RestJsonClientManager.getInstance().getHttpClient(true), false)
                );
            } catch (Exception e) {
                LOG.error("Error getting sample volume info from {} for renderedOctree at {}", url, renderedOctreeUri, e);
                throw new IllegalStateException(e);
            } finally {
                getMethod.releaseConnection();
            }
        } else {
            renderedVolumeLocation = new FileBasedRenderedVolumeLocation(Paths.get(renderedOctreeUri), p -> Paths.get(OsFilePathRemapper.remapLinuxPath(p.toString())));
            renderedVolumeMetadata = nttc.getRenderedVolumeLoader().loadVolume(renderedVolumeLocation).orElseThrow(() -> new IllegalStateException("No rendering information found for " + renderedVolumeLocation.getDataStorageURI()));
        }

        return new RenderedVolume(renderedVolumeLocation, renderedVolumeMetadata);
    }

    private KtxOctreeBlockTileSource createKtxSource(URL renderedOctreeUrl, TmSample sample) {
        KtxOctreeBlockTileSource previousSource = nttc.getKtxSource();
        if (previousSource != null) {
            LOG.trace("previousUrl: {}", previousSource.getOriginatingSampleURL());
            if (Objects.equal(renderedOctreeUrl, previousSource.getOriginatingSampleURL())) {
                return previousSource; // Source did not change
            }
        }
        return new KtxOctreeBlockTileSource(renderedOctreeUrl, nttc.getTileLoader()).init(sample);
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
