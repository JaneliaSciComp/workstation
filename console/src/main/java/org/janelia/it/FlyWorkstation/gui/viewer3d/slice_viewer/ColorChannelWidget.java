package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.FlyWorkstation.gui.util.Icons;

public class ColorChannelWidget extends JPanel 
{
	private static final long serialVersionUID = 1L;
	private int channelIndex;
	
	ColorChannelWidget(int channelIndex) {
		this.channelIndex = channelIndex;
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		// A: We want the reset color button right aligned
		// B: All components in a BoxLayout should have the same alignment
		// C: Therefore, the slider widget should be right aligned
		// setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(new VisibilityButton());
		add(new JSlider(JSlider.HORIZONTAL));
		add(new ColorButton());
	}
	
	
	class VisibilityButton extends ToolModeButton 
	{
		private static final long serialVersionUID = 1L;
		public VisibilityButton() {
			super(new ChannelVisibilityAction(channelIndex));
			setIcon(Icons.getIcon("closed_eye.png"));
			setSelectedIcon(Icons.getIcon("eye.png"));
			setRolloverSelectedIcon(Icons.getIcon("eye.png"));
			setSelected(true);
			setToolTipText("Click to show/hide color channel");
		}
	}
	
	
	class ColorButton extends ToolButton
	{
		private static final long serialVersionUID = 1L;
		public ColorButton() {
			super(new ChannelColorAction(channelIndex));
			
			BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
			for (int x = 0; x < 16; ++x)
				for (int y = 0; y < 16; ++y)
					image.setRGB(x, y, Color.orange.getRGB());
			ImageIcon icon = new ImageIcon(image);
			setIcon(icon);
			setToolTipText("Click to change channel "+channelIndex+"color");
		}
	}
	
	class ChannelVisibilityAction extends AbstractAction
	{
		private static final long serialVersionUID = 1L;
		
		public ChannelVisibilityAction(int channelIndex) {
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			System.out.println("visibility"); // TODO
		}
	}
	
	
	class ChannelColorAction extends AbstractAction
	{
		private static final long serialVersionUID = 1L;
		JDialog colorDialog;
		JColorChooser colorChooser;
		
		public ChannelColorAction(int channelIndex) 
		{
			ActionListener actionListener = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					System.out.println("action!");
				}
			};
			PropertyChangeListener colorListener = new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					System.out.println("property changed "+event.getPropertyName());
				}
			};
			colorChooser = new JColorChooser(Color.pink);
			colorChooser.getSelectionModel().addChangeListener(new ChangeListener() 
			{
				@Override
				public void stateChanged(ChangeEvent arg0) {
					System.out.println("Color changed");
				}
			});
			colorDialog = JColorChooser.createDialog(ColorChannelWidget.this,
					"Select color for channel "+channelIndex,
					false, // not modal
					colorChooser,
					actionListener, actionListener);
			colorDialog.setVisible(false);
			colorDialog.addPropertyChangeListener(colorListener);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			colorDialog.setVisible(true);
		}
	}
	
}
