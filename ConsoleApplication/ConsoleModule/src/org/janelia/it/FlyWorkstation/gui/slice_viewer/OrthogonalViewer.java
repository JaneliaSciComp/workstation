package org.janelia.it.FlyWorkstation.gui.slice_viewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import org.janelia.it.FlyWorkstation.geom.CoordinateAxis;
import org.janelia.it.FlyWorkstation.geom.Rotation3d;
import org.janelia.it.FlyWorkstation.geom.Vec3;
import org.janelia.it.FlyWorkstation.gui.camera.Camera3d;
import org.janelia.it.FlyWorkstation.gui.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.opengl.GLActor;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.action.BasicMouseMode;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.action.MouseMode;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.action.PanMode;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.action.TraceMode;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.action.WheelMode;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.action.ZScanMode;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.action.ZoomMode;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.action.MouseMode.Mode;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.skeleton.SkeletonActor;
import org.janelia.it.FlyWorkstation.gui.util.Icons;
import org.janelia.it.FlyWorkstation.gui.util.MouseHandler;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.AwtActor;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.janelia.it.FlyWorkstation.signal.Signal1;
import org.janelia.it.FlyWorkstation.signal.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intended replacement class for SliceViewer,
 * generalized for X,Y,Z orthogonal views
 * @author brunsc
 *
 */
