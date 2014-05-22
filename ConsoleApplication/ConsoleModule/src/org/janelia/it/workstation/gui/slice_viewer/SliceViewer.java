package org.janelia.it.workstation.gui.slice_viewer;

import org.janelia.it.workstation.geom.CoordinateAxis;
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.gui.slice_viewer.action.MouseMode;
import org.janelia.it.workstation.gui.slice_viewer.action.TraceMode;
import org.janelia.it.workstation.gui.slice_viewer.action.WheelMode;
import org.janelia.it.workstation.gui.slice_viewer.action.ZoomMode;
import org.janelia.it.workstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.workstation.signal.Signal1;
import org.janelia.it.workstation.signal.Slot1;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Viewer widget for viewing 2D quadtree tiles from pyramid data structure
public class SliceViewer 
extends GLJPanel
implements MouseModalWidget, TileConsumer
{
	private static final Logger log = LoggerFactory.getLogger(SliceViewer.class);

    private boolean optInStatic3d = false;
    private URL dataUrl;

	protected MouseMode mouseMode;
	protected MouseMode.Mode mouseModeId;
	
	protected WheelMode wheelMode;
	protected WheelMode.Mode wheelModeId;
	protected org.janelia.it.workstation.gui.camera.ObservableCamera3d camera;
	protected SliceRenderer renderer = new SliceRenderer();
	protected Viewport viewport = renderer.getViewport();
	protected org.janelia.it.workstation.gui.slice_viewer.RubberBand rubberBand = new org.janelia.it.workstation.gui.slice_viewer.RubberBand();
	protected org.janelia.it.workstation.gui.slice_viewer.skeleton.SkeletonActor skeletonActor = new org.janelia.it.workstation.gui.slice_viewer.skeleton.SkeletonActor();
	

	SharedVolumeImage sharedVolumeImage = new SharedVolumeImage();
	protected org.janelia.it.workstation.gui.slice_viewer.TileServer tileServer = new org.janelia.it.workstation.gui.slice_viewer.TileServer(sharedVolumeImage);
	protected org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d volumeImage = sharedVolumeImage;
	protected SliceActor sliceActor;
	private ImageColorModel imageColorModel;
	private org.janelia.it.workstation.gui.slice_viewer.action.BasicMouseMode pointComputer = new org.janelia.it.workstation.gui.slice_viewer.action.BasicMouseMode();
	
	// Popup menu
	MenuItemGenerator systemMenuItemGenerator;
	MenuItemGenerator modeMenuItemGenerator;
	
	public Signal1<String> statusMessageChanged = new Signal1<String>();
	
	protected org.janelia.it.workstation.signal.Slot repaintSlot = new org.janelia.it.workstation.signal.Slot() {
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
    
	public SliceViewer(GLCapabilities capabilities,
			GLCapabilitiesChooser chooser,
			GLContext sharedContext,
			org.janelia.it.workstation.gui.camera.ObservableCamera3d camera)
	{
		super(capabilities, chooser, sharedContext);
		init(camera);
	}
	
	private void init(org.janelia.it.workstation.gui.camera.ObservableCamera3d camera) {
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);
	    setMouseMode(MouseMode.Mode.PAN);
	    setWheelMode(WheelMode.Mode.SCAN);
		addGLEventListener(renderer);
		setCamera(camera);
		//
		ViewTileManager viewTileManager = new ViewTileManager(this);
		viewTileManager.setVolumeImage(tileServer.getSharedVolumeImage());
		viewTileManager.setTextureCache(tileServer.getTextureCache());
		tileServer.addViewTileManager(viewTileManager);
		sliceActor = new SliceActor(viewTileManager);
		//
		// gray background for testing
		// this.renderer.setBackgroundColor(new Color(0.5f, 0.5f, 0.5f, 0.0f));
		// renderer.setBackgroundColor(Color.white);
		// black background for production
		renderer.setBackgroundColor(Color.black);
        setPreferredSize( new Dimension( 600, 600 ) );
        rubberBand.changed.connect(repaintSlot);
        // setToolTipText("Double click to center on a point.");
        renderer.addActor(sliceActor);
        // renderer.addActor(new TileOutlineActor(viewTileManager));
        // Initialize pointComputer for interconverting pixelXY <=> sceneXYZ
		pointComputer.setCamera(getCamera());
		pointComputer.setWidget(this, false);
		//
        renderer.addActor(skeletonActor);
        skeletonActor.skeletonActorChangedSignal.connect(repaintSlot);
        skeletonActor.setZThicknessInPixels(viewport.getDepth());
		//
        // PopupMenu
        addMouseListener(new org.janelia.it.workstation.gui.util.MouseHandler() {
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
			// log.info("max = "+max+"; min = "+min);
		}
	}

	public org.janelia.it.workstation.gui.viewer3d.BoundingBox3d getBoundingBox3d()
	{
		org.janelia.it.workstation.gui.viewer3d.BoundingBox3d bb = new org.janelia.it.workstation.gui.viewer3d.BoundingBox3d();
		for (org.janelia.it.workstation.gui.opengl.GLActor a : renderer.getActors()) {
			bb.include(a.getBoundingBox3d());
		}
		return bb;
	}

	public org.janelia.it.workstation.gui.camera.ObservableCamera3d getCamera() {
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

	public org.janelia.it.workstation.signal.Slot getRepaintSlot() {
		return repaintSlot;
	}

	public org.janelia.it.workstation.gui.slice_viewer.RubberBand getRubberBand() {
		return rubberBand;
	}

	@Override
	public Viewport getViewport() {
		return viewport;
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
		org.janelia.it.workstation.geom.Vec3 xyz = pointComputer.worldFromPixel(event.getPoint());
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
        System.out.println("slice viewer: mouse pressed");
		mouseMode.mousePressed(event);
	}

	@Override
	public void mouseReleased(MouseEvent event) {
        System.out.println("slice viewer: mouse released");
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
	
    @Override
    public void setMouseMode(MouseMode.Mode modeId) {
        if (modeId == this.mouseModeId)
            return; // no change
        this.mouseModeId = modeId;
        if (modeId == MouseMode.Mode.PAN) {
            this.mouseMode = new org.janelia.it.workstation.gui.slice_viewer.action.PanMode();
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
        this.mouseMode.setWidget(this, true);
        this.setToolTipText(mouseMode.getToolTipText());        
        this.modeMenuItemGenerator = mouseMode.getMenuItemGenerator();
    }
    
	public void setCamera(org.janelia.it.workstation.gui.camera.ObservableCamera3d camera) {
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
	        this.wheelMode = new org.janelia.it.workstation.gui.slice_viewer.action.ZScanMode(volumeImage);
	    }
		this.wheelMode.setWidget(this, false);
		this.wheelMode.setCamera(camera);
	}

	public org.janelia.it.workstation.gui.slice_viewer.TileServer getTileServer() {
		return tileServer;
	}

	public void setImageColorModel(ImageColorModel imageColorModel) {
		if (this.imageColorModel == imageColorModel)
			return;
		this.imageColorModel = imageColorModel;
        imageColorModel.getColorModelChangedSignal().connect(getRepaintSlot());
		sliceActor.setImageColorModel(imageColorModel);
	}
	
	public org.janelia.it.workstation.gui.slice_viewer.skeleton.Skeleton getSkeleton() {
		return skeletonActor.getSkeleton();
	}
	
	public void setSkeleton(org.janelia.it.workstation.gui.slice_viewer.skeleton.Skeleton skeleton) {
		skeletonActor.setSkeleton(skeleton);
	}

	public org.janelia.it.workstation.gui.slice_viewer.skeleton.SkeletonActor getSkeletonActor() {
		return skeletonActor;
	}

	public void setSkeletonActor(org.janelia.it.workstation.gui.slice_viewer.skeleton.SkeletonActor skeletonActor) {
		this.skeletonActor = skeletonActor;
		skeletonActor.setZThicknessInPixels(viewport.getDepth());
	}

	public void setSystemMenuItemGenerator(MenuItemGenerator systemMenuItemGenerator) {
        this.systemMenuItemGenerator = systemMenuItemGenerator;
    }

    @Override
	public JComponent getComponent() {
		return this;
	}

	@Override
	public MouseMode getMouseMode() {
		return mouseMode;
	}

	@Override
	public CoordinateAxis getSliceAxis() {
		return CoordinateAxis.Z;
	}

	@Override
	public Rotation3d getViewerInGround() {
		return new Rotation3d(); // identity matrix
	}

	@Override
	public void mouseEntered(MouseEvent event) {
		mouseMode.mouseEntered(event);
	}

	@Override
	public void mouseExited(MouseEvent event) {
		mouseMode.mouseExited(event);
	}

	// KeyListener interface
	@Override
	public void keyTyped(KeyEvent event) {
		mouseMode.keyTyped(event);
	}

	@Override
	public void keyPressed(KeyEvent event) {
		mouseMode.keyPressed(event);
	}

	@Override
	public void keyReleased(KeyEvent event) {
		mouseMode.keyPressed(event);
	}

    public void setUrl( URL url ) {
        dataUrl = url;
    }

    // TEMP public setting... LLF, 8/2/2013
    public List<JMenuItem> getLocalItems() {
        JMenuItem snapShot3dItem = new JMenuItem( "3D Snapshot" );

        List<JMenuItem> rtnVal = Arrays.asList( snapShot3dItem );
        snapShot3dItem.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent ae ) {
                launch3dViewer();
            }

        });

        return rtnVal;
    }

    /** Launches a 3D popup static-block viewer. */
    private void launch3dViewer() {
        try {
            org.janelia.it.workstation.gui.passive_3d.ViewTileManagerVolumeSource collector = new org.janelia.it.workstation.gui.passive_3d.ViewTileManagerVolumeSource(
                    getCamera(),
                    getViewport(),
                    getSliceAxis(),
                    getViewerInGround(),
                    dataUrl
            );

            org.janelia.it.workstation.gui.passive_3d.Snapshot3d snapshotViewer = new org.janelia.it.workstation.gui.passive_3d.Snapshot3d();
            snapshotViewer.launch( collector );

        } catch ( Exception ex ) {
            System.err.println("Failed to launch viewer: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
