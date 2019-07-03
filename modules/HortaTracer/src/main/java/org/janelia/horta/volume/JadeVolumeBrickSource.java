package org.janelia.horta.volume;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.janelia.horta.BrainTileInfo;
import org.janelia.horta.BrainTileInfoBuilder;
import org.janelia.it.jacs.shared.utils.HttpClientHelper;
import org.janelia.model.security.AppAuthorization;
import org.janelia.rendering.JADEBasedRenderedVolumeLocation;
import org.janelia.rendering.RenderedVolume;
import org.janelia.rendering.RenderedVolumeLoader;
import org.janelia.rendering.RenderedVolumeMetadata;
import org.janelia.rendering.utils.HttpClientProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.util.Collection;

public class JadeVolumeBrickSource implements StaticVolumeBrickSource {
    private static final Logger LOG = LoggerFactory.getLogger(JadeVolumeBrickSource.class);
    private static final HttpClientHelper HTTP_HELPER = new HttpClientHelper();

    private final URI volumeBaseURI;
    private final AppAuthorization appAuthorization;
    private final RenderedVolumeLoader renderedVolumeLoader;
    private final ObjectMapper objectMapper;
    private RenderedVolume renderedVolume;
    private final Double resolution;
    private final BrickInfoSet brickInfoSet;

    public JadeVolumeBrickSource(RenderedVolumeLoader renderedVolumeLoader, URI volumeBaseURI, AppAuthorization appAuthorization, boolean leverageCompressedFiles) {
        this.volumeBaseURI = volumeBaseURI;
        this.appAuthorization = appAuthorization;
        this.renderedVolumeLoader = renderedVolumeLoader;
        this.objectMapper = (new ObjectMapper()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Pair<Double, BrickInfoSet> volumeBricksMetadata = loadVolumeBricksMetadata(leverageCompressedFiles);
        this.resolution = volumeBricksMetadata.getLeft();
        this.brickInfoSet = volumeBricksMetadata.getRight();
    }

    private Pair<Double, BrickInfoSet> loadVolumeBricksMetadata(boolean leverageCompressedFiles) {
        String url = volumeBaseURI.resolve("volume_info").toString();
        LOG.trace("Getting volume metadata from: {}", url);
        GetMethod getMethod = new GetMethod(url);
        getMethod.getParams().setParameter("http.method.retry-handler", new DefaultHttpMethodRetryHandler(3, false));
        try {
            int statusCode = HTTP_HELPER.executeMethod(getMethod, this.appAuthorization);
            if (statusCode != 200) {
                throw new IllegalStateException("HTTP status " + statusCode + " (not OK) from url " + url);
            }
            RenderedVolumeMetadata renderedVolumeMetadata = objectMapper.readValue(getMethod.getResponseBodyAsStream(), RenderedVolumeMetadata.class);
            this.renderedVolume = new RenderedVolume(
                    new JADEBasedRenderedVolumeLocation(
                            renderedVolumeMetadata.getConnectionURI(),
                            renderedVolumeMetadata.getDataStorageURI(),
                            renderedVolumeMetadata.getVolumeBasePath(),
                            this.appAuthorization.getAuthenticationToken(),
                            (String) null,
                            new HttpClientProvider() {
                                public Client getClient() {
                                    Client client = ClientBuilder.newClient();
                                    JacksonJsonProvider provider = (JacksonJsonProvider) ((JacksonJsonProvider) (new JacksonJaxbJsonProvider())
                                            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))
                                            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                                    client.register(provider);
                                    return client;
                                }
                            }),
                    renderedVolumeMetadata.getRenderingType(),
                    renderedVolumeMetadata.getOriginVoxel(),
                    renderedVolumeMetadata.getVolumeSizeInVoxels(),
                    renderedVolumeMetadata.getMicromsPerVoxel(),
                    renderedVolumeMetadata.getNumZoomLevels(),
                    renderedVolumeMetadata.getXyTileInfo(),
                    renderedVolumeMetadata.getYzTileInfo(),
                    renderedVolumeMetadata.getZxTileInfo());
            // There is no dynamic loading by resolution at the moment for raw tiles in yaml file
            // so treat all tiles as having the same resolution as the first tile
            return renderedVolumeLoader.loadVolumeRawImageTiles(renderedVolume.getRvl()).stream()
                    .map(rawImage -> BrainTileInfoBuilder.fromRawImage(renderedVolume.getRvl(), rawImage, leverageCompressedFiles))
                    .reduce(MutablePair.of(null, new BrickInfoSet()),
                            (Pair<Double, BrickInfoSet> res, BrainTileInfo brainTileInfo) -> {
                                res.getRight().add(brainTileInfo);
                                if (res.getLeft() == null) {
                                    return MutablePair.of(brainTileInfo.getResolutionMicrometers(), res.getRight());
                                } else {
                                    return res;
                                }
                            },
                            (r1, r2) -> {
                                r1.getRight().addAll(r2.getRight());
                                if (r1.getLeft() == null) {
                                    return MutablePair.of(r2.getLeft(), r1.getRight());
                                } else {
                                    return r1;
                                }
                            });
        } catch (Exception e) {
            LOG.error("Error getting sample volume info from {}", url, e);
            throw new IllegalStateException(e);
        } finally {
            getMethod.releaseConnection();
        }
    }

    @Override
    public Collection<Double> getAvailableResolutions() {
        return resolution != null ? ImmutableSet.of(resolution) : ImmutableSet.of();
    }

    @Override
    public BrickInfoSet getAllBrickInfoForResolution(Double resolution) {
        if (resolution == null || this.resolution == null || this.resolution == 0) {
            return null;
        }
        // since we considered the same resolution for the entire brick set we simply check whether
        // the requested resolution is within 30% of the current resolution
        // if it is we return the current brickset otherwise nothing (null)
        if (Math.abs(resolution - this.resolution) / this.resolution < 0.3) {
            return this.brickInfoSet;
        } else {
            return null;
        }
    }
}
