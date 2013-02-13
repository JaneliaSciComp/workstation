package org.janelia.it.FlyWorkstation.gui.viewer3d;

import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.util.panels.ChannelSelectionPanel;
import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeMaskBuilder;
import org.janelia.it.FlyWorkstation.gui.viewer3d.resolver.FileResolver;
import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.ColorMapTextureBean;

public class Mip3d 
extends BaseGLViewer
{
    public static final int RGBDIALOG_WIDTH = 300;
    public static final int RGBDIALOG_HEIGHT = 150;
	
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

    //todo consider making these RGB value setting a preference, rather than this drill-in-setter.
    public void setRgbChannelValues() {
        if ( renderer.getRgbValues() != null ) {
            float[] colorMask = renderer.getRgbValues();
            final JDialog rgbPopup = new JDialog();
            rgbPopup.setSize( new Dimension(RGBDIALOG_WIDTH, RGBDIALOG_HEIGHT) );
            rgbPopup.setTitle("Red, Green or Blue Channels");
            rgbPopup.setLayout( new BorderLayout() );
            final ChannelSelectionPanel channelSelectionPanel = new ChannelSelectionPanel( colorMask );

            JButton cancelButton = new JButton( "Cancel" );
            cancelButton.setActionCommand( "Cancel" );
            JButton okButton = new JButton( "OK" );
            okButton.setActionCommand("OK");

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout( new FlowLayout() );
            buttonPanel.add( okButton );
            buttonPanel.add( cancelButton );

            ActionListener buttonListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    rgbPopup.setVisible( false );
                    if ( ((AbstractButton)e.getSource()).getActionCommand().equals( "OK" ) ) {
                        float[] rgbChoices = channelSelectionPanel.getRgbChoices();
                        renderer.setRgbValues( rgbChoices );
                        Mip3d.this.repaint();
                    }
                }
            };

            cancelButton.addActionListener( buttonListener );
            okButton.addActionListener( buttonListener );

            rgbPopup.add( channelSelectionPanel, BorderLayout.CENTER );
            rgbPopup.add( buttonPanel, BorderLayout.SOUTH );
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int locX = (int)screenSize.getWidth() / 4 - (RGBDIALOG_WIDTH / 2);
            int locY = (int)screenSize.getHeight() / 2 - (RGBDIALOG_HEIGHT / 2);
            rgbPopup.setLocation( locX, locY );
            rgbPopup.setVisible(true);
        }

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
                    ColorMapTextureBean colorMapTextureData = new ColorMapTextureBean();
                    colorMapTextureData.setMapping( neuronNumToRGB );
                    brick.setColorMapTextureData( colorMapTextureData );
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
}
