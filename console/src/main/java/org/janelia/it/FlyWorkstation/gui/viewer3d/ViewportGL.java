package org.janelia.it.FlyWorkstation.gui.viewer3d;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;

// ViewportGL combines:
//  1) size of OpenGL viewport
//  2) OpenGL viewport response to size changes
//     a) viewport size
public class ViewportGL 
implements Viewport
{
    private int width; // in pixels
    private int height; // in pixels
    private int originX = 0;
	private int originY = 0;


    public int getOriginX() {
		return originX;
	}

	public void setOriginX(int originX) {
		this.originX = originX;
	}

	public int getOriginY() {
		return originY;
	}

	public void setOriginY(int originY) {
		this.originY = originY;
	}

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
		gl.glViewport(originX, originY, width, height);
    }

	public void setHeight(int height) {
		this.height = height;
	}

	public void setWidth(int width) {
		this.width = width;
	}
}
