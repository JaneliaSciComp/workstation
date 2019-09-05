package org.janelia.horta;

import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Objects;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.methods.GetMethod;
import org.janelia.console.viewerapi.CachedRenderedVolumeLocation;
import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.ViewerLocationAcceptor;
import org.janelia.filecacheutils.LocalFileCacheStorage;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import org.janelia.horta.blocks.KtxOctreeBlockTileSource;
import org.janelia.horta.blocks.RenderedVolumeKtxOctreeBlockTileSource;
import org.janelia.horta.volume.RenderedVolumeBrickSource;
import org.janelia.horta.volume.StaticVolumeBrickSource;
import org.janelia.it.jacs.shared.utils.HttpClientHelper;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.security.AppAuthorization;
import org.janelia.rendering.FileBasedRenderedVolumeLocation;
import org.janelia.rendering.JADEBasedRenderedVolumeLocation;
import org.janelia.rendering.RenderedVolume;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.rendering.RenderedVolumeMetadata;
import org.janelia.rendering.utils.ClientProxy;
import org.janelia.scenewindow.SceneWindow;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.LocalCacheMgr;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleLocationAcceptor implements ViewerLocationAcceptor {
    private static final Logger LOG = LoggerFactory.getLogger(SampleLocationAcceptor.class);
    private static final HttpClientHelper HTTP_HELPER = new HttpClientHelper();
    private static final int CACHE_CONCURRENCY = 10;

    private final NeuronTraceLoader loader;
    private final NeuronTracerTopComponent nttc;
    private final SceneWindow sceneWindow;
    private final ObjectMapper objectMapper;


    SampleLocationAcceptor(NeuronTraceLoader loader,
                           NeuronTracerTopComponent nttc,
                           SceneWindow sceneWindow) {
        this.loader = loader;
        this.nttc = nttc;
        this.sceneWindow = sceneWindow;
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void acceptLocation(final SampleLocation sampleLocation) throws Exception {
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
                    URL url = sampleLocation.getSampleUrl();
                    TmSample sample = sampleLocation.getSample();

                    // trying to run down a bug:
                    if (sample == null) {
                        LOG.info("found null sample for sample url " + url + " at coordinates "
                                + sampleLocation.getFocusXUm() + ", "
                                + sampleLocation.getFocusYUm() + ", "
                                + sampleLocation.getFocusZUm());
                    }
                    RenderedVolume renderedVolume = getVolumeInfo(url.toURI());

                    if (nttc.isPreferKtx()) {
                        // for KTX tile the camera must be set before the tiles are loaded in order for them to be displayed first time
                        progress.setDisplayName("Centering on location...");
                        setCameraLocation(sampleLocation);
                        // use ktx tiles
                        KtxOctreeBlockTileSource ktxSource = createKtxSource(renderedVolume, url, sample);
                        nttc.setKtxSource(ktxSource);
                        // start loading ktx tiles
                        progress.switchToIndeterminate(); // TODO: enhance tile loading with a progress listener
                        progress.setDisplayName("Loading KTX brain tile image...");
                        if (nttc.doesUpdateVolumeCache()) {
                            loader.loadTransientKtxTileAtCurrentFocus(nttc.getKtxSource());
                        } else {
                            loader.loadPersistentKtxTileAtCurrentFocus(nttc.getKtxSource());
                        }
                    } else {
                        // use raw tiles, which are handled by the StaticVolumeBrickSource
                        StaticVolumeBrickSource volumeBrickSource = createStaticVolumeBrickSource(renderedVolume, sample, progress);
                        nttc.setVolumeSource(volumeBrickSource);
                        // start loading raw tiles
                        progress.switchToIndeterminate();
                        progress.setDisplayName("Loading brain tile image...");
                        loader.loadTileAtCurrentFocus(volumeBrickSource, sampleLocation.getDefaultColorChannel());
                        // for raw tiles the camera needs to be set after the tile began loading in order to trigger the display
                        progress.setDisplayName("Centering on location...");
                        setCameraLocation(sampleLocation);
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
                } finally {
                    progress.finish();
                }
            }
        };
        RequestProcessor.getDefault().post(task);
    }

    private RenderedVolume getVolumeInfo(URI renderedOctreeUri) {
        RenderedVolumeLocation renderedVolumeLocation;
        RenderedVolumeMetadata renderedVolumeMetadata;
        if (ApplicationOptions.getInstance().isUseHTTPForTileAccess()) {
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
                LOG.error("Error getting sample volume info from {}", url, e);
                throw new IllegalStateException(e);
            } finally {
                getMethod.releaseConnection();
            }
        } else {
            renderedVolumeLocation = new FileBasedRenderedVolumeLocation(Paths.get(renderedOctreeUri));
            renderedVolumeMetadata = nttc.getRenderedVolumeLoader().loadVolume(renderedVolumeLocation).orElseThrow(() -> new IllegalStateException("No rendering information found for " + renderedVolumeLocation.getDataStorageURI()));
        }

        return new RenderedVolume(
                new CachedRenderedVolumeLocation(
                        renderedVolumeLocation,
                        LocalCacheMgr.getInstance().getLocalFileCacheStorage(),
                        CACHE_CONCURRENCY,
                        Executors.newFixedThreadPool(
                                CACHE_CONCURRENCY,
                                new ThreadFactoryBuilder()
                                        .setNameFormat("HortaTileCacheWriter-%d")
                                        .setDaemon(true)
                                        .build())),
                renderedVolumeMetadata);
    }

    private KtxOctreeBlockTileSource createKtxSource(RenderedVolume renderedVolume, URL renderedOctreeUrl, TmSample sample) {
        KtxOctreeBlockTileSource previousSource = nttc.getKtxSource();
        if (previousSource != null) {
            LOG.trace("previousUrl: {}", previousSource.getOriginatingSampleURL());
            if (Objects.equal(renderedOctreeUrl, previousSource.getOriginatingSampleURL())) {
                return previousSource; // Source did not change
            }
        }
        return new RenderedVolumeKtxOctreeBlockTileSource(renderedVolume.getVolumeLocation(), renderedOctreeUrl).init(sample);
    }

    private StaticVolumeBrickSource createStaticVolumeBrickSource(RenderedVolume renderedVolume, TmSample sample, ProgressHandle progress) {
        progress.switchToDeterminate(100);
        RawTileLoader rawTileLoader;
        if (ApplicationOptions.getInstance().isUseHTTPForTileAccess()) {
            rawTileLoader = new CachedRawTileLoader(
                    new JadeBasedRawTileLoader(new JadeServiceClient(ConsoleProperties.getString("jadestorage.rest.url"), () -> new ClientProxy(RestJsonClientManager.getInstance().getHttpClient(true), false))),
                    LocalCacheMgr.getInstance().getLocalFileCacheStorage(),
                    CACHE_CONCURRENCY,
                    Executors.newFixedThreadPool(
                            CACHE_CONCURRENCY,
                            new ThreadFactoryBuilder()
                                    .setNameFormat("HortaTileCacheWriter-%d")
                                    .setDaemon(true)
                                    .build())
            );
        } else {
            rawTileLoader = new FileBasedRawTileLoader();
        }
        return new RenderedVolumeBrickSource(nttc.getRenderedVolumeLoader(), renderedVolume, rawTileLoader).init(sample, progress::progress);
    }

    private void setCameraLocation(SampleLocation sampleLocation) {
        // Now, position this component over other component's focus.
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
            v.setDefaultSceneUnitsPerViewportHeight((float) zoom);
        }

        v.notifyObservers();
    }

}
