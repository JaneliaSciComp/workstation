package org.janelia.workstation.gui.large_volume_viewer;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.janelia.console.viewerapi.CachedRenderedVolumeLocation;
import org.janelia.rendering.FileBasedRenderedVolumeLocation;
import org.janelia.rendering.RenderedVolumeLoader;
import org.janelia.rendering.RenderedVolumeLoaderImpl;
import org.janelia.rendering.RenderedVolumeLocation;
import org.janelia.rendering.RenderedVolumeMetadata;
import org.janelia.rendering.TileInfo;
import org.janelia.rendering.TileKey;
import org.janelia.workstation.core.api.LocalCacheMgr;
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
public class FileBasedBlockTiffOctreeLoadAdapter extends BlockTiffOctreeLoadAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(FileBasedBlockTiffOctreeLoadAdapter.class);

    // Metadata: file location required for local system as mount point.
    private final Path baseFolder;
    private final RenderedVolumeLoader renderedVolumeLoader;
    private final RenderedVolumeLocation renderedVolumeLocation;
    private RenderedVolumeMetadata renderedVolumeMetadata;

    FileBasedBlockTiffOctreeLoadAdapter(TileFormat tileFormat, URI volumeBaseURI) {
        super(tileFormat, volumeBaseURI);
        this.baseFolder = Paths.get(volumeBaseURI);
        this.renderedVolumeLoader = new RenderedVolumeLoaderImpl();
        this.renderedVolumeLocation = new CachedRenderedVolumeLocation(
                new FileBasedRenderedVolumeLocation(baseFolder),
                LocalCacheMgr.getInstance().getLocalFileCacheStorage(),
                Executors.newFixedThreadPool(4,
                        new ThreadFactoryBuilder()
                                .setNameFormat("FileBasedOctreeCacheWriter-%d")
                                .setDaemon(true)
                                .build()));
    }

    @Override
    public void loadMetadata() {
        renderedVolumeMetadata = renderedVolumeLoader.loadVolume(renderedVolumeLocation).orElse(null);
        getTileFormat().initializeFromRenderedVolumeMetadata(renderedVolumeMetadata);
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
            LOG.debug("Load tile {} using key {} -> {}", tileIndex, tileKey, renderedVolumeMetadata.getRelativeTilePath(tileKey));
            return renderedVolumeLoader.loadSlice(renderedVolumeLocation, renderedVolumeMetadata, tileKey)
                    .flatMap(sc -> {
                        byte[] content = sc.getBytes();
                        return content == null ? Optional.empty() : Optional.of(content);
                    })
                    .map(TextureData2d::new)
                    .orElse(null);
        } catch (Exception ex) {
            LOG.error("Error getting sample 2d tile from {}", renderedVolumeMetadata.getDataStorageURI(), ex);
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
