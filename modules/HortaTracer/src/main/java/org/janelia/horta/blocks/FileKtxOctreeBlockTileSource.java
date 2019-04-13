package org.janelia.horta.blocks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.janelia.model.domain.tiledMicroscope.TmSample;

/**
 * @author brunsc
 */
public class FileKtxOctreeBlockTileSource extends KtxOctreeBlockTileSource {

    FileKtxOctreeBlockTileSource(URL originatingSampleURL) {
        super(originatingSampleURL);
    }

    @Override
    protected String getSourceServerURL(TmSample sample) {
        return originatingSampleURL.toString();
    }
 
    protected URI getKeyBlockPathURI(KtxOctreeBlockTileKey key) {
        try {
            return originatingSampleURL.toURI()
                    .resolve(getKtxSubDir())
                    .resolve(key.getKeyPath())
                    .resolve(key.getKeyBlockName("_8_xy_"))
                    ;
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected InputStream streamKeyBlock(KtxOctreeBlockTileKey octreeKey) {
        try {
            return Files.newInputStream(Paths.get(getKeyBlockPathURI(octreeKey)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
