package org.janelia.it.FlyWorkstation.gui.viewer3d.gui_elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import java.awt.*;
import java.nio.IntBuffer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 5/31/13
 * Time: 10:03 AM
 *
 * This minimal-footprint widget exists only so it can be queried for information off the graphics card.
 */
public class GpuSampler implements GLEventListener {
    private static final int GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX = 0x9049;   // NVidia
    private static final int TEXTURE_FREE_MEMORY_ATI = 0x87FC;                        // Radeon

    private static final int NO_ESTIMATE = 0;
    private static final int WAIT_TIME_MS = 100;
    private static int MAX_WAIT_LOOPS = 2000 / WAIT_TIME_MS; // Up to 2 seconds.

    private Logger logger = LoggerFactory.getLogger( GpuSampler.class );

    private int freeTexMem = 0;
    private AtomicBoolean isInitialized = new AtomicBoolean( false );
    private Color camoColor;

    public GpuSampler( Color camoColor ) {
        this.camoColor = camoColor;
    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        GL2 gl = glAutoDrawable.getGL().getGL2();

        freeTexMem = getFreeTextureMemory( gl );
        isInitialized.set( true );
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
    }

    @Override
    public void display(GLAutoDrawable gl) {
        GL2 gl2 = gl.getGL().getGL2();
        gl2.glClearColor( camoColor.getRed() / 255.0f, camoColor.getGreen() / 255.0f, camoColor.getBlue() / 255.0f, 1.0f );
        gl2.glClear( GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_STENCIL_BUFFER_BIT );
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int i, int i2, int i3, int i4) {
    }

    /**
     * Return the free texture memory found below.
     * @return the estimate, when it becomes ready.
     */
    public Future<Integer> getEstimatedTextureMemory() {
        FutureTask<Integer> future =
                new FutureTask<Integer>(new Callable<Integer>() {
                    public Integer call() {
                        int numLoops = 0;
                        while ( ! isInitialized.get() ) {
                            try {
                                Thread.sleep( WAIT_TIME_MS );
                                numLoops ++;
                                if ( numLoops > MAX_WAIT_LOOPS ) {
                                    logger.warn( "Exceeded max wait time to estimate texture memory.  Returning the 0.");
                                    return NO_ESTIMATE;
                                }
                            } catch ( Exception ex ) {
                                logger.error( "Failed to obtain free texture memory estimate.  Returning the 0.");
                                ex.printStackTrace();
                                return NO_ESTIMATE;
                            }
                        }
                        return freeTexMem;
                    }
                }
        );
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(future);
        return future;
    }

    /**
     * Attempt to determine how much memory is free for use with textures.
     *
     * @param gl descriptor for OpenGL calls.
     * @return value obtained from either NVidea or Radeon call.
     */
    private int getFreeTextureMemory(GL2 gl) {
        /*
           This technique is derived from information available at these sources:

            http://www.opengl.org/registry/specs/ATI/meminfo.txt
            http://developer.download.nvidia.com/opengl/specs/GL_NVX_gpu_memory_info.txt
         */
        gl.glGetError(); // Clear any old errors.
        int rtnVal = Integer.MAX_VALUE;  // Default to max, in case neither returns.  No constraints against unknowns.
        IntBuffer rtnBuf = IntBuffer.allocate( 4 ); // Max required, under Radeon.
        gl.glGetIntegerv(GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX, rtnBuf);
        int errnum = gl.glGetError();
        if ( errnum == 0 ) {
            rtnVal = rtnBuf.array()[ 0 ];
        }
        else {
            rtnBuf.rewind();
            gl.glGetIntegerv(TEXTURE_FREE_MEMORY_ATI, rtnBuf);
            errnum = gl.glGetError();
            if ( errnum == 0 ) {
                rtnVal = rtnBuf.array()[ 0 ];
            }
            else {
                logger.warn( "Neither NVidea nor Radeon video memory calls succeeded {}/{}.", errnum, rtnBuf.array()[0] );
            }
        }

        return rtnVal;
    }


}
