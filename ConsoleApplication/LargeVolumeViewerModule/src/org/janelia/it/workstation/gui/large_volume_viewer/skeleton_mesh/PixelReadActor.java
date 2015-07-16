package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.workstation.gui.viewer3d.OcclusiveViewer;
import org.janelia.it.workstation.gui.viewer3d.OpenGLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This one can read from a preset mouse position.
 *
 * @author fosterl
 */
public class PixelReadActor implements GLActor {
    private Logger logger = LoggerFactory.getLogger(PixelReadActor.class);
    private BoundingBox3d boundingBox;
    private OcclusiveViewer viewer;
    private int x = -1;
    private int y = -1;
    
    private float[] rgb = null;
    private PixelListener listener;
    
    public PixelReadActor( OcclusiveViewer viewer ) {
        this.viewer = viewer;
    }
    
    public void setSampleCoords( int x, int y ) {
        this.x = x;
        this.y = viewer.getHeight() - y;
    }
    
    public float[] getRGB() {
        return rgb;
    }
    
    public void setPixelListener(PixelListener listener) {
        this.listener = listener;
    }

    /**
     * This 'display' method never displays anything; it simply samples what
     * has been displayed by the other actors.
     * @param glDrawable 
     */
    @Override
    public void display(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();
        if (x != -1) {
            
            gl.glReadBuffer(GL2.GL_FRONT); // Later: use GL_BACK
            OpenGLUtils.reportError(gl, "Read-Buffer setup");
            // 4 floats: R,G,B,A
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(Float.SIZE / Byte.SIZE * 4);
            byteBuffer.order(ByteOrder.nativeOrder());
            FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
            floatBuffer.rewind();
            gl.glReadPixels( x, y, 1, 1, GL2.GL_RGBA, GL2.GL_FLOAT, floatBuffer );
            logger.info("Reading pixels at {},{}.", x, y);
            OpenGLUtils.reportError(gl, "Read-Pixels");
            rgb = new float[3];
            rgb[0] = floatBuffer.get();
            rgb[1] = floatBuffer.get();
            rgb[2] = floatBuffer.get();
            
            if (listener != null) {
                listener.setPixel(rgb);
            }

            // Cleanup prior to next display.
            x = -1;
            y = -1;
        }
    }

    @Override
    public BoundingBox3d getBoundingBox3d() {
        return boundingBox;
    }
    
    public void setBoundingBox3d(BoundingBox3d box) {
        this.boundingBox = box;
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
        // Nothing to do.
    }

    @Override
    public void dispose(GLAutoDrawable glDrawable) {
    }
    
    /**
     * Implement this and set it, to be informed of the captured pixel value.
     */
    public static interface PixelListener {
        void setPixel( float[] pixel );
    }
}
