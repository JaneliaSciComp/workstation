package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BaseGLViewer;
import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

/**
 * Intended replacement class for SliceViewer,
 * generalized for X,Y,Z orthogonal views
 * @author brunsc
 *
 */
public class OrthogonalViewer // extends BaseGLViewer
extends GLJPanel
{
	private Camera3d camera;
	private Viewport viewport;
	private VolumeImage3d volume;
	private CoordinateAxis viewAxis;
	private SliceRenderer renderer = new SliceRenderer();
	
	public OrthogonalViewer(CoordinateAxis axis) {
		init(axis);
	}
	
	public OrthogonalViewer(CoordinateAxis axis, 
			GLCapabilities capabilities,
			GLCapabilitiesChooser chooser,
			GLContext sharedContext) 
	{
		super(capabilities, chooser, sharedContext);
		init(axis);
	}
	
	private void init(CoordinateAxis axis) {
		this.viewAxis = axis;
		addGLEventListener(renderer);
		renderer.setBackgroundColor(Color.pink); // TODO set to black		
	}

	public void setCamera(ObservableCamera3d camera) {
		this.camera = camera;
	}
	
	public void setVolumeImage3d(VolumeImage3d volume) {
		this.volume = volume;
	}
}
