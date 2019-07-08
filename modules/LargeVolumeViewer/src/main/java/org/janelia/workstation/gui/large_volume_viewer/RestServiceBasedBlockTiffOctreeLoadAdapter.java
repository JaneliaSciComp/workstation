package org.janelia.workstation.gui.large_volume_viewer;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.janelia.console.viewerapi.CachedRenderedVolumeLocation;
import org.janelia.it.jacs.shared.utils.HttpClientHelper;
import org.janelia.model.security.AppAuthorization;
import org.janelia.rendering.CachedRenderedVolumeLoader;
import org.janelia.rendering.JADEBasedRenderedVolumeLocation;
import org.janelia.rendering.RenderedVolume;
import org.janelia.rendering.RenderedVolumeLoader;
import org.janelia.rendering.RenderedVolumeLoaderImpl;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.rendering.RenderedVolumeMetadata;
import org.janelia.rendering.RenderingType;
import org.janelia.rendering.TileInfo;
import org.janelia.rendering.TileKey;
import org.janelia.rendering.utils.HttpClientProvider;
import org.janelia.workstation.core.api.LocalPreferenceMgr;
import org.janelia.workstation.core.util.ConsoleProperties;
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
    private RenderedVolume renderedVolume;

    RestServiceBasedBlockTiffOctreeLoadAdapter(TileFormat tileFormat,
                                               URI volumeBaseURI,
                                               AppAuthorization appAuthorization,
                                               int volumeCacheSize,
                                               int tileCacheSize) {
        super(tileFormat, volumeBaseURI);
        this.appAuthorization = appAuthorization;
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.renderedVolumeLoader = new CachedRenderedVolumeLoader(new RenderedVolumeLoaderImpl(), volumeCacheSize, tileCacheSize);
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
            RenderedVolumeMetadata renderedVolumeMetadata = objectMapper.readValue(strData, RenderedVolumeMetadata.class);
            RenderedVolumeLocation rvl = new CachedRenderedVolumeLocation(
                    new JADEBasedRenderedVolumeLocation(
                            renderedVolumeMetadata.getConnectionURI(),
                            renderedVolumeMetadata.getDataStorageURI(),
                            renderedVolumeMetadata.getVolumeBasePath(),
                            appAuthorization.getAuthenticationToken(),
                            null,
                            new HttpClientProvider() {
                                public Client getClient() {
                                    Client client = ClientBuilder.newClient();
                                    JacksonJsonProvider provider = new JacksonJaxbJsonProvider()
                                            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                                    client.register(provider);
                                    return client;
                                }
                            }
                    ),
                    LocalPreferenceMgr.getInstance().getLocalFileCacheStorage());

            this.renderedVolume = new RenderedVolume(
                    rvl,
                    renderedVolumeMetadata.getRenderingType(),
                    renderedVolumeMetadata.getOriginVoxel(),
                    renderedVolumeMetadata.getVolumeSizeInVoxels(),
                    renderedVolumeMetadata.getMicromsPerVoxel(),
                    renderedVolumeMetadata.getNumZoomLevels(),
                    renderedVolumeMetadata.getXyTileInfo(),
                    renderedVolumeMetadata.getYzTileInfo(),
                    renderedVolumeMetadata.getZxTileInfo()
            );

            getTileFormat().setDefaultParameters();
            getTileFormat().setZoomLevelCount(renderedVolumeMetadata.getNumZoomLevels());
            getTileFormat().setVolumeSize(renderedVolumeMetadata.getVolumeSizeInVoxels());
            getTileFormat().setVoxelMicrometers(renderedVolumeMetadata.getMicromsPerVoxel());
            getTileFormat().setOrigin(renderedVolumeMetadata.getOriginVoxel());
            if (renderedVolumeMetadata.getYzTileInfo() != null) {
                getTileFormat().setHasXSlices(true);
                updateTileFormatFromTileInfo(renderedVolumeMetadata.getYzTileInfo());
            }
            if (renderedVolumeMetadata.getZxTileInfo() != null) {
                getTileFormat().setHasYSlices(true);
                updateTileFormatFromTileInfo(renderedVolumeMetadata.getZxTileInfo());
            }
            if (renderedVolumeMetadata.getXyTileInfo() != null) {
                getTileFormat().setHasZSlices(true);
                updateTileFormatFromTileInfo(renderedVolumeMetadata.getXyTileInfo());
            }
        } catch (Exception ex) {
            LOG.error("Error getting sample 2d tile from {}", url, ex);
            throw new IllegalStateException(ex);
        } finally {
            getMethod.releaseConnection();
        }
    }

    private void updateTileFormatFromTileInfo(TileInfo tileInfo) {
        getTileFormat().setChannelCount(tileInfo.getChannelCount());
        getTileFormat().setTileSize(tileInfo.getVolumeSize());
        getTileFormat().setSrgb(tileInfo.isSrgb());
        getTileFormat().setBitDepth(tileInfo.getBitDepth());
        getTileFormat().setIntensityMax((int) Math.pow(2, tileInfo.getBitDepth()) - 1);
    }

    @Override
    public TextureData2d loadToRam(TileIndex tileIndex)
            throws TileLoadError {
        try {
            TileInfo tileInfo = getTileInfo(tileIndex);
            TileKey tileKey = TileKey.fromRavelerTileCoord(tileIndex.getX(), tileIndex.getY(), tileIndex.getZ(),
                    tileIndex.getZoom(),
                    tileInfo.getSliceAxis(),
                    tileInfo);
            LOG.debug("Load tile {} using key {}", tileIndex, tileKey);
            return renderedVolumeLoader.loadSlice(this.renderedVolume, tileKey)
                    .map(TextureData2d::new)
                    .orElse(null);
        } catch (Exception ex) {
            LOG.error("Error getting sample 2d tile from {}", this.renderedVolume.getDataStorageURI(), ex);
            throw new TileLoadError(ex);
        }
    }

    private TileInfo getTileInfo(TileIndex tileIndex) {
        switch (tileIndex.getSliceAxis()) {
            case X:
                return this.renderedVolume.getYzTileInfo();
            case Y:
                return this.renderedVolume.getZxTileInfo();
            case Z:
                return this.renderedVolume.getXyTileInfo();
            default:
                throw new IllegalArgumentException("Unknown slice axis in " + tileIndex);
        }
    }
}
