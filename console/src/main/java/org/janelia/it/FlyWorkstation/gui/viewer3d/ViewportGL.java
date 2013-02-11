package org.janelia.it.FlyWorkstation.gui.viewer3d;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;

import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicCamera3d;

// ViewportGL combines:
//  1) camera information
//  2) size of OpenGL viewport
//  3) OpenGL viewport response to size changes
//     a) viewport size
//     b) camera projection? TODO
public class ViewportGL {
    private int width; // in pixels
    private int height; // in pixels
    private BasicCamera3d camera;
    
	public BasicCamera3d getCamera() {
		return camera;
	}

	public int getHeight() {
		return height;
	}

    public int getWidth() {
		return width;
	}

	public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) {
        final GL2 gl = gLDrawable.getGL().getGL2();
		// Assuming OpenGL viewport occupies entire GLEventListener widget...
        // (won't be so for side-by-side stereo, for example)
		gl.glViewport(0, 0, width, height);
    }

	public void setCamera(BasicCamera3d camera) {
		this.camera = camera;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setWidth(int width) {
		this.width = width;
	}
}
