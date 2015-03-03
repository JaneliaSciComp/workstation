/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer;

import java.awt.Insets;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.janelia.it.workstation.gui.large_volume_viewer.controller.ColorModelListener;
import org.janelia.it.workstation.gui.util.Icons;

/**
 * Encapsulates all multi-slider functionality for reuse.
 * @author fosterl
 */
public class SliderPanel extends JPanel {
    private static final String IMAGES_LOCK = "lock.png";
    private static final String IMAGES_LOCK_UNLOCK = "lock_unlock.png";

    private ColorChannelWidget colorChannelWidget_0;
	private ColorChannelWidget colorChannelWidget_1;
	private ColorChannelWidget colorChannelWidget_2;
	private ColorChannelWidget colorChannelWidget_3;
	private ColorChannelWidget[] colorWidgets;
    private JToggleButton lockBlackButton;
    private JToggleButton lockGrayButton;
    private JToggleButton lockWhiteButton;
	private JPanel colorLockPanel = new JPanel();
    private ImageColorModel imageColorModel;
    private ColorModelListener visibilityListener;

    public SliderPanel( ImageColorModel imageColorModel ) {
        setImageColorModel( imageColorModel );
    }
    
    public final void setImageColorModel( ImageColorModel imageColorModel ) {
        if ( colorWidgets != null ) {
            for ( ColorChannelWidget ccw: colorWidgets ) {
                this.remove( ccw );
            }
        }
        colorChannelWidget_0 = new ColorChannelWidget(0, imageColorModel);
        colorChannelWidget_1 = new ColorChannelWidget(1, imageColorModel);
        colorChannelWidget_2 = new ColorChannelWidget(2, imageColorModel);
        colorChannelWidget_3 = new ColorChannelWidget(3, imageColorModel);
        colorWidgets = new ColorChannelWidget[] {
            colorChannelWidget_0, 
            colorChannelWidget_1, 
            colorChannelWidget_2, 
            colorChannelWidget_3
        };
        if ( visibilityListener != null && imageColorModel != null ) {
            imageColorModel.removeColorModelListener(visibilityListener);
        }
        this.imageColorModel = imageColorModel;
        guiInit();
        updateLockButtons();
        setVisible(true);
    }

    @Override
    public void setVisible(boolean isVisible) {
        int sc = imageColorModel.getChannelCount();
        for ( int i = 0; i < sc; i++ ) {
            colorWidgets[ i ].setVisible( isVisible );
        }
        super.setVisible(isVisible);

        colorLockPanel.setVisible(sc > 1);
        // TODO Trying without success to get sliders to initially paint correctly
        SliderPanel.this.validate();
        SliderPanel.this.repaint();
    }
    
    public void updateLockButtons() {
        if ( lockBlackButton != null ) {
            lockBlackButton.setSelected(imageColorModel.isBlackSynchronized());
            lockGrayButton.setSelected(imageColorModel.isGammaSynchronized());
            lockWhiteButton.setSelected(imageColorModel.isWhiteSynchronized());
        }
    }
    
    public void guiInit() {
        if ( colorLockPanel == null ) {
            return;
        }
        
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Must add all widgets up front, because number of channels is a
        // dynamic value.
        for ( ColorChannelWidget widget: colorWidgets ) {
            widget.setVisible( false );
            this.add( widget );
        }
		
		// JPanel colorLockPanel = new JPanel();
		colorLockPanel.setVisible(false);
        colorLockPanel.removeAll();
        this.remove(colorLockPanel);
		this.add(colorLockPanel);
		colorLockPanel.setLayout(new BoxLayout(colorLockPanel, BoxLayout.X_AXIS));
		
		colorLockPanel.add(Box.createHorizontalStrut(30));
		
		lockBlackButton = new JToggleButton("");
		lockBlackButton.setToolTipText("Synchronize channel black levels");
		lockBlackButton.setMargin(new Insets(0, 0, 0, 0));
		lockBlackButton.setRolloverIcon(Icons.getIcon(IMAGES_LOCK_UNLOCK));
		lockBlackButton.setRolloverSelectedIcon(Icons.getIcon(IMAGES_LOCK));
		lockBlackButton.setIcon(Icons.getIcon(IMAGES_LOCK_UNLOCK));
		lockBlackButton.setSelectedIcon(Icons.getIcon(IMAGES_LOCK));
		lockBlackButton.setSelected(true);
		colorLockPanel.add(lockBlackButton);
		lockBlackButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				ImageColorModel colorModel = imageColorModel;
				if (colorModel == null)
					return;
				AbstractButton button = (AbstractButton)event.getSource();
				colorModel.setBlackSynchronized(button.isSelected());
			}
		});
		
		colorLockPanel.add(Box.createHorizontalGlue());

		lockGrayButton = new JToggleButton("");
		lockGrayButton.setToolTipText("Synchronize channel gray levels");
		lockGrayButton.setMargin(new Insets(0, 0, 0, 0));
		lockGrayButton.setRolloverIcon(Icons.getIcon(IMAGES_LOCK_UNLOCK));
		lockGrayButton.setRolloverSelectedIcon(Icons.getIcon(IMAGES_LOCK));
		lockGrayButton.setIcon(Icons.getIcon(IMAGES_LOCK_UNLOCK));
		lockGrayButton.setSelectedIcon(Icons.getIcon(IMAGES_LOCK));
		lockGrayButton.setSelected(true);
		colorLockPanel.add(lockGrayButton);
		lockGrayButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				ImageColorModel colorModel = imageColorModel;
				if (colorModel == null)
					return;
				AbstractButton button = (AbstractButton)event.getSource();
				colorModel.setGammaSynchronized(button.isSelected());
			}
		});
		
		colorLockPanel.add(Box.createHorizontalGlue());

		lockWhiteButton = new JToggleButton("");
		lockWhiteButton.setToolTipText("Synchronize channel white levels");
		lockWhiteButton.setMargin(new Insets(0, 0, 0, 0));
		lockWhiteButton.setRolloverIcon(Icons.getIcon(IMAGES_LOCK_UNLOCK));
		lockWhiteButton.setRolloverSelectedIcon(Icons.getIcon(IMAGES_LOCK));
		lockWhiteButton.setIcon(Icons.getIcon(IMAGES_LOCK_UNLOCK));
		lockWhiteButton.setSelectedIcon(Icons.getIcon(IMAGES_LOCK));
		lockWhiteButton.setSelected(true);
		colorLockPanel.add(lockWhiteButton);
		lockWhiteButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				ImageColorModel colorModel = imageColorModel;
				if (colorModel == null)
					return;
				AbstractButton button = (AbstractButton)event.getSource();
				colorModel.setWhiteSynchronized(button.isSelected());
			}
		});
		
		colorLockPanel.add(Box.createHorizontalStrut(30));
        if ( visibilityListener == null ) {
            visibilityListener = new ColorModelListener() {
                @Override
                public void colorModelChanged() {
                    setVisible(imageColorModel.getChannelCount() > 0);
                }
            };
        }
        imageColorModel.addColorModelListener(visibilityListener);
		
    }

}
