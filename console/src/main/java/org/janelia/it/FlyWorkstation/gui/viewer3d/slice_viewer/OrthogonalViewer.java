package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JComponent;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Rotation;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.janelia.it.FlyWorkstation.gui.viewer3d.camera.ObservableCamera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Camera3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.Viewport;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.BasicMouseMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.MouseMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.PanMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.TraceMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.WheelMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ZScanMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.ZoomMode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.action.MouseMode.Mode;
import org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer.skeleton.SkeletonActor;
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
implements MouseModalWidget
{
    private static final Logger log = LoggerFactory.getLogger(OrthogonalViewer.class);

	private Camera3d camera;
	private Viewport viewport;
	private VolumeImage3d volume;
	private CoordinateAxis viewAxis;
	private SliceRenderer renderer = new SliceRenderer();
    // Viewer orientation relative to canonical orientation.
    // Canonical orientation is x-right, y-down, z-away
    Rotation viewerInGround = new Rotation();

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

    public Signal1<String> statusMessageChanged = new Signal1<String>();
    protected Slot repaintSlot = new Slot() {
        @Override
        public void execute() {
            // System.out.println("repaint slot");
            repaint();
        }
    };

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
        setMouseMode(MouseMode.Mode.PAN);
        setWheelMode(WheelMode.Mode.ZOOM);
        rubberBand.changed.connect(repaintSlot);
	}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        rubberBand.paint(g2);
    }
    
	public void setCamera(ObservableCamera3d camera) {
		this.camera = camera;
	}
	
	public void setVolumeImage3d(VolumeImage3d volume) {
		this.volume = volume;
	}
    @Override
    public void mouseClicked(MouseEvent event) {
        mouseMode.mouseClicked(event);
        requestFocusInWindow();
    }

    @Override
    public void mouseEntered(MouseEvent event) {
        mouseMode.mouseEntered(event);
    }

    @Override
    public void mouseExited(MouseEvent event) {
        mouseMode.mouseExited(event);
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
            this.mouseMode = new PanMode();
        }
        else if (modeId == MouseMode.Mode.TRACE) {
            TraceMode traceMode = new TraceMode(skeletonActor.getSkeleton());
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

    @Override
    public void setWheelMode(WheelMode.Mode mode) {
        if (this.wheelModeId == mode)
            return;
        this.wheelModeId = mode;
        if (wheelModeId == WheelMode.Mode.ZOOM) {
            this.wheelMode = new ZoomMode();
        }
        else if (wheelModeId == WheelMode.Mode.SCAN) {
            this.wheelMode = new ZScanMode(volume);
        }
        this.wheelMode.setComponent(this);
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
        return viewport;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }
}
