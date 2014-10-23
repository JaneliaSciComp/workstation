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
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Encapsulates all multi-slider functionality for reuse.
 * @author fosterl
 */
public class SliderPanel extends JPanel {
	private ColorChannelWidget colorChannelWidget_0;
	private ColorChannelWidget colorChannelWidget_1;
	private ColorChannelWidget colorChannelWidget_2;
	private ColorChannelWidget colorChannelWidget_3;
	private final ColorChannelWidget[] colorWidgets;
    private JToggleButton lockBlackButton;
    private JToggleButton lockGrayButton;
    private JToggleButton lockWhiteButton;
	private JPanel colorLockPanel = new JPanel();
    private ImageColorModel imageColorModel;

    public SliderPanel( ImageColorModel imageColorModel ) {
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
        this.imageColorModel = imageColorModel;
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
        lockBlackButton.setSelected(imageColorModel.isBlackSynchronized());
        lockGrayButton.setSelected(imageColorModel.isGammaSynchronized());
        lockWhiteButton.setSelected(imageColorModel.isWhiteSynchronized());
    }
    
    public void guiInit() {
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Must add all widgets up front, because number of channels is a
        // dynamic value.
        for ( ColorChannelWidget widget: colorWidgets ) {
            widget.setVisible( false );
            this.add( widget );
        }
		
		// JPanel colorLockPanel = new JPanel();
		colorLockPanel.setVisible(false);
		this.add(colorLockPanel);
		colorLockPanel.setLayout(new BoxLayout(colorLockPanel, BoxLayout.X_AXIS));
		
		colorLockPanel.add(Box.createHorizontalStrut(30));
		
		lockBlackButton = new JToggleButton("");
		lockBlackButton.setToolTipText("Synchronize channel black levels");
		lockBlackButton.setMargin(new Insets(0, 0, 0, 0));
		lockBlackButton.setRolloverIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock_unlock.png")));
		lockBlackButton.setRolloverSelectedIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock.png")));
		lockBlackButton.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock_unlock.png")));
		lockBlackButton.setSelectedIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock.png")));
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
		lockGrayButton.setRolloverIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock_unlock.png")));
		lockGrayButton.setRolloverSelectedIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock.png")));
		lockGrayButton.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock_unlock.png")));
		lockGrayButton.setSelectedIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock.png")));
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
		lockWhiteButton.setRolloverIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock_unlock.png")));
		lockWhiteButton.setRolloverSelectedIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock.png")));
		lockWhiteButton.setIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock_unlock.png")));
		lockWhiteButton.setSelectedIcon(new ImageIcon(QuadViewUi.class.getResource("/images/lock.png")));
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

		imageColorModel.getColorModelInitializedSignal().connect(new org.janelia.it.workstation.signal.Slot() {
			@Override
			public void execute() {
				setVisible(imageColorModel.getChannelCount() > 0);
			}
		});
		
    }

}
