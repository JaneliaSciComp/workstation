package org.janelia.it.workstation.gui.viewer3d;

import org.janelia.it.workstation.gui.opengl.GLActor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class Mip3d extends BaseGLViewer implements ActionListener {

    private static final long serialVersionUID = 1L;
	private MipRenderer renderer = new MipRenderer();
    private VolumeModel volumeModel = renderer.getVolumeModel();
    private double axisLengthDivisor;

    public enum InteractionMode {
		ROTATE,
		TRANSLATE,
		ZOOM
	}
	
	public Mip3d() {
		addGLEventListener(renderer);
        setPreferredSize( new Dimension( 400, 400 ) );

        // Context menu for resetting view
        JMenuItem resetViewItem = new JMenuItem("Reset view");
        resetViewItem.addActionListener(this);
        popupMenu.add(resetViewItem);
    }

    public VolumeModel getVolumeModel() {
        return volumeModel;
    }

    /** External addition to this conveniently-central popup menu. */
    public void addMenuAction( Action action ) {
        popupMenu.add( action );
    }

    public void releaseMenuActions() {
        popupMenu.removeAll();
    }

    public void refresh() {
        volumeModel.setVolumeUpdate();
    }

    public void refreshRendering() {
        volumeModel.setRenderUpdate();
    }

    public void clear() {
        renderer.clear();
    }
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// System.out.println("reset view");
        resetView();
	}

    public void resetView() {
        renderer.resetView();
        repaint();
    }

    public void setResetFirstRedraw(boolean resetFirstRedraw) {
        renderer.setResetFirstRedraw(resetFirstRedraw);
    }

    public double getAxisLengthDivisor() {
        return axisLengthDivisor;
    }

    /**
     * Add any actor to this Mip as desired.
     */
    public void addActor(GLActor actor) {
        addActorToRenderer(actor);
    }

    public void setGamma( float gamma ) {
        volumeModel.setGammaAdjustment( gamma );
        repaint();
    }

    public void setCropOutLevel( float cropOutLevel ) {
        volumeModel.setCropOutLevel( cropOutLevel );
        repaint();
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
	public void mouseWheelMoved(MouseWheelEvent event) {
		int notches = event.getWheelRotation();
		double zoomRatio = Math.pow(2.0, notches/50.0);
		renderer.zoom(zoomRatio);
		// Java does not seem to coalesce mouse wheel events,
		// giving the appearance of sluggishness.  So call repaint(),
		// not display().
		repaint();
	}

    public void toggleRGBValue(int colorChannel, boolean isEnabled) {
        float[] newValues = volumeModel.getColorMask();
        volumeModel.setColorMask(newValues);
        newValues[colorChannel]=isEnabled?1:0;
    }

    /** Special synchronized method, for adding actors. Supports multi-threaded brick-add. */
    private void addActorToRenderer(GLActor brick) {
        synchronized ( this ) {
            renderer.addActor(brick);
            renderer.resetView();
        }
    }

}
