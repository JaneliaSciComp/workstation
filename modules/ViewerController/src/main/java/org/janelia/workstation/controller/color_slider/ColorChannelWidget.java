package org.janelia.workstation.controller.color_slider;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.workstation.controller.model.color.ChannelColorModel;
import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.workstation.controller.widgets.SimpleIcons;
import org.janelia.workstation.controller.widgets.ToolButton;
import org.janelia.workstation.controller.widgets.ToolModeButton;

import org.janelia.workstation.controller.listener.ColorModelInitListener;
import org.janelia.workstation.controller.listener.ColorModelListener;

public class ColorChannelWidget extends JPanel 
{
	private static final long serialVersionUID = 1L;
	private final int channelIndex;
	private VisibilityButton visibilityButton;
	private ImageColorModel imageColorModel;
	private ColorButton colorButton;
	private UglyColorSlider slider;
	private TextColorSlider textSlider;
	private JButton sliderToggle;
	
	ColorChannelWidget(int channelIndex, ImageColorModel imageColorModel) {
		this.channelIndex = channelIndex;
		this.imageColorModel = imageColorModel;
		this.colorButton = new ColorButton(); // construct after imageColorModel is set...
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		// A: We want the reset color button right aligned
		// B: All components in a BoxLayout should have the same alignment
		// C: Therefore, the slider widget should be right aligned
		// setAlignmentX(Component.RIGHT_ALIGNMENT);
		visibilityButton = new VisibilityButton();
		add(visibilityButton);
		slider = new UglyColorSlider(channelIndex, imageColorModel);
		add(slider);

		// text input for colors; it's hidden behind a little # button
		// not sure where the asymmetry is, but there is enough space here already,
		//	don't need another spacer
		// add(Box.createRigidArea(new Dimension(10, 0)));
		textSlider = new TextColorSlider(channelIndex, imageColorModel);
		add(textSlider);

		sliderToggle = new JButton("#");
		sliderToggle.addActionListener(e -> textSlider.setVisible(!textSlider.isVisible()));
		add(Box.createRigidArea(new Dimension(10, 0)));
		add(sliderToggle);
		textSlider.setVisible(false);

		add(Box.createRigidArea(new Dimension(10, 0)));
		add(colorButton);
		updateColor();
        updateVisibility();
        imageColorModel.addColorModelInitListener(new ColorModelInitListener() {
            @Override
            public void colorModelInit() {
				updateColor();
                updateVisibility();
            }            
        });
		imageColorModel.addColorModelListener(new ColorModelListener() {
            @Override
            public void colorModelChanged() {
                updateColor();
                updateVisibility();
            }            
        });
	}

	public ImageColorModel getImageColorModel() {
		return imageColorModel;
	}

	public void setWhiteColor(Color whiteColor) {
		if (whiteColor == null)
			return;
		if (colorButton != null)
			colorButton.setColor(whiteColor);
		if (slider != null)
			slider.setWhiteColor(whiteColor);
		if (imageColorModel == null)
			return;
		if (imageColorModel.getChannelCount() <= channelIndex)
			return;
		ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
		ccm.setColor(whiteColor);
	}

	public void updateColor() {
		if (imageColorModel == null)
			return;
		if (imageColorModel.getChannelCount() <= channelIndex)
			return;
		ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
		setWhiteColor(ccm.getColor());
	}

    public void updateVisibility() {
        if (imageColorModel == null)
            return;
        if (imageColorModel.getChannelCount() <= channelIndex)
            return;
        ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
        visibilityButton.setSelected(ccm.isVisible());
    }
	
	class VisibilityButton extends ToolModeButton 
	{
		private static final long serialVersionUID = 1L;
		public VisibilityButton() {
			super(new ChannelVisibilityAction(channelIndex));
			setIcon(SimpleIcons.getIcon("closed_eye.png"));
            setRolloverIcon(SimpleIcons.getIcon("closed_eye.png"));
			setSelectedIcon(SimpleIcons.getIcon("eye.png"));
			setRolloverSelectedIcon(SimpleIcons.getIcon("eye.png"));
			setSelected(true);
			setToolTipText("Click to show/hide color channel " + channelIndex);
			setMaximumSize(getPreferredSize()); // after loading icon on Windows
		}
	}
	
	
	class ColorButton extends ToolButton
	{
		private static final long serialVersionUID = 1L;
		
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		private ImageIcon icon = new ImageIcon(image);
		private Color whiteColor = Color.white;
		
		public ColorButton() {
			super(new ChannelColorAction(channelIndex));
			setColor(whiteColor);
			setIcon(icon);
			setToolTipText("Click to change channel "+channelIndex+" color");
			setMaximumSize(getPreferredSize()); // after loading icon on Windows
		}
		
		public void setColor(Color color) {
			if (color == null)
				return;
			if (whiteColor.equals(color))
				return;
			whiteColor = color;
			int c = color.getRGB();
			for (int x = 0; x < 16; ++x)
				for (int y = 0; y < 16; ++y)
					image.setRGB(x, y, c);
			repaint();
		}
	}
	
	class ChannelVisibilityAction extends AbstractAction
	{
		private static final long serialVersionUID = 1L;
		
		public ChannelVisibilityAction(int channelIndex) {
		}
		
		@Override
		public void actionPerformed(ActionEvent event) {
			if (imageColorModel.getChannelCount() <= channelIndex)
				return;
			ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
			VisibilityButton button = (VisibilityButton)event.getSource();
			ccm.setVisible(button.isSelected());
			// System.out.println("visibility");
		}
	}
	
	
	class ChannelColorAction extends AbstractAction
	{
		private static final long serialVersionUID = 1L;
		JDialog colorDialog;
		JColorChooser colorChooser;
		private Color originalColor;
		private int channelIndex = 0;
		
		public ChannelColorAction(int channelIndexParam) 
		{
			this.channelIndex = channelIndexParam;
			
			ActionListener cancelListener = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					// System.out.println("cancel");
					setWhiteColor(originalColor);
				}
			};
			ActionListener okListener = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					// System.out.println("ok");
					setWhiteColor(colorChooser.getColor());
				}
			};
			colorChooser = new JColorChooser(Color.pink);
			colorChooser.getSelectionModel().addChangeListener(new ChangeListener() 
			{
				@Override
				public void stateChanged(ChangeEvent arg0) {
					setWhiteColor(colorChooser.getColor());
				}
			});
			colorDialog = JColorChooser.createDialog(ColorChannelWidget.this,
					"Select color for channel "+channelIndex,
					false, // not modal
					colorChooser,
					okListener, cancelListener);
			colorDialog.setVisible(false);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (imageColorModel == null)
				return;
			if (imageColorModel.getChannelCount() <= channelIndex)
				return;
			ChannelColorModel ccm = imageColorModel.getChannel(channelIndex);
			originalColor = ccm.getColor();
			colorChooser.setColor(originalColor);
			colorDialog.setVisible(true);
		}
	}
	
}
