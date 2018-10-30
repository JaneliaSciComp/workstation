package org.janelia.it.workstation.gui.large_volume_viewer;

import com.google.common.cache.CacheLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.janelia.it.jacs.shared.lvv.AbstractTextureLoadAdapter.TileLoadError;
import org.janelia.it.jacs.shared.lvv.BlockTiffOctreeLoadAdapter;
import org.janelia.it.jacs.shared.lvv.FileBasedOctreeMetadataSniffer;
import org.janelia.it.jacs.shared.lvv.TextureData2d;
import org.janelia.it.jacs.shared.lvv.TileIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalFileTileCacheLoader extends CacheLoader<TileIndex, Optional<TextureData2d>> {

    private static Logger LOG = LoggerFactory.getLogger(LocalFileTileCacheLoader.class);

    private final BlockTiffOctreeLoadAdapter tileLoader;
    private final Path localTilesCacheDir;

    public LocalFileTileCacheLoader(BlockTiffOctreeLoadAdapter tileLoader) {
        this.tileLoader = tileLoader;
        this.localTilesCacheDir = Paths.get(""); // FIXME !!!!!!!!!!
    }

    @Override
    public Optional<TextureData2d> load(TileIndex tileIndex) throws Exception {
        Path sliceImagePath = getSliceImagePathFromTileIndex(tileIndex);
        if (Files.exists(sliceImagePath)) {
            try {
                return Optional.of(new TextureData2d(Files.readAllBytes(sliceImagePath)));
            } catch (IOException e) {
                LOG.error("Error loading tile {} from {}", tileIndex, sliceImagePath, e);
                throw new TileLoadError(e);
            }
        } else {
            TextureData2d sliceImage = tileLoader.loadToRam(tileIndex);
            if (sliceImage != null) {
                try {
                    Files.write(sliceImagePath, sliceImage.copyToByteArray());
                } catch (IOException e) {
                    LOG.error("Error caching tile {} locally to {}", tileIndex, sliceImagePath, e);
                }
                return Optional.of(sliceImage);
            } else {
                return Optional.empty();
            }
        }
    }

    private Path getSliceImagePathFromTileIndex(TileIndex tileIndex) {
        Path relativeSlicePath = FileBasedOctreeMetadataSniffer.getOctreeFilePath(tileIndex, tileLoader.getTileFormat());
        int sliceNumber = getSliceNumberFromTileIndex(tileIndex);
        Path slicePath = localTilesCacheDir.resolve(relativeSlicePath);
        return slicePath.resolve(sliceNumber + ".texture");
    }

    private int getSliceNumberFromTileIndex(TileIndex tileIndex) {
        int zoomScale = (int) Math.pow(2, tileIndex.getZoom());
        int axisIx = tileIndex.getSliceAxis().index();
        int tileDepth = tileLoader.getTileFormat().getTileSize()[axisIx];
        int absoluteSlice = (tileIndex.getCoordinate(axisIx)) / zoomScale;
        int relativeSlice = absoluteSlice % tileDepth;
        if (axisIx == 1) {
            // Raveller y is flipped so flip when slicing in Y (right?)
            relativeSlice = tileDepth - relativeSlice - 1;
        }
        return relativeSlice;
    }

}
