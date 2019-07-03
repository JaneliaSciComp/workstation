package org.janelia.workstation.gui.large_volume_viewer;

import java.net.URI;

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
public abstract class BlockTiffOctreeLoadAdapter extends AbstractTextureLoadAdapter {

    private final URI volumeBaseURI;

    public BlockTiffOctreeLoadAdapter(TileFormat tileFormat, URI volumeBaseURI) {
        super(tileFormat);
        this.volumeBaseURI = volumeBaseURI;
        tileFormat.setIndexStyle(TileIndex.IndexStyle.OCTREE);
    }

    public URI getVolumeBaseURI() {
        return volumeBaseURI;
    }

    public abstract void loadMetadata();

    public int getSliceSize() {
        return getTileFormat().getTileSize()[0] * getTileFormat().getTileSize()[1] * getTileFormat().getBitDepth() / 8;
    }
}
