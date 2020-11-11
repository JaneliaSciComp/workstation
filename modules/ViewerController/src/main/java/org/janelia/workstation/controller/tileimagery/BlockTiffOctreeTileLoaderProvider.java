package org.janelia.workstation.controller.tileimagery;

public abstract class BlockTiffOctreeTileLoaderProvider {
    public abstract BlockTiffOctreeLoadAdapter createLoadAdapter(String baseURI);
}
