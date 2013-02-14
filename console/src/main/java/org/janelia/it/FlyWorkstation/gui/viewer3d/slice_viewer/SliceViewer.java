package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;

import org.janelia.it.FlyWorkstation.gui.viewer3d.BaseGLViewer;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;

// Viewer widget for viewing 2D quadtree tiles from pyramid data structure
public class SliceViewer 
extends BaseGLViewer
implements MouseModalWidget
{
	private static final long serialVersionUID = 1L;
	protected MouseMode mouseMode = new PanMode();
	protected WheelMode wheelMode = new ZoomMode();
	protected ObservableCamera3d camera;
	protected SliceRenderer renderer = new SliceRenderer();
	protected RubberBand rubberBand = new RubberBand();

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
		wheelMode.setComponent(this);
		// pink color for testing only
		this.renderer.setBackgroundColor(new Color(0.5f, 0.5f, 0.5f, 0.0f));
        setPreferredSize( new Dimension( 600, 600 ) );
        rubberBand.changed.connect(repaintSlot);
        setToolTipText("Double click to center on point.");
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

	@Override
	public Point2D getPixelOffsetFromCenter(Point2D point) {
		double dx = point.getX() - getWidth() / 2.0;
		double dy = point.getY() - getHeight() / 2.0;
		return new Point2D.Double(dx, dy);
	}

	public QtSlot<Object> getRepaintSlot() {
		return repaintSlot;
	}

	public RubberBand getRubberBand() {
		return rubberBand;
	}

	@Override
	public Dimension getViewportSize() {
		return getSize();
	}

	public WheelMode getWheelMode() {
		return wheelMode;
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		if (event.getClickCount() == 1) {
			mouseMode.mouseClicked(event);
		}
		else if (event.getClickCount() == 2) {
			Point2D p = getPixelOffsetFromCenter(event.getPoint());
			camera.incrementFocusPixels(p.getX(), p.getY(), 0.0);
		}
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
	public void mouseWheelMoved(MouseWheelEvent event) {
		this.wheelMode.mouseWheelMoved(event);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		rubberBand.paint(g2);
	}
	
	public void setMouseMode(MouseMode mouseMode) {
		this.mouseMode = mouseMode;
		this.mouseMode.setComponent(this);
		this.mouseMode.setCamera(camera);
	}

	public void setCamera(ObservableCamera3d camera) {
		if (camera == null)
			return;
		if (camera == this.camera)
			return;
		this.camera = camera;
		// Update image whenever camera changes
		camera.getViewChangedSignal().connect(repaintSlot);
		renderer.setCamera(camera);
		mouseMode.setCamera(camera);
		wheelMode.setCamera(camera);
	}
	
	public void setWheelMode(WheelMode wheelMode) {
		this.wheelMode = wheelMode;
		this.wheelMode.setComponent(this);
		this.wheelMode.setCamera(camera);
	}
}
