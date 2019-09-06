package org.janelia.horta.volume;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.janelia.horta.BrainTileInfo;
import org.janelia.horta.BrainTileInfoBuilder;
import org.janelia.horta.TileLoader;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.rendering.RawImage;

public class RawVolumeBrickSource implements StaticVolumeBrickSource {

    private final TileLoader tileLoader;
    private Double resolution;
    private BrickInfoSet brickInfoSet;

    public RawVolumeBrickSource(TileLoader tileLoader) {
        this.tileLoader = tileLoader;
    }

    public RawVolumeBrickSource init(TmSample sample, List<RawImage> rawTiles, Consumer<Integer> progressUpdater) {
        Pair<Double, BrickInfoSet> volumeBricksMetadata = loadVolumeBricksMetadata(sample.getTwoPhotonAcquisitionFilepath(), rawTiles, progressUpdater);
        this.resolution = volumeBricksMetadata.getLeft();
        this.brickInfoSet = volumeBricksMetadata.getRight();
        return this;
    }

    private Pair<Double, BrickInfoSet> loadVolumeBricksMetadata(String acquisitionPath, List<RawImage> rawTiles, Consumer<Integer> progressUpdater) {
        // There is no dynamic loading by resolution at the moment for raw tiles in yaml file
        // so treat all tiles as having the same resolution as the first tile
        int totalRawImageTilesCount = rawTiles.size();
        progressUpdater.accept(25);
        return rawTiles.stream()
                .map(rawImage -> BrainTileInfoBuilder.fromRawImage(tileLoader, StringUtils.defaultIfBlank(acquisitionPath, rawImage.getAcquisitionPath()), rawImage))
                .reduce(MutablePair.of(null, new BrickInfoSet()),
                        (Pair<Double, BrickInfoSet> res, BrainTileInfo brainTileInfo) -> {
                            res.getRight().add(brainTileInfo);
                            int tileCount = res.getRight().size();
                            int progressValue = (int) (25 + (90 - 25) * (tileCount / totalRawImageTilesCount));
                            progressUpdater.accept(progressValue);
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
