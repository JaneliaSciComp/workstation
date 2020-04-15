package org.janelia.workstation.controller.tileimagery;

abstract class BlockTiffOctreeTileLoaderProvider {
    abstract BlockTiffOctreeLoadAdapter createLoadAdapter(String baseURI);
}
