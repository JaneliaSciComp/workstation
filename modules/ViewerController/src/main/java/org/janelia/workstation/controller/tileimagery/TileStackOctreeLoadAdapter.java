package org.janelia.workstation.controller.tileimagery;

import java.net.URI;

import org.janelia.jacsstorage.clients.api.JadeStorageAttributes;
import org.janelia.workstation.core.api.AccessManager;

/**
 * Created by murphys on 11/6/2015.
 */

/**
 * Created by murphys on 10/22/2015.
 */
public class TileStackOctreeLoadAdapter extends BlockTiffOctreeLoadAdapter {

    private final BlockTiffOctreeLoadAdapter blockTiffOctreeLoadAdapter;

    public TileStackOctreeLoadAdapter(TileFormat tileFormat, URI baseURI, int concurrency, JadeStorageAttributes storageAttributes) {
        super(tileFormat, baseURI);
        if (baseURI.getScheme().startsWith("file")) {
            blockTiffOctreeLoadAdapter = new FileBasedBlockTiffOctreeLoadAdapter(tileFormat, baseURI, concurrency);
        } else if (baseURI.getScheme().startsWith("http")) {
            blockTiffOctreeLoadAdapter = new RestServiceBasedBlockTiffOctreeLoadAdapter(tileFormat, baseURI, concurrency, storageAttributes, AccessManager.getAccessManager().getAppAuthorization());
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
        TextureData2d textureData2d = blockTiffOctreeLoadAdapter.loadToRam(tileIndex);
        if (textureData2d != null) {
            return new TextureData2dGL(textureData2d);
        } else {
            return null;
        }
    }
}
