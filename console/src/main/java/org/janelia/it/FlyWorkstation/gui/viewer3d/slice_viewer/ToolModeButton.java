package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

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
	}
	
	@Override
	public void updateUI()
	{
		super.updateUI();
		init();
	}
}
