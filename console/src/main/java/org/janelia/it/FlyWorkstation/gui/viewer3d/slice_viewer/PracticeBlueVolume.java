package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.net.URL;

import javax.media.opengl.GL2;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

// Blue square in the X/Y plane
public class PracticeBlueVolume 
implements VolumeImage3d, GLActor
{
	private GLActor actor = new PracticeBlueTileActor();
	
	@Override
	public BoundingBox3d getBoundingBox3d() {
		return actor.getBoundingBox3d();
	}

	@Override
	public double getXResolution() {
		return 0.10;
	}

	@Override
	public double getYResolution() {
		return 0.10;
	}

	@Override
	public double getZResolution() {
		return 0.10;
	}

	@Override
	public void display(GL2 gl) {
		actor.display(gl);
	}

	@Override
	public void init(GL2 gl) {
		actor.init(gl);
	}

	@Override
	public void dispose(GL2 gl) {
		actor.dispose(gl);
	}

	@Override
	public int getNumberOfChannels() {
		return 3;
	}

	@Override
	public int getMaximumIntensity() {
		return 255;
	}

	@Override
	public boolean loadURL(URL url) {
		// The blue square you want is already "loaded"
		return false;
	}
}
