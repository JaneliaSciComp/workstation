package org.janelia.it.FlyWorkstation.gui.viewer3d;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class Mip3d 
extends GLCanvas 
// extends GLJPanel // in case lightweight widget is required
implements MouseListener, MouseMotionListener, ActionListener,
	MouseWheelListener
{
	private static final long serialVersionUID = 1L;
	// setup OpenGL Version 2
	static GLProfile profile = GLProfile.get(GLProfile.GL2);
	static GLCapabilities capabilities = new GLCapabilities(profile);

	private Point previousMousePos;
	private boolean bMouseIsDragging = false;
	private MipRenderer renderer;
	public JPopupMenu popupMenu;

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
        	setSize(400, 400);
        	addMouseListener(this);
        	addMouseMotionListener(this);
        	addMouseWheelListener(this);
        	// Context menu for resetting view
        	popupMenu = new JPopupMenu();
        	JMenuItem resetViewItem = new JMenuItem("Reset view");
        	resetViewItem.addActionListener(this);
        	popupMenu.add(resetViewItem);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		System.out.println("reset view");
		renderer.resetView();
		display();
	}
	
	public boolean loadVolume(String fileName)
	{
		VolumeLoader volumeLoader = new VolumeLoader();
		if (volumeLoader.loadVolume(fileName)) {
			VolumeBrick brick = new VolumeBrick(renderer);
			volumeLoader.populateBrick(brick);
			renderer.clear();
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
    public void mouseDragged(MouseEvent event)
    {
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
    			renderer.translatePixels(-dPos.x, dPos.y, 0);
    			display();
    		}
    		else if (mode == InteractionMode.ROTATE) {
    			renderer.rotatePixels(dPos.x, dPos.y, 0);
    			display();
    		}
    		else if (mode == InteractionMode.ZOOM) {
    			renderer.zoomPixels(p1, p0);
    			display();
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
			display();
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
		display();
	}
	
}
