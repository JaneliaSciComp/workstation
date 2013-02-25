package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.RenderMapTextureBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Map;

public class Mip3d extends BaseGLViewer {
	private static final long serialVersionUID = 1L;
	private MipRenderer renderer = new MipRenderer();
    private boolean clearOnLoad = true;
    private Map<Integer,byte[]> neuronNumToRGB;

	public enum InteractionMode {
		ROTATE,
		TRANSLATE,
		ZOOM
	}
	
	public Mip3d()
    {
		addGLEventListener(renderer);
        setPreferredSize( new Dimension( 400, 400 ) );

        // Context menu for resetting view
        JMenuItem resetViewItem = new JMenuItem("Reset view");
        resetViewItem.addActionListener(this);
        popupMenu.add(resetViewItem);
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
     * Sets the info needed for the shader to colorize the otherwise luminance-only masks.
     *
     * @param neuronNumToRGB mapping: id of neuron mask to RGB output.
     */
    public void setMaskColorMappings(Map<Integer,byte[]> neuronNumToRGB) {
        this.neuronNumToRGB = neuronNumToRGB;
    }

	public boolean loadVolume(String fileName, VolumeMaskBuilder volumeMaskBuilder, FileResolver resolver) {
        if (clearOnLoad)
            renderer.clear();

		VolumeLoader volumeLoader = new VolumeLoader(resolver);
		if (volumeLoader.loadVolume(fileName)) {
            VolumeBrick brick = new VolumeBrick(renderer);
			volumeLoader.populateVolumeAcceptor(brick);
            if ( volumeMaskBuilder != null ) {
                brick.setMaskTextureData( volumeMaskBuilder.getCombinedTextureData() );

                if ( neuronNumToRGB != null ) {
                    RenderMapTextureBean renderMapTextureData = new RenderMapTextureBean();
                    renderMapTextureData.setMapping(neuronNumToRGB);

                    // DEBUG ***
//                    for ( Integer nNum: neuronNumToRGB.keySet() ) {
//                        byte[] vals = neuronNumToRGB.get( nNum );
//                        System.out.println(nNum + " vs " + vals[0] + "," + vals[1] + "," + vals[2] + ": " + vals[3]);
//                    }
                    brick.setColorMapTextureData( renderMapTextureData );
                }
            }

			renderer.addActor(brick);
			renderer.resetView();
			return true;
		}
		else
			return false;
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
        float[] newValues = renderer.getRgbValues();
        newValues[colorChannel]=isEnabled?1:0;
        renderer.setRgbValues(newValues);
    }
}
