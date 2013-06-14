package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BaseGLViewer;
import org.janelia.it.FlyWorkstation.gui.viewer3d.BoundingBox3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.BasicObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.GLActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.BasicMouseMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.MouseMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.MouseMode.Mode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.PanMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.TraceMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.WheelMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ZScanMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ZoomMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ZoomScrollModeAction;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.Skeleton;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.SkeletonActor;

import javax.media.opengl.GLProfile;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Viewer widget for viewing 2D quadtree tiles from pyramid data structure
public class SliceViewer 
extends BaseGLViewer
implements MouseModalWidget, VolumeViewer
{
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(SliceViewer.class);
	static public GLProfile glProfile;
	static {
		glProfile = BaseGLViewer.profile;
	}
	
	protected MouseMode mouseMode;
	protected MouseMode.Mode mouseModeId;
	
	protected WheelMode wheelMode;
	protected WheelMode.Mode wheelModeId;
	protected ObservableCamera3d camera;
	protected SliceRenderer renderer = new SliceRenderer();
	protected Viewport viewport = renderer.getViewport();
	protected RubberBand rubberBand = new RubberBand();
	protected SkeletonActor skeletonActor = new SkeletonActor();
	
	// TODO - use a factory to choose the particular volumeimage
	// protected PracticeBlueVolume volume0 = new PracticeBlueVolume();
	// protected Simple2dImageVolume volume0 = new Simple2dImageVolume(
	// 		"/Users/brunsc/svn/jacs/console/src/main/java/images/kittens.jpg");	
	protected TileServer tileServer = new TileServer(new SharedVolumeImage());
	protected VolumeImage3d volumeImage = tileServer;
	protected SliceActor volumeActor = new SliceActor(tileServer);
	private ImageColorModel imageColorModel;
	private BasicMouseMode pointComputer = new BasicMouseMode();
	
	// Popup menu
	MenuItemGenerator systemMenuItemGenerator;
	MenuItemGenerator modeMenuItemGenerator;
	
	public Signal1<URL> getFileLoadedSignal() {
		return fileLoadedSignal;
	}

	protected Signal1<URL> fileLoadedSignal = new Signal1<URL>();
	public Signal1<String> statusMessageChanged = new Signal1<String>();
	
	protected Slot1<URL> loadUrlSlot = new Slot1<URL>() {
		@Override
		public void execute(URL url) {
			// log.info("loadUrlSlot");
			if (! loadURL(url)) {
				JOptionPane.showMessageDialog(SliceViewer.this, 
						"Error loading volume from\n  "+url
						+"\nIs the share mounted?");
			}
		}
	};
	
	protected Slot repaintSlot = new Slot() {
		@Override
		public void execute() {
			// System.out.println("repaint slot");
			repaint();
		}
	};
	
	public Slot1<MouseMode.Mode> setMouseModeSlot =
	    new Slot1<MouseMode.Mode>() {
            @Override
            public void execute(MouseMode.Mode modeId) {
                SliceViewer.this.setMouseMode(modeId);
            }
	};
    public Slot1<WheelMode.Mode> setWheelModeSlot =
        new Slot1<WheelMode.Mode>() {
            @Override
            public void execute(WheelMode.Mode modeId) {
                SliceViewer.this.setWheelMode(modeId);
            }
    };	
	public SliceViewer() {
	    setMouseMode(MouseMode.Mode.PAN);
	    setWheelMode(WheelMode.Mode.SCAN);
		addGLEventListener(renderer);
		setCamera(new BasicObservableCamera3d());
		tileServer.setViewport(viewport);
		// gray background for testing
		// this.renderer.setBackgroundColor(new Color(0.5f, 0.5f, 0.5f, 0.0f));
		// renderer.setBackgroundColor(Color.white);
		// black background for production
		renderer.setBackgroundColor(Color.black);
        setPreferredSize( new Dimension( 600, 600 ) );
        rubberBand.changed.connect(repaintSlot);
        // setToolTipText("Double click to center on a point.");
        setImageColorModel(new ImageColorModel(volumeImage));
        renderer.addActor(volumeActor);
        tileServer.getViewTextureChangedSignal().connect(getRepaintSlot());
        imageColorModel.getColorModelChangedSignal().connect(getRepaintSlot());
        // Initialize pointComputer for interconverting pixelXY <=> sceneXYZ
		pointComputer.setCamera(getCamera());
		pointComputer.setComponent(this);
		//
        renderer.addActor(skeletonActor);
        skeletonActor.skeletonActorChangedSignal.connect(repaintSlot);
        skeletonActor.setViewport(viewport);
		//
        // PopupMenu
        addMouseListener(new MouseHandler() {
            @Override
            protected void popupTriggered(MouseEvent e) {
                // System.out.println("popup");
                if (e.isConsumed()) 
                    return;
                JPopupMenu popupMenu = new JPopupMenu();
                // Mode specific menu items first
                List<JMenuItem> modeItems = modeMenuItemGenerator.getMenus(e);
                List<JMenuItem> systemMenuItems = systemMenuItemGenerator.getMenus(e);
                if ((modeItems.size() == 0) && (systemMenuItems.size() == 0))
                    return;
                if ((modeItems != null) && (modeItems.size() > 0)) {
                    for (JMenuItem item : modeItems) {
                        if (item == null)
                            popupMenu.addSeparator();
                        else
                            popupMenu.add(item);
                    }
                    if (systemMenuItems.size() > 0)
                        popupMenu.addSeparator();
                }
                // Generic menu items last
                for (JMenuItem item : systemMenuItems) {
                    if (item == null)
                        popupMenu.addSeparator();
                    else
                        popupMenu.add(item);
                }
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
                e.consume();
            }
        });
        //
        resetView();
	}

	public void autoContrastNow() {
		ImageBrightnessStats bs = tileServer.getCurrentBrightnessStats();
		if (bs == null)
			return;
		// Remember which channel has the extreme values
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		int minChan = 0;
		int maxChan = 0;
		// Set one channel at a time
		for (int c = 0; c < bs.size(); ++c) {
			if (c >= imageColorModel.getChannelCount())
				break;
			ChannelColorModel chanModel = imageColorModel.getChannel(c);
			ChannelBrightnessStats chanStats = bs.get(c);
			// int cMin = chanStats.getMin();
			int cMin = chanStats.estimateQuantile(0.05);
			int cMax = chanStats.estimateQuantile(1.0);
			chanModel.setBlackLevel(cMin);
			chanModel.setWhiteLevel(cMax);
			if (chanStats.getMax() >= max) {
				max = cMax;
				maxChan = c;
			}
			if (cMin <= min) {
				min = cMin;
				minChan = c;
			}
		}
		// In case black and/or white levels are locked,
		// re-specify the most extreme values
		if (bs.size() > 1) {
			imageColorModel.getChannel(maxChan).setWhiteLevel(max);
			imageColorModel.getChannel(minChan).setBlackLevel(min);
			log.info("max = "+max+"; min = "+min);
		}
	}

	public Slot1<URL> getLoadUrlSlot() {
		return loadUrlSlot;
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

	public ImageColorModel getImageColorModel() {
		return imageColorModel;
	}

	@Override
	public Point2D getPixelOffsetFromCenter(Point2D point) {
		double dx = point.getX() - getWidth() / 2.0;
		double dy = point.getY() - getHeight() / 2.0;
		return new Point2D.Double(dx, dy);
	}

	public Slot getRepaintSlot() {
		return repaintSlot;
	}

	public RubberBand getRubberBand() {
		return rubberBand;
	}

	@Override
	public Viewport getViewport() {
		return viewport;
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
		requestFocusInWindow();
	}
	
	@Override
	public void mouseDragged(MouseEvent event) {
		mouseMode.mouseDragged(event);
	}

	@Override
	public void mouseMoved(MouseEvent event) {
		mouseMode.mouseMoved(event);
		Vec3 xyz = pointComputer.worldFromPixel(event.getPoint());
		DecimalFormat fmt = new DecimalFormat("0.0");
		String msg = "["
				+ fmt.format(xyz.getX())
				+ ", " + fmt.format(xyz.getY())
				+ ", " + fmt.format(xyz.getZ())
				+ "] \u00B5m"; // micrometers. Maybe I should use pixels (also?)?
		statusMessageChanged.emit(msg);
		// System.out.println(xyz);
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
	
    @Override
    public void setMouseMode(MouseMode.Mode modeId) {
        if (modeId == this.mouseModeId)
            return; // no change
        this.mouseModeId = modeId;
        if (modeId == MouseMode.Mode.PAN) {
            this.mouseMode = new PanMode();
        }
        else if (modeId == MouseMode.Mode.TRACE) {
            TraceMode traceMode = new TraceMode(getSkeleton());
            traceMode.setViewport(getViewport());
            traceMode.setActor(skeletonActor);
            this.mouseMode = traceMode;
        }
        else if (modeId == MouseMode.Mode.ZOOM) {
            this.mouseMode = new ZoomMode();
        }
        else {
            log.error("Unknown mouse mode");
            return;
        }
        this.mouseMode.setCamera(camera);
        this.mouseMode.setComponent(this);
        this.setToolTipText(mouseMode.getToolTipText());        
        this.modeMenuItemGenerator = mouseMode.getMenuItemGenerator();
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
		tileServer.setCamera(camera);
		pointComputer.setCamera(camera);
		skeletonActor.setCamera(camera);
	}
	
	public void setWheelMode(WheelMode.Mode wheelModeId) {
	    if (this.wheelModeId == wheelModeId)
	        return;
	    this.wheelModeId = wheelModeId;
	    if (wheelModeId == WheelMode.Mode.ZOOM) {
	        this.wheelMode = new ZoomMode();
	    }
	    else if (wheelModeId == WheelMode.Mode.SCAN) {
	        this.wheelMode = new ZScanMode(this);
	    }
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

	public TileServer getTileServer() {
		return tileServer;
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
	public double getXResolution() {
		return volumeImage.getXResolution();
	}

	@Override
	public double getYResolution() {
		return volumeImage.getYResolution();
	}

	@Override
	public double getZResolution() {
		return volumeImage.getZResolution();
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
		return volumeImage.getNumberOfChannels();
	}

	@Override
	public int getMaximumIntensity() {
		return volumeImage.getMaximumIntensity();
	}

	@Override
	public double getMaxZoom()
	{
		// 300 screen pixels per image pixel
		// Yes, I mean min, because high resolution is a small distance value
		double maxRes = Math.min(getXResolution(), getYResolution());
		return 300.0 / maxRes;
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
	public Signal1<Double> getZoomChangedSignal() {
		return camera.getZoomChangedSignal();
	}

	@Override
	public boolean loadURL(URL url) {
		boolean result = volumeImage.loadURL(url);
		if (result) {
			getImageColorModel().reset(volumeImage);
			resetView();
			// log.info("emitting file loaded signal");
			getFileLoadedSignal().emit(url);
		}
		return result;
	}
	
	public void setImageColorModel(ImageColorModel imageColorModel) {
		this.imageColorModel = imageColorModel;
		volumeActor.setImageColorModel(imageColorModel);
	}
	
	public Skeleton getSkeleton() {
		return skeletonActor.getSkeleton();
	}
	
	public void setSkeleton(Skeleton skeleton) {
		skeletonActor.setSkeleton(skeleton);
	}

	public SkeletonActor getSkeletonActor() {
		return skeletonActor;
	}

	public void setSkeletonActor(SkeletonActor skeletonActor) {
		this.skeletonActor = skeletonActor;
		skeletonActor.setViewport(viewport);
	}

	public void setSystemMenuItemGenerator(MenuItemGenerator systemMenuItemGenerator) {
        this.systemMenuItemGenerator = systemMenuItemGenerator;
    }

    @Override
	public JComponent getComponent() {
		return this;
	}

	@Override
	public int getDepth() {
		return viewport.getDepth();
	}

	@Override
	public double getResolution(int ix) {
		if (ix == 0) return getXResolution();
		else if (ix == 1) return getYResolution();
		else return getZResolution();
	}
}
