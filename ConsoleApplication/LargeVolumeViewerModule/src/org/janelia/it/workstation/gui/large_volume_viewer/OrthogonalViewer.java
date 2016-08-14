package org.janelia.it.workstation.gui.large_volume_viewer;

import org.janelia.it.jacs.shared.lvv.AbstractTextureLoadAdapter;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Rotation3d;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.camera.Camera3d;
import org.janelia.it.workstation.gui.large_volume_viewer.action.*;
import org.janelia.it.workstation.gui.large_volume_viewer.action.MouseMode.Mode;
import org.janelia.it.workstation.gui.large_volume_viewer.camera.ObservableCamera3d;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.CameraListenerAdapter;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.MessageListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.RepaintListener;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.SkeletonActor;
import org.janelia.it.workstation.gui.opengl.GLActor;
import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.MouseHandler;
import org.janelia.it.workstation.gui.viewer3d.interfaces.AwtActor;
import org.janelia.it.workstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.workstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Vector;
import org.apache.commons.lang.SystemUtils;

/**
 * Intended replacement class for LargeVolumeViewer,
 * generalized for X,Y,Z orthogonal views
 * @author brunsc
 *
 */
public class OrthogonalViewer
// extends GLJPanel
implements MouseModalWidget, TileConsumer, RepaintListener
{
    private static final Logger log = LoggerFactory.getLogger(OrthogonalViewer.class);

	private Camera3d camera;
	private VolumeImage3d volume;
	private CoordinateAxis sliceAxis;
	private SliceRenderer renderer = new SliceRenderer();

    private MouseMode mouseMode;
    private MouseMode.Mode mouseModeId;
    private BasicMouseMode pointComputer = new BasicMouseMode();
    private MicronCoordsFormatter micronCoordsFormatter = new MicronCoordsFormatter(pointComputer);
    private WheelMode wheelMode;
    private WheelMode.Mode wheelModeId;
    // Popup menu
    MenuItemGenerator systemMenuItemGenerator;
    MenuItemGenerator modeMenuItemGenerator;
    private Point popupPoint = null; // Cache location of popup menu item
    //
    protected RubberBand rubberBand = new RubberBand();
    protected SkeletonActor skeletonActor;
    protected SliceActor sliceActor;
    private ReticleActor reticleActor;
    // 
    private List<AwtActor> hudActors = new Vector<>();

    private MessageListener messageListener;
    
	private TileServer tileServer;

    // May 2015 extend by containment, not inheritance
    // Store GLCanvas or GLJPanel in terms of these interfaces:
    GLDrawableWrapper glCanvas;
    
    public OrthogonalViewer(CoordinateAxis axis) {
		init(axis);
	}
	
	public OrthogonalViewer(CoordinateAxis axis, 
			GLCapabilities capabilities,
			GLCapabilitiesChooser chooser,
			GLContext sharedContext) 
	{
        // Use GLCanvas on Linux, for better frame rate on EnginFrame
        // But not on Windows, so we could still use Java2D decorations
        // if (false)
        // if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX) 
        if (SystemUtils.IS_OS_LINUX) 
        {
            glCanvas = new GLCanvasWrapper(capabilities, chooser, sharedContext) 
            {
                @Override
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    for (AwtActor actor : hudActors) {
                        if (actor.isVisible()) {
                            actor.paint(g2);
                        }
                    }
                }
            };
        } else { // Mac cannot use GLCanvas -- too broken
            glCanvas = new GLJPanelWrapper(capabilities, chooser, sharedContext) {
                @Override
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D)g;
                    for (AwtActor actor : hudActors)
                        if (actor.isVisible())
                            actor.paint(g2);
                }
            };
        }
        
		init(axis);
	}
	
    /**
     * @param messageListener the messageListener to set
     */
    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
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
		glCanvas.getGLAutoDrawable().addGLEventListener(renderer);
        setMouseMode(MouseMode.Mode.PAN);
        setWheelMode(WheelMode.Mode.ZOOM);
        rubberBand.setRepaintListener(this);
        glCanvas.getInnerAwtComponent().addMouseListener(this);
        glCanvas.getInnerAwtComponent().addMouseMotionListener(this);
        glCanvas.getInnerAwtComponent().addMouseWheelListener(this);
        glCanvas.getInnerAwtComponent().addKeyListener(this);
        pointComputer.setCamera(camera);
        pointComputer.setWidget(this, false);
        pointComputer.setViewerInGround(getViewerInGround());
        //
		renderer.setBackgroundColor(Color.black);
        // PopupMenu
        glCanvas.getInnerAwtComponent().addMouseListener(new MouseHandler() {
            @Override
            protected void popupTriggered(MouseEvent e) {
                // System.out.println("popup");
                if (e.isConsumed()) 
                    return;
                popupPoint = e.getPoint();
                JPopupMenu popupMenu = new JPopupMenu();
                // Mode specific menu items first
                List<JMenuItem> modeItems = modeMenuItemGenerator.getMenus(e);
                List<JMenuItem> systemMenuItems = systemMenuItemGenerator.getMenus(e);
                if ((modeItems.size() == 0) && (systemMenuItems.size() == 0))
                    return;
                if (modeItems.size() > 0) {
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
    
	public void setCamera(ObservableCamera3d camera) {
        if (camera == null)
            return;
        if (camera == this.camera)
            return;
        this.camera = camera;
        // Update image whenever camera changes
        camera.addCameraListener(new CameraListenerAdapter() {
            @Override
            public void viewChanged() {
                repaint();
            }
        });
        renderer.setCamera(camera);
        mouseMode.setCamera(camera);
        wheelMode.setCamera(camera);
        pointComputer.setCamera(camera);
        if (skeletonActor != null)
            skeletonActor.getModel().setCamera(camera);
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
        glCanvas.getInnerAwtComponent().requestFocusInWindow();
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
        sendLocationMessage(event.getPoint());
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        wheelMode.mouseWheelMoved(event);
        sendLocationMessage(event.getPoint());
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
            TraceMode traceMode = new TraceMode(skeletonActor.getModel().getSkeleton());
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
        glCanvas.getOuterJComponent().setToolTipText(mouseMode.getToolTipText());        
        this.modeMenuItemGenerator = mouseMode.getMenuItemGenerator();
    }

//	public AwtActor getReticle() {
//		return reticleActor;
//	}

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
        double dx = point.getX() - glCanvas.getInnerAwtComponent().getWidth() / 2.0;
        double dy = point.getY() - glCanvas.getInnerAwtComponent().getHeight() / 2.0;
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
        return glCanvas.getOuterJComponent();
    }

	public void setSkeletonActor(SkeletonActor skeletonActor) {
		if (this.skeletonActor == skeletonActor)
			return;
		this.skeletonActor = skeletonActor;
		skeletonActor.setZThicknessInPixels(getViewport().getDepth());
		skeletonActor.getModel().setCamera(camera);
        skeletonActor.getModel().getUpdater().addListener(this);
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

    @Override
    public boolean isShowing()
    {
        return glCanvas.getInnerAwtComponent().isShowing();
    }

    @Override
    public void repaint()
    {
        glCanvas.repaint();
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
			putValue(MNEMONIC_KEY, KeyEvent.VK_PAGE_UP);
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
			putValue(MNEMONIC_KEY, KeyEvent.VK_PAGE_DOWN);
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
		glCanvas.getInnerAwtComponent().removeComponentListener(tileServer); // in case it's already there
		glCanvas.getInnerAwtComponent().addComponentListener(tileServer);
	}
    
    
    Vec3 getPopupPositionInWorld()
    {
        if (popupPoint == null)
            return null;
        return pointComputer.worldFromPixel(popupPoint);
    }

    /** Convenience method to repeat this simple notification. */
    private void sendLocationMessage(Point point) {
        if (messageListener != null) {
            String msg = micronCoordsFormatter.formatForPresentation(point);
            messageListener.message(msg);
        }
    }

}
