package org.janelia.workstation.gui.large_volume_viewer;

import java.net.URI;

abstract class TileLoaderProvider {
    abstract BlockTiffOctreeLoadAdapter createLoadAdapter(String baseURI);
}
