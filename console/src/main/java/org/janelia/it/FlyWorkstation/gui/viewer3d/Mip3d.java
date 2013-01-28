package org.janelia.it.FlyWorkstation.gui.viewer3d;

import java.awt.Point;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.List;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mip3d 
//extends GLCanvas // in case heavyweight widget is preferred
extends GLJPanel // in case lightweight widget is required
implements MouseListener, MouseMotionListener, ActionListener,
	MouseWheelListener
{
    private static final Logger log = LoggerFactory.getLogger(Mip3d.class);
	
	private static final long serialVersionUID = 1L;
	// setup OpenGL Version 2
	static GLProfile profile = null;
	static GLCapabilities capabilities = null;

    static {
        try {
            profile = GLProfile.get(GLProfile.GL2);
            capabilities = new GLCapabilities(profile);
        } catch ( Throwable th ) {
            profile = null;
            capabilities = null;
            log.warn("JOGL is unavailable. No 3D images will be shown.");
        }
    }

    public static boolean isAvailable() {
		return capabilities!=null;
	}
	
	private Point previousMousePos;
	private boolean bMouseIsDragging = false;
	private MipRenderer renderer;
	public JPopupMenu popupMenu;
    private boolean clearOnLoad = true;
    private VolumeMaskBuilder volumeMaskBuilder;

	public enum InteractionMode {
		ROTATE,
		TRANSLATE,
		ZOOM
	}
	
	public Mip3d()
    {
        super(capabilities);
        renderer = new MipRenderer();
        addGLEventListener(renderer);
        setPreferredSize( new Dimension( 400, 400 ) );

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        // Context menu for resetting view
        popupMenu = new JPopupMenu();
        JMenuItem resetViewItem = new JMenuItem("Reset view");
        resetViewItem.addActionListener(this);
        popupMenu.add(resetViewItem);
    }

    //todo consider making these RGB value setting a preference, rather than this drill-in-setter.
    public void setRgbValues() {
        renderer.setRgbValues();
        repaint();
    }

    public void refresh() {
        renderer.refresh();
    }

    public void clear() {
        renderer.clear();
    }
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// System.out.println("reset view");
		renderer.resetView();
		repaint();
	}

    public void setClearOnLoad(boolean clearOnLoad) {
        this.clearOnLoad = clearOnLoad;
    }

    /**
     * Accumulates all data for masking, from the set of files provided, preparing them for
     * injection into th evolume being loaded.
     *
     * @param maskFiles list of all mask files to use against the signal volumes.
     */
    public void setMaskFiles(List<String> maskFiles, FileResolver resolver) {

        // Build the masking texture info.
        if (maskFiles != null) {
            VolumeMaskBuilder builder = new VolumeMaskBuilder();
            for ( String maskFile: maskFiles ) {
                VolumeLoader volumeLoader = new VolumeLoader( resolver );
                volumeLoader.loadVolume(maskFile);
                volumeLoader.populateVolumeAcceptor(builder);
            }
            volumeMaskBuilder = builder;
        }

    }

	public boolean loadVolume(String fileName, FileResolver resolver) {
        if (clearOnLoad)
            renderer.clear();

		VolumeLoader volumeLoader = new VolumeLoader(resolver);
		if (volumeLoader.loadVolume(fileName)) {
            VolumeBrick brick = new VolumeBrick(renderer);
			volumeLoader.populateVolumeAcceptor(brick);
            if ( volumeMaskBuilder != null ) {
                brick.setMaskTextureData( volumeMaskBuilder.getCombinedTextureData() );
            }
			renderer.addActor(brick);
			renderer.resetView();
			return true;
		}
		else
			return false;
	}
	
	private void maybeShowPopup(MouseEvent event)
	{
		if (event.isPopupTrigger()) {
			popupMenu.show(event.getComponent(),
					event.getX(), event.getY());
		}
	}
	
	@Override
    public void mouseDragged(MouseEvent event) {
        Point p1 = event.getPoint();
        if (! bMouseIsDragging) {
            bMouseIsDragging = true;
            previousMousePos = p1;
            return;
        }

        Point p0 = previousMousePos;
        Point dPos = new Point(p1.x-p0.x, p1.y-p0.y);

        InteractionMode mode = InteractionMode.ROTATE; // default drag mode is ROTATE
        if (event.isMetaDown()) // command-drag to zoom
            mode = InteractionMode.ZOOM;
        if (SwingUtilities.isMiddleMouseButton(event)) // middle drag to translate
            mode = InteractionMode.TRANSLATE;
        if (event.isShiftDown()) // shift-drag to translate
            mode = InteractionMode.TRANSLATE;

        if (mode == InteractionMode.TRANSLATE) {
            renderer.translatePixels(dPos.x, dPos.y, 0);
            repaint();
        }
        else if (mode == InteractionMode.ROTATE) {
            renderer.rotatePixels(dPos.x, dPos.y, 0);
            repaint();
        }
        else if (mode == InteractionMode.ZOOM) {
            renderer.zoomPixels(p1, p0);
            repaint();
        }

        previousMousePos = p1;
    }
    
    @Override
    public void mouseMoved(MouseEvent event) {}

	@Override
	public void mouseClicked(MouseEvent event) {
		bMouseIsDragging = false;
		// Double click to center
		if (event.getClickCount() == 2) {
			renderer.centerOnPixel(event.getPoint());
			repaint();
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		bMouseIsDragging = false;
		maybeShowPopup(e);
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		bMouseIsDragging = false;
		maybeShowPopup(event);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent event) {
		int notches = event.getWheelRotation();
		double zoomRatio = Math.pow(2.0, notches/50.0);
		renderer.zoom(zoomRatio);
		// Java does not seem to coalesce mouse wheel events,
		// giving the appearance of sluggishness.  So call repaint(),
		// not display().
		repaint();
	}

}
