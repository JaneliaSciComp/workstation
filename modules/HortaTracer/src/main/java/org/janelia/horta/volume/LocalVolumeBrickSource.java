package org.janelia.horta.volume;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;

import org.janelia.console.viewerapi.OsFilePathRemapper;
import org.janelia.horta.BrainTileInfoBuilder;
import org.janelia.rendering.FileBasedRenderedVolumeLocation;
import org.janelia.rendering.RenderedVolumeLocation;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Christopher Bruns
 */
public class LocalVolumeBrickSource implements StaticVolumeBrickSource {
    private final Double resolution;
    private final BrickInfoSet brickInfoSet;

    public LocalVolumeBrickSource(URI volumeURI, InputStream yamlStream, boolean leverageCompressedFiles, Consumer<Integer> progressUpdater) {
        Yaml yaml = new Yaml();
        progressUpdater.accept(10);

        Object foo = yaml.load(yamlStream);
        Map<String, Object> tilebase = (Map<String, Object>)foo;
        progressUpdater.accept(20);

        String tilebasePath = (String) tilebase.get("path"); // this is the path read from the YAML file
        // Note that if the tiles are not read directly from the same (NFS) mountpoint the path must be mapped to the actual path
        String localBasePath = OsFilePathRemapper.remapLinuxPath(tilebasePath); // Convert to OS-specific file path
        progressUpdater.accept(25);

        List<Map<String, Object>> tiles = (List<Map<String, Object>>) tilebase.get("tiles");
        // Index tiles for easy retrieval
        int tileCount = 0;
        float totalTiles = tiles.size();
        Double tileResolution = null;

        RenderedVolumeLocation renderedVolumeLocation = new FileBasedRenderedVolumeLocation(Paths.get(volumeURI));

        brickInfoSet = new BrickInfoSet();
        // There is no dynamic loading by resolution at the moment for raw tiles in yaml file
        // so treat all tiles as having the same resolution as the first tile
        for (Map<String, Object> tile : tiles) {
            BrickInfo tileInfo = BrainTileInfoBuilder.fromYAMLFragment(renderedVolumeLocation, localBasePath, leverageCompressedFiles, tile);
            // Compute resolution
            // Jan 2017 CMB - Treat all tiles in YAML file as same resolution as the first tile.
            if (tileResolution == null) {
                tileResolution = tileInfo.getResolutionMicrometers();
            }
            brickInfoSet.add(tileInfo);
            tileCount += 1;
            int progressValue = (int)(25 + (90-25)*(tileCount/totalTiles));
            progressUpdater.accept(progressValue);
        }
        resolution = tileResolution;
        progressUpdater.accept(90);
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
