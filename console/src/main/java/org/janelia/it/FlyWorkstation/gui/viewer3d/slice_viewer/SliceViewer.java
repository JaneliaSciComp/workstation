package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BaseGLViewer;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;

// Viewer widget for viewing 2D quadtree tiles from pyramid data structure
public class SliceViewer 
extends BaseGLViewer
{
	private static final long serialVersionUID = 1L;
	protected MouseMode panMode = new PanMode();
	protected MouseMode mouseMode = panMode;
	protected ObservableCamera3d camera;
	protected SliceRenderer renderer = new SliceRenderer();

	protected QtSlot<Object> repaintSlot = new QtSlot<Object>(this) {
		@Override
		public void execute(Object arg) {
			// System.out.println("repaint slot");
			((SliceViewer)receiver).repaint();
		}
	};

	public SliceViewer() {
		addGLEventListener(renderer);
		setCamera(new BasicObservableCamera3d());
		mouseMode.setComponent(this);
		// pink color for testing only
		this.renderer.setBackgroundColor(new Color(0.5f, 0.5f, 0.5f, 0.0f));
        setPreferredSize( new Dimension( 600, 600 ) );
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public ObservableCamera3d getCamera() {
		return camera;
	}

	public MouseMode getMouseMode() {
		return mouseMode;
	}

	public QtSlot<Object> getRepaintSlot() {
		return repaintSlot;
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		mouseMode.mouseClicked(event);
	}
	
	@Override
	public void mouseDragged(MouseEvent event) {
		mouseMode.mouseDragged(event);
	}

	@Override
	public void mouseMoved(MouseEvent event) {
		mouseMode.mouseMoved(event);
	}

	@Override
	public void mousePressed(MouseEvent event) {
		super.mousePressed(event); // Activate context menu
		mouseMode.mousePressed(event);
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		super.mouseReleased(event); // Activate context menu
		mouseMode.mouseReleased(event);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		// TODO Auto-generated method stub
	}

	public void setMouseMode(MouseMode mouseMode) {
		this.mouseMode = mouseMode;
	}

	public void setCamera(ObservableCamera3d camera) {
		if (camera == null)
			return;
		if (camera == this.camera)
			return;
		this.camera = camera;
		// Update image whenever camera changes
		getCamera().getViewChangedSignal().connect(repaintSlot);
		renderer.setCamera(camera);
		mouseMode.setCamera(camera);
	}
	
	public void setPanMode() {
		System.out.println("Pan mode set");
		setMouseMode(panMode);
	}
}