public class OrthogonalViewer
extends GLJPanel
implements MouseModalWidget, TileConsumer
{
    private static final Logger log = LoggerFactory.getLogger(OrthogonalViewer.class);

	private Camera3d camera;
	private VolumeImage3d volume;
	private CoordinateAxis sliceAxis;
	private SliceRenderer renderer = new SliceRenderer();

    private MouseMode mouseMode;
    private MouseMode.Mode mouseModeId;
    private BasicMouseMode pointComputer = new BasicMouseMode();
    private WheelMode wheelMode;
    private WheelMode.Mode wheelModeId;
    // Popup menu
    MenuItemGenerator systemMenuItemGenerator;
    MenuItemGenerator modeMenuItemGenerator;
    //
    protected RubberBand rubberBand = new RubberBand();
    protected SkeletonActor skeletonActor;
    protected SliceActor sliceActor;
    private ReticleActor reticleActor;
    // 
    private List<AwtActor> hudActors = new Vector<AwtActor>();

    public Signal1<String> statusMessageChanged = new Signal1<String>();
    public Slot repaintSlot = new Slot() {
        @Override
        public void execute() {
            repaint();
        }
    };

	private TileServer tileServer;

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
	
	public void addActor(GLActor actor) {
		renderer.addActor(actor);
	}

	public void addActor(AwtActor actor) {
		hudActors.add(actor);
	}

	private void init(CoordinateAxis axis) {
		this.sliceAxis = axis;
		if (axis == CoordinateAxis.Z)
		    setViewerInGround(new Rotation3d()); // identity rotation, canonical orientation
		else if (axis == CoordinateAxis.X) // y-down, z-left, x-away
		    getViewerInGround().setFromCanonicalRotationAboutPrincipalAxis(
		            1, CoordinateAxis.Y);
		else // Y-away, x-right, z-up
		    getViewerInGround().setFromCanonicalRotationAboutPrincipalAxis(
		            3, CoordinateAxis.X);
		addGLEventListener(renderer);
        setMouseMode(MouseMode.Mode.PAN);
        setWheelMode(WheelMode.Mode.ZOOM);
        rubberBand.changed.connect(repaintSlot);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);
        pointComputer.setCamera(camera);
        pointComputer.setWidget(this, false);
        pointComputer.setViewerInGround(getViewerInGround());
        //
		renderer.setBackgroundColor(Color.black);
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
        hudActors.add(rubberBand);
        reticleActor = new ReticleActor(getViewport());
        reticleActor.setVisible(false);
        hudActors.add(reticleActor);
	}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        for (AwtActor actor : hudActors)
        	if (actor.isVisible())
        		actor.paint(g2);
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
        pointComputer.setCamera(camera);
        if (skeletonActor != null)
            skeletonActor.setCamera(camera);
	}
	
	public void setSystemMenuItemGenerator(MenuItemGenerator systemMenuItemGenerator) 
	{
		this.systemMenuItemGenerator = systemMenuItemGenerator;
	}

	public void setVolumeImage3d(VolumeImage3d volume) {
		this.volume = volume;
		// TODO put bounding box in BasicMouseMode
		if (mouseModeId == MouseMode.Mode.PAN) {
		    ((PanMode)mouseMode).setBoundingBox(volume.getBoundingBox3d());
		}
		if (mouseModeId == MouseMode.Mode.TRACE) {
		    ((TraceMode)mouseMode).setBoundingBox(volume.getBoundingBox3d());
		}
	}
    @Override
    public void mouseClicked(MouseEvent event) {
        mouseMode.mouseClicked(event);
        requestFocusInWindow();
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        mouseMode.mouseEntered(event);
        if (! reticleActor.isVisible()) {
        	reticleActor.setVisible(true);
        	repaint();
        }
    }

    @Override
    public void mouseExited(MouseEvent event) {
        mouseMode.mouseExited(event);
        if (reticleActor.isVisible()) {
        	reticleActor.setVisible(false);
        	repaint();
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
        mouseMode.mousePressed(event);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        mouseMode.mouseReleased(event);
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
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        wheelMode.mouseWheelMoved(event);
    }

    @Override
    public void setMouseMode(Mode modeId) {
        if (modeId == this.mouseModeId)
            return; // no change
        this.mouseModeId = modeId;
        if (modeId == MouseMode.Mode.PAN) {
            PanMode panMode = new PanMode();
            panMode.setViewerInGround(getViewerInGround());
            if (volume != null)
                panMode.setBoundingBox(volume.getBoundingBox3d());
            this.mouseMode = panMode;
        }
        else if (modeId == MouseMode.Mode.TRACE) {
            TraceMode traceMode = new TraceMode(skeletonActor.getSkeleton());
            traceMode.setViewport(getViewport());
            traceMode.setActor(skeletonActor);
            traceMode.setViewerInGround(getViewerInGround());
            if (volume != null)
                traceMode.setBoundingBox(volume.getBoundingBox3d());
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

	public AwtActor getReticle() {
		return reticleActor;
	}

	@Override
	public Rotation3d getViewerInGround() {
        return renderer.getViewerInGround();
    }

    public void setViewerInGround(Rotation3d viewerInGround) {
        renderer.setViewerInGround(viewerInGround);
        pointComputer.setViewerInGround(viewerInGround);
        if (this.mouseMode instanceof BasicMouseMode)
        	((BasicMouseMode)this.mouseMode).setViewerInGround(viewerInGround);
    }

    @Override
    public void setWheelMode(WheelMode.Mode mode) {
        if (this.wheelModeId == mode)
            return;
        this.wheelModeId = mode;
        if (wheelModeId == WheelMode.Mode.ZOOM) {
            this.wheelMode = new ZoomMode();
        }
        else if (wheelModeId == WheelMode.Mode.SCAN) {
            ZScanMode scanMode = new ZScanMode(volume);
            scanMode.setSliceAxis(sliceAxis);
            if (tileServer != null) {
            	AbstractTextureLoadAdapter loadAdapter = tileServer.getLoadAdapter();
            	if (loadAdapter != null) {
            		scanMode.setTileFormat(loadAdapter.getTileFormat());
            	}
            }
            this.wheelMode = scanMode;
        }
        this.wheelMode.setWidget(this, false);
        this.wheelMode.setCamera(camera);
    }

    @Override
    public Point2D getPixelOffsetFromCenter(Point2D point) {
        double dx = point.getX() - getWidth() / 2.0;
        double dy = point.getY() - getHeight() / 2.0;
        return new Point2D.Double(dx, dy);
    }

    @Override
    public RubberBand getRubberBand() {
        return rubberBand;
    }

    @Override
    public Viewport getViewport() {
        return renderer.getViewport();
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

	public void setSkeletonActor(SkeletonActor skeletonActor) {
		if (this.skeletonActor == skeletonActor)
			return;
		this.skeletonActor = skeletonActor;
		skeletonActor.setZThicknessInPixels(getViewport().getDepth());
		skeletonActor.setCamera(camera);
		skeletonActor.skeletonActorChangedSignal.connect(repaintSlot);
        renderer.addActor(skeletonActor);
	}

	@Override
	public Camera3d getCamera() {
		return camera;
	}

	@Override
	public MouseMode getMouseMode() {
		return mouseMode;
	}

	public SliceActor getSliceActor() {
		return sliceActor;
	}

	public void setSliceActor(SliceActor sliceActor) {
		if (this.sliceActor == sliceActor)
			return;
		this.sliceActor = sliceActor;
		addActor(sliceActor);
	}

	@Override
	public CoordinateAxis getSliceAxis() {
		return sliceAxis;
	}

	public void incrementSlice(int sliceCount) {
		if (sliceCount == 0)
			return;
		if (camera == null)
			return;
		if (volume == null)
			return;
		int ix = sliceAxis.index(); // X, Y, or Z
		double res = volume.getResolution(ix);
		// At lower zoom, we must jump multiple slices to see a different image
		int deltaSlice = 1;
		TileSet someTiles = sliceActor.getViewTileManager().getLatestTiles();
		if (someTiles.size() > 0)
			deltaSlice = someTiles.iterator().next().getIndex().getDeltaSlice();
		Vec3 dFocus = new Vec3(0,0,0);
		dFocus.set(ix, sliceCount * res * deltaSlice);
		camera.setFocus(camera.getFocus().plus(dFocus));
	}
	
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
		mouseMode.keyReleased(event);
	}

	public static class IncrementSliceAction extends AbstractAction {
		private int increment;
		private OrthogonalViewer viewer;

		IncrementSliceAction(int increment, OrthogonalViewer viewer) {
			this.increment = increment;
			this.viewer = viewer;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			viewer.incrementSlice(increment);
		}
	}
	
	public static class PreviousSliceAction extends IncrementSliceAction {
		PreviousSliceAction(OrthogonalViewer viewer) {
			super(-1, viewer);
			putValue(NAME, "Previous "+viewer.sliceAxis.getName()+" Slice");
			putValue(SMALL_ICON, Icons.getIcon("z_stack_up.png"));
			putValue(MNEMONIC_KEY, (int)KeyEvent.VK_PAGE_UP);
			KeyStroke accelerator = KeyStroke.getKeyStroke(
				KeyEvent.VK_PAGE_UP, 0);
			putValue(ACCELERATOR_KEY, accelerator);
			putValue(SHORT_DESCRIPTION,
					"View previous "+viewer.sliceAxis.getName()+" slice"
					+"\n (Shortcut: "+accelerator+")"
					);		
		}
	}
	
	public static class NextSliceAction extends IncrementSliceAction {
		NextSliceAction(OrthogonalViewer viewer) {
			super(1, viewer);
			putValue(NAME, "Next "+viewer.sliceAxis.getName()+" Slice");
			putValue(SMALL_ICON, Icons.getIcon("z_stack_down.png"));
			putValue(MNEMONIC_KEY, (int)KeyEvent.VK_PAGE_DOWN);
			KeyStroke accelerator = KeyStroke.getKeyStroke(
				KeyEvent.VK_PAGE_DOWN, 0);
			putValue(ACCELERATOR_KEY, accelerator);
			putValue(SHORT_DESCRIPTION,
					"View next "+viewer.sliceAxis.getName()+" slice"
					+"\n (Shortcut: "+accelerator+")"
					);		
		}
	}

	public void setTileServer(TileServer tileServer) {
		if (tileServer == this.tileServer)
			return;
		this.tileServer = tileServer;
		// Prepare to update tile set if viewer resizes
		removeComponentListener(tileServer); // in case it's already there
		addComponentListener(tileServer);
	}

	@Override
	public Slot getRepaintSlot() {
		return repaintSlot;
	}


}
