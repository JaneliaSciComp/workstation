package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.TextureBuilderI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import java.util.concurrent.CyclicBarrier;

/**
 * Simply makes a texture builder build its texture, inside a thread.  Once complete, the getter for the
 * texture data may be called.  Not before.
 */
public class TexBuildRunnable implements Runnable {
    private TextureBuilderI textureBuilder;
    private CyclicBarrier barrier;
    private TextureDataI textureData;

    public TexBuildRunnable( TextureBuilderI textureBuilder, CyclicBarrier barrier ) {
        this.textureBuilder = textureBuilder;
        this.barrier = barrier;
    }

    public TextureDataI getTextureData() {
        return textureData;
    }

    public void run() {
        try {
            textureData = textureBuilder.buildTextureData();
            barrier.await();
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }
}

