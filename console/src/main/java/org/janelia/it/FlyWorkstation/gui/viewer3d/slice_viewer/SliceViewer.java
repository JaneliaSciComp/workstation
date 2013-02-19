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
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;

// Viewer widget for viewing 2D quadtree tiles from pyramid data structure
public class SliceViewer 
extends BaseGLViewer
implements MouseModalWidget, VolumeViewer
{
	private static final long serialVersionUID = 1L;
	protected MouseMode mouseMode = new PanMode();
	protected WheelMode wheelMode = new ZoomMode();
	protected ObservableCamera3d camera;
	protected SliceRenderer renderer = new SliceRenderer();
	protected Viewport viewport = renderer.getViewport();
	protected RubberBand rubberBand = new RubberBand();
	protected PracticeBlueVolume volume = new PracticeBlueVolume();
	
	protected QtSignal1<Double> zoomChanged = new QtSignal1<Double>();

	protected QtSlot repaintSlot = new QtSlot(this) {
		@Override
		public void execute() {
			// System.out.println("repaint slot");
			((SliceViewer)receiver).repaint();
		}
	};

	public SliceViewer() {
		addGLEventListener(renderer);
		setCamera(new BasicObservableCamera3d());
		camera.getZoomChanged().connect(zoomChanged); // Forward signal
		mouseMode.setComponent(this);
		wheelMode.setComponent(this);
		// pink color for testing only
		this.renderer.setBackgroundColor(new Color(0.5f, 0.5f, 0.5f, 0.0f));
        setPreferredSize( new Dimension( 600, 600 ) );
        rubberBand.changed.connect(repaintSlot);
        setToolTipText("Double click to center on a point.");
        renderer.addActor(volume);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public BoundingBox3d getBoundingBox3d() 
	{
		BoundingBox3d bb = new BoundingBox3d();
		for (GLActor a : renderer.getActors()) {
			bb.include(a.getBoundingBox3d());
		}
		return bb;
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

	public QtSlot getRepaintSlot() {
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
	
	public boolean resetFocus() {
		return resetFocus(getBoundingBox3d());
	}
	
	public boolean resetFocus(BoundingBox3d box) {
		if (box.isEmpty())
			return false;
		return camera.setFocus(box.getCenter());
	}
	
	public boolean resetView() {
		// Compute the smallest bounding box that holds all actors
		BoundingBox3d bb = getBoundingBox3d();
		if (! resetFocus(bb))
			return false;
		if (! resetZoom(bb))
			return false;
		return true;
	}
	
	public boolean resetZoom() 
	{
		return resetZoom(getBoundingBox3d());
	}

	public boolean resetZoom(BoundingBox3d box) 
	{
		if (box.isEmpty())
			return false;
		// Need to fit entire bounding box
		double sx = box.getWidth();
		double sy = box.getHeight();
		// ... plus offset from center
		Vec3 bc = box.getCenter();
		Camera3d camera = getCamera();		
		Vec3 focus = camera.getFocus();
		sx += 2.0 * Math.abs(bc.getX() - focus.getX());
		sy += 2.0 * Math.abs(bc.getY() - focus.getY());
		// Use minimum of X and Y zoom
		double zx = getWidth() / sx;
		double zy = getHeight() / sy;
		double zoom = Math.min(zx, zy);
		if (! camera.setPixelsPerSceneUnit(zoom))
			return false;
		return true;
	}
	
	public void setMouseMode(MouseMode mouseMode) {
		if (mouseMode == this.mouseMode)
			return;
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

	@Override
	public Vec3 getFocus() {
		return camera.getFocus();
	}

	@Override
	public double getPixelsPerSceneUnit() {
		return camera.getPixelsPerSceneUnit();
	}

	@Override
	public Rotation getRotation() {
		return camera.getRotation();
	}

	@Override
	public boolean incrementFocusPixels(double dx, double dy, double dz) {
		return camera.incrementFocusPixels(dx, dy, dz);
	}

	@Override
	public boolean incrementFocusPixels(Vec3 offset) {
		return camera.incrementFocusPixels(offset);
	}

	@Override
	public boolean incrementZoom(double zoomRatio) {
		return camera.incrementZoom(zoomRatio);
	}

	@Override
	public boolean resetRotation() {
		return camera.resetRotation();
	}

	@Override
	public boolean setFocus(double x, double y, double z) {
		return camera.setFocus(x, y, z);
	}

	@Override
	public boolean setFocus(Vec3 focus) {
		return camera.setFocus(focus);
	}

	@Override
	public boolean setRotation(Rotation r) {
		return camera.setRotation(r);
	}

	@Override
	public boolean setPixelsPerSceneUnit(double pixelsPerSceneUnit) {
		return camera.setPixelsPerSceneUnit(pixelsPerSceneUnit);
	}

	@Override
	public double getMaxResolution() {
		return volume.getMaxResolution();
	}

	@Override
	public int getOriginX() {
		return viewport.getOriginX();
	}

	@Override
	public int getOriginY() {
		return viewport.getOriginY();
	}

	@Override
	public int getNumberOfChannels() {
		return volume.getNumberOfChannels();
	}

	@Override
	public int getMaximumIntensity() {
		return volume.getMaximumIntensity();
	}

	@Override
	public double getMaxZoom() 
	{
		// 300 screen pixels per image pixel
		return 300.0 / getMaxResolution();
	}

	@Override
	public double getMinZoom() {
		// Fit two of the whole volume on the screen
		BoundingBox3d box = getBoundingBox3d();
		double minZoomX = 0.5 * viewport.getWidth() / box.getWidth();
		double minZoomY = 0.5 * viewport.getHeight() / box.getHeight();
		return Math.min(minZoomX, minZoomY);
	}

	@Override
	public QtSignal1<Double> getZoomChanged() {
		return zoomChanged;
	}
}
