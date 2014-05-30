package org.janelia.it.workstation.gui.alignment_board_viewer;

import org.janelia.it.workstation.gui.alignment_board_viewer.masking.TextureBuilderI;
import org.janelia.it.workstation.gui.viewer3d.texture.TextureDataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CyclicBarrier;

/**
 * Simply makes a texture builder build its texture, inside a thread.  Once complete, the getter for the
 * texture data may be called.  Not before.
 */
public class TexBuildRunnable implements Runnable {
    private TextureBuilderI textureBuilder;
    private CyclicBarrier barrier;
    private TextureDataI textureData;
    private Logger logger = LoggerFactory.getLogger( TexBuildRunnable.class );

    public TexBuildRunnable( TextureBuilderI textureBuilder, CyclicBarrier barrier ) {
        this.textureBuilder = textureBuilder;
        this.barrier = barrier;
    }

    public TextureDataI getTextureData() {
        return textureData;
    }

    public void run() {
        try {
            logger.info( "About to build texture data." );
            textureData = textureBuilder.buildTextureData();
            logger.info( "Awaiting barrier." );
            barrier.await();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            barrier.reset();  // signals the problem through BrokenBarrierException, to others awaiting.
        }
    }
}

