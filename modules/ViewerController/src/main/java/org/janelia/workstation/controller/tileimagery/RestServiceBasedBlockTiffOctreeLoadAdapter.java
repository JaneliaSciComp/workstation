package org.janelia.workstation.controller.tileimagery;

import java.net.URI;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.janelia.console.viewerapi.CachedRenderedVolumeLocation;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.utils.HttpClientHelper;
import org.janelia.model.security.AppAuthorization;
import org.janelia.rendering.JADEBasedRenderedVolumeLocation;
import org.janelia.rendering.RenderedVolumeLoader;
import org.janelia.rendering.RenderedVolumeLoaderImpl;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.rendering.RenderedVolumeMetadata;
import org.janelia.rendering.TileInfo;
import org.janelia.rendering.TileKey;
import org.janelia.rendering.utils.ClientProxy;
import org.janelia.workstation.core.api.LocalCacheMgr;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Loader for large volume viewer format negotiated with Nathan Clack
 * March 21, 2013.
 * 512x512 tiles
 * Z-order octree folder layout
 * uncompressed tiff stack for each set of slices
 * named like "default.0.tif" for channel zero
 * 16-bit unsigned int
 * intensity range 0-65535
 */
public class RestServiceBasedBlockTiffOctreeLoadAdapter extends BlockTiffOctreeLoadAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(RestServiceBasedBlockTiffOctreeLoadAdapter.class);
    private static final HttpClientHelper HTTP_CLIENT_HELPER = new HttpClientHelper();

    // Metadata: file location required for local system as mount point.
    private final ObjectMapper objectMapper;
    private final AppAuthorization appAuthorization;
    private final RenderedVolumeLoader renderedVolumeLoader;
    private final int concurrency;
    private RenderedVolumeLocation renderedVolumeLocation;
    private RenderedVolumeMetadata renderedVolumeMetadata;

    RestServiceBasedBlockTiffOctreeLoadAdapter(TileFormat tileFormat,
                                               URI volumeBaseURI,
                                               int concurrency,
                                               AppAuthorization appAuthorization) {
        super(tileFormat, volumeBaseURI);
        this.appAuthorization = appAuthorization;
        this.renderedVolumeLoader = new RenderedVolumeLoaderImpl();
        this.concurrency = concurrency;
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void loadMetadata() {
        String url = getVolumeBaseURI().resolve("volume_info").toString();
        LOG.trace("Getting volume metadata from: {}", url);

        GetMethod getMethod = new GetMethod(url);
        getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

        try {
            int statusCode = HTTP_CLIENT_HELPER.executeMethod(getMethod, appAuthorization);
            if (statusCode != HttpStatus.SC_OK) {
                throw new IllegalStateException("HTTP status " + statusCode + " (not OK) from url " + url);
            }
            String strData = getMethod.getResponseBodyAsString();
            renderedVolumeMetadata = objectMapper.readValue(strData, RenderedVolumeMetadata.class);
            renderedVolumeLocation = new CachedRenderedVolumeLocation(
                    new JADEBasedRenderedVolumeLocation(
                            renderedVolumeMetadata.getConnectionURI(),
                            renderedVolumeMetadata.getDataStorageURI(),
                            renderedVolumeMetadata.getVolumeBasePath(),
                            appAuthorization.getAuthenticationToken(),
                            null,
                            () -> new ClientProxy(RestJsonClientManager.getInstance().getHttpClient(true), false)
                    ),
                    LocalCacheMgr.getInstance().getLocalFileCacheStorage(),
                    concurrency,
                    Executors.newFixedThreadPool(
                            concurrency,
                            new ThreadFactoryBuilder()
                                    .setNameFormat("RestBasedOctreeCacheWriter-%d")
                                    .setDaemon(true)
                                    .build()));
            getTileFormat().initializeFromRenderedVolumeMetadata(renderedVolumeMetadata);
        } catch (Exception ex) {
            LOG.error("Error getting sample 2d tile from {}", url, ex);
            throw new IllegalStateException(ex);
        } finally {
            getMethod.releaseConnection();
        }
    }

    @Override
    public TextureData2d loadToRam(TileIndex tileIndex)
            throws TileLoadError {
        TileInfo tileInfo = getTileInfo(tileIndex);
        TileKey tileKey = TileKey.fromRavelerTileCoord(tileIndex.getX(), tileIndex.getY(), tileIndex.getZ(),
                tileIndex.getZoom(),
                tileInfo.getSliceAxis(),
                tileInfo);
        LOG.trace("Loading tile {} using key {}", tileIndex, tileKey);
        try {
            byte[] textureBytes =renderedVolumeLoader.loadSlice(renderedVolumeLocation, renderedVolumeMetadata, tileKey)
                    .getContent();
            return textureBytes != null ? new TextureData2d(textureBytes) : null;
        } catch (Exception ex) {
            LOG.error("Error getting sample 2d tile {} based on tileIndex {} using http based on {}", tileKey, tileIndex, renderedVolumeMetadata.getDataStorageURI(), ex);
            throw new TileLoadError(ex);
        }
    }

    private TileInfo getTileInfo(TileIndex tileIndex) {
        switch (tileIndex.getSliceAxis()) {
            case X:
                return renderedVolumeMetadata.getYzTileInfo();
            case Y:
                return renderedVolumeMetadata.getZxTileInfo();
            case Z:
                return renderedVolumeMetadata.getXyTileInfo();
            default:
                throw new IllegalArgumentException("Unknown slice axis in " + tileIndex);
        }
    }
}
