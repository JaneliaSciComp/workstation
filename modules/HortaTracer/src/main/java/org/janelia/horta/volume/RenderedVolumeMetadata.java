package org.janelia.horta.volume;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.janelia.rendering.RenderingType;
import org.janelia.rendering.TileInfo;

class RenderedVolumeMetadata {
    @JsonProperty
    String baseURI;
    @JsonProperty
    String volumeBasePath;
    @JsonProperty
    RenderingType renderingType;
    @JsonProperty
    int[] originVoxel;
    @JsonProperty
    int[] volumeSizeInVoxels;
    @JsonProperty
    double[] micromsPerVoxel;
    @JsonProperty
    int numZoomLevels;
    @JsonProperty
    TileInfo xyTileInfo;
    @JsonProperty
    TileInfo yzTileInfo;
    @JsonProperty
    TileInfo zxTileInfo;
}
