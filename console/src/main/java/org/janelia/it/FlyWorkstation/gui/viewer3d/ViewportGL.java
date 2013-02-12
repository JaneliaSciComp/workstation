package org.janelia.it.FlyWorkstation.gui.viewer3d;

import javax.media.opengl.GL2;

// ViewportGL combines:
//  1) size of OpenGL viewport
//  2) OpenGL viewport response to size changes
//     a) viewport size
public class ViewportGL {
    private int width; // in pixels
    private int height; // in pixels

    public int getHeight() {
		return height;
	}

    public int getWidth() {
		return width;
	}

	public void reshape(GL2 gl, int width, int height) {
		if ( (width == getWidth()) && (height == getHeight()) )
			return; // no change
		setWidth(width);
		setHeight(height);
		gl.glViewport(0, 0, width, height);
    }

	public void setHeight(int height) {
		this.height = height;
	}

	public void setWidth(int width) {
		this.width = width;
	}
}
