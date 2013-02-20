package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class ColorChannelWidget extends JPanel 
{
	private static final long serialVersionUID = 1L;
	private int channelIndex;
	
	ColorChannelWidget(int channelIndex) {
		this.channelIndex = channelIndex;
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(new JSlider(JSlider.HORIZONTAL));
	}
}
