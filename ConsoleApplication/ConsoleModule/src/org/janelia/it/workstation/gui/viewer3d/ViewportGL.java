package org.janelia.it.workstation.gui.viewer3d;

import javax.media.opengl.GL2;

import org.janelia.it.workstation.gui.viewer3d.interfaces.Viewport;

// ViewportGL combines:
//  1) size of OpenGL viewport
//  2) OpenGL viewport response to size changes
//     a) viewport size
public class ViewportGL 
implements Viewport
{
    private int width = 1000; // in pixels
    private int height = 600; // in pixels
	// this value is chosen so at high zoom, we have room to show
    //  annotations ver greater depth than at low zoom (was
    //  originally 80)
    private int depth = 300; // in pixels
    private int originX = 0;
	private int originY = 0;


    public int getOriginX() {
		return originX;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public int getDepth() {
		return depth;
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
