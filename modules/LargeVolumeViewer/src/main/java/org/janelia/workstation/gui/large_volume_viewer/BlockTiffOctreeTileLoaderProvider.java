package org.janelia.workstation.gui.large_volume_viewer;

abstract class BlockTiffOctreeTileLoaderProvider {
    abstract BlockTiffOctreeLoadAdapter createLoadAdapter(String baseURI);
}
