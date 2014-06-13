package org.janelia.it.workstation.gui.slice_viewer;

import java.awt.Insets;

import javax.swing.Action;
import javax.swing.JToggleButton;

// Tool bar button used to select a particular mouse or wheel mode
public class ToolModeButton extends JToggleButton 
{
	private static final long serialVersionUID = 1L;

	public ToolModeButton(Action action)
	{
		super(action);
		init();
	}
	
	protected void init()
	{
		setHideActionText(true); // Want icon only; no text
		setFocusable(false); // Remove that useless blue glow on Mac
		setRolloverEnabled(true); // No effect on Mac?
		setHideActionText(true); // Want icon only; no text
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
