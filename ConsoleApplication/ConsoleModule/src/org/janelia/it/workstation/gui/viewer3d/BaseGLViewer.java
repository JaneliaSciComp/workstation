package org.janelia.it.workstation.gui.viewer3d;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JPopupMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Shared base class for Mip3d and LargeVolumeViewer
public abstract class BaseGLViewer 
//extends GLCanvas // in case heavyweight widget is preferred
extends GLJPanel // in case lightweight widget is required
implements MouseListener, MouseMotionListener,
MouseWheelListener
{
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(Mip3d.class);
	// setup OpenGL Version 2
	protected static GLProfile profile = null;
	protected static GLCapabilities capabilities = null;

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

	public JPopupMenu popupMenu;

	public BaseGLViewer() {
		super(capabilities);
        popupMenu = new JPopupMenu();
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }
    
    public static boolean isAvailable() {
		return capabilities!=null;
	}
	
	protected Point previousMousePos;
	protected boolean bMouseIsDragging = false;
	
	protected void maybeShowPopup(MouseEvent event)
	{
		if (event.isPopupTrigger()) {
			popupMenu.show(event.getComponent(),
					event.getX(), event.getY());
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
}
