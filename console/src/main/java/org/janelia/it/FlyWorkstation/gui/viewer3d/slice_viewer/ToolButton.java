package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Insets;

import javax.swing.Action;
import javax.swing.JButton;

// Tool bar button used to select a particular mouse or wheel mode
public class ToolButton extends JButton 
{
	private static final long serialVersionUID = 1L;

	public ToolButton(Action action)
	{
		super(action);
		init();
	}
	
	protected void init()
	{
		setHideActionText(true); // Want icon only; no text
		setRolloverEnabled(true); // No effect on Mac?
		setFocusable(false);
		setAlignmentX(CENTER_ALIGNMENT); // so it lines up with (centered) zoom slider
		setAlignmentY(CENTER_ALIGNMENT);
		setMargin(new Insets(0,0,0,0)); // keep the button small
		setMaximumSize(getPreferredSize()); // does this help?
	}
	
	@Override
	public void updateUI()
	{
		super.updateUI();
		init();
	}
}
