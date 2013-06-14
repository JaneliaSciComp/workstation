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
import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation;
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
    // Viewer orientation relative to canonical orientation.
    // Canonical orientation is x-right, y-down, z-away
    Rotation viewerInGround = new Rotation();

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
		if (axis == CoordinateAxis.Z)
		    viewerInGround = new Rotation(); // identity rotation, canonical orientation
		else if (axis == CoordinateAxis.X) // y-down, z-left, x-away
		    viewerInGround.setFromCanonicalRotationAboutPrincipalAxis(
		            1, CoordinateAxis.Y);
		else // Y-away, x-right, z-up
		    viewerInGround.setFromCanonicalRotationAboutPrincipalAxis(
		            3, CoordinateAxis.X);
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
