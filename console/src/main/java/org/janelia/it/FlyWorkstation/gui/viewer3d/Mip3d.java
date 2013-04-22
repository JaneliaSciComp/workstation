package org.janelia.it.FlyWorkstation.gui.viewer3d;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.VolumeLoaderI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.MaskBuilderI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.RenderMapTextureBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.TextureDataI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class Mip3d extends BaseGLViewer implements ActionListener {
    public static final float DEFAULT_CROPOUT = 0.05f;
    /** All -1 sends signal: no cropping requried. */
    public static final float[] DEFAULT_CROP_COORDS = new float[] {
        -1.0f, -1.0f,  // startX, endX
        -1.0f, -1.0f,  // startY, endY
        -1.0f, -1.0f   // startZ, endZ
    };

    private static final long serialVersionUID = 1L;
	private MipRenderer renderer = new MipRenderer();
    private VolumeModel volumeModel = new VolumeModel();

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

    /** External addition to this conveniently-central popup menu. */
    public void addMenuAction( Action action ) {
        popupMenu.add( action );
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
		renderer.resetView();
		repaint();
	}

    /**
     * Load a simple signal volume.
     *
     * @param fileName for signal file data.
     * @param resolver flexibility: allows different ways of resolving the file, which may be server-based.
     * @return true if it worked; false otherwise.
     */
    public boolean loadVolume(String fileName, FileResolver resolver) {
        VolumeLoader volumeLoader = new VolumeLoader(resolver);
        if (volumeLoader.loadVolume(fileName)) {
            VolumeBrick brick = new VolumeBrick(renderer, volumeModel);
            volumeLoader.populateVolumeAcceptor(brick);

            addActorToRenderer(brick);
            return true;
        }
        else
            return false;
    }

    /**
     * Load a volume which may have a mask against it.
     *
     * @param volumeLoader for pushing data into the brick.
     * @param maskBuilder for mask file data.
     * @return true if it worked; false otherwise.
     */
    public boolean setVolume(
            VolumeLoaderI volumeLoader,
            MaskBuilderI maskBuilder,
            RenderMappingI renderMapping,
            float gamma
    ) {
        if ( volumeLoader != null ) {
            VolumeBrick brick = new VolumeBrick(renderer, volumeModel);
            volumeModel.setGammaAdjustment( gamma );
            if ( maskBuilder != null ) {
                brick.setMaskTextureData( maskBuilder.getCombinedTextureData() );

                RenderMapTextureBean renderMapTextureData = new RenderMapTextureBean();
                renderMapTextureData.setMapping( renderMapping );

                brick.setColorMapTextureData( renderMapTextureData );
            }
            volumeLoader.populateVolumeAcceptor(brick);

            addActorToRenderer(brick);
            return true;
        }
        else
            return false;
    }

    /**
     * A multi-thread-load-friendly overload of the set-volume method.  The texture objects may be
     * built at the caller's leisure, rather than being requested of passed-in builders.
     *
     * @param signalTexture for the intensity data.
     * @param maskTexture for the labels.
     * @param renderMapping for the mapping of labels to rendering techniques.
     * @param gamma brightness control.
     * @return true if sufficient params passed.
     */
    public boolean setVolume(
            TextureDataI signalTexture,
            TextureDataI maskTexture,
            RenderMappingI renderMapping,
            float gamma ) {
        if ( signalTexture != null ) {
            VolumeBrick brick = new VolumeBrick( renderer, volumeModel );
            volumeModel.setGammaAdjustment( gamma );
            brick.setTextureData( signalTexture );
            if ( maskTexture != null ) {
                brick.setMaskTextureData( maskTexture );

                RenderMapTextureBean renderMapTextureData = new RenderMapTextureBean();
                renderMapTextureData.setMapping( renderMapping );
                brick.setColorMapTextureData( renderMapTextureData );
            }

            addActorToRenderer(brick);

            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Load a volume which may have a mask against it.
     *
     * @param fileName for signal file data.
     * @param maskBuilder for mask file data.
     * @param resolver flexibility: allows different ways of resolving the file, which may be server-based.
     * @return true if it worked; false otherwise.
     */
	public boolean loadVolume(
            String fileName,
            MaskBuilderI maskBuilder,
            FileResolver resolver,
            RenderMappingI renderMapping,
            float gamma
    ) {
		VolumeLoader volumeLoader = new VolumeLoader(resolver);
		if (volumeLoader.loadVolume(fileName)) {
            VolumeBrick brick = new VolumeBrick(renderer, volumeModel);
            volumeModel.setGammaAdjustment( gamma );
			volumeLoader.populateVolumeAcceptor(brick);
            if ( maskBuilder != null ) {
                brick.setMaskTextureData( maskBuilder.getCombinedTextureData() );

                RenderMapTextureBean renderMapTextureData = new RenderMapTextureBean();
                renderMapTextureData.setMapping( renderMapping );

                brick.setColorMapTextureData( renderMapTextureData );
            }

            addActorToRenderer(brick);
			return true;
		}
		else
			return false;
	}

    /**
     * Load a volume which may have a color mask, rather than a voxel mask.
     *
     * @param fileName for signal file data.
     * @param colorMask for mask coloring--not mask texture. Color across all voxels, not fine-grained.
     * @param resolver flexibility: allows different ways of resolving the file, which may be server-based.
     * @return true if it worked; false otherwise.
     */
    public boolean loadVolume(
            String fileName, float[] colorMask,  FileResolver resolver, float gamma
    ) {
        VolumeLoader volumeLoader = new VolumeLoader(resolver);
        if (volumeLoader.loadVolume(fileName)) {
            VolumeBrick brick = new VolumeBrick(renderer, volumeModel);
            volumeModel.setColorMask( colorMask );
            volumeModel.setGammaAdjustment( gamma );
            volumeLoader.populateVolumeAcceptor(brick);

            addActorToRenderer(brick);
            return true;
        }
        else
            return false;
    }

    public void setGamma( float gamma ) {
        volumeModel.setGammaAdjustment( gamma );
        repaint();
    }

    public void setCropCoords( float[] cropCoords ) {
        volumeModel.setCropCoords( cropCoords );
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
        volumeModel.setColorMask( newValues );
        newValues[colorChannel]=isEnabled?1:0;
    }

    /** Special synchronized method, for adding actors. Supports multi-threaded brick-add. */
    private void addActorToRenderer(VolumeBrick brick) {
        synchronized ( this ) {
            renderer.addActor(brick);
            renderer.resetView();
        }
    }

}
