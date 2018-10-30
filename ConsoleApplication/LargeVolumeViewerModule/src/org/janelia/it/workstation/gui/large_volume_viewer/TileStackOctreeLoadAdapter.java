package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.jacs.shared.lvv.AbstractTextureLoadAdapter;
import org.janelia.it.jacs.shared.lvv.BlockTiffOctreeLoadAdapter;
import org.janelia.it.jacs.shared.lvv.FileBasedBlockTiffOctreeLoadAdapter;
import org.janelia.it.jacs.shared.lvv.RestServiceBasedBlockTiffOctreeLoadAdapter;
import org.janelia.it.jacs.shared.lvv.TextureData2d;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.jacs.shared.lvv.TileIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * Created by murphys on 11/6/2015.
 */

/**
 * Created by murphys on 10/22/2015.
 */
public class TileStackOctreeLoadAdapter extends BlockTiffOctreeLoadAdapter {

    BlockTiffOctreeLoadAdapter blockTiffOctreeLoadAdapter;

    TileStackOctreeLoadAdapter(TileFormat tileFormat, URI baseURI) {
        super(tileFormat, baseURI);
        if (baseURI.getScheme().startsWith("file")) {
            blockTiffOctreeLoadAdapter = new FileBasedBlockTiffOctreeLoadAdapter(tileFormat, baseURI);
        } else if (baseURI.getScheme().startsWith("http")) {
            blockTiffOctreeLoadAdapter = new RestServiceBasedBlockTiffOctreeLoadAdapter(tileFormat, baseURI);
        } else {
            throw new IllegalArgumentException("Don't know how to load " + baseURI);
        }
    }

    @Override
    public void loadMetadata() {
        blockTiffOctreeLoadAdapter.loadMetadata();
    }

    @Override
    public TextureData2dGL loadToRam(TileIndex tileIndex) throws TileLoadError, MissingTileException {
        long startTime = System.nanoTime();
        TextureData2d textureData2d = blockTiffOctreeLoadAdapter.loadToRam(tileIndex);
        if (textureData2d != null) {
            return new TextureData2dGL(textureData2d);
        } else {
            return null;
        }
    }
}
