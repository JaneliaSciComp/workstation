package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Timer;
import javax.swing.Action;
import javax.swing.JButton;

// Tool bar button used to invoke an Action
// Automatically repeats if button is held down.
public class ToolButton extends JButton
implements MouseListener, ActionListener
{
	private static final long serialVersionUID = 1L;
	private int autoRepeatInitialDelay = 300; // milliseconds
	private int autoRepeatDelay = 100; // milliseconds
	private Timer autoRepeatTimer = new Timer(autoRepeatDelay, this);

	public ToolButton(Action action)
	{
		super(action);
		init();
	}
	
	protected void init()
	{
		addMouseListener(this);
		if (autoRepeatTimer != null) {
			autoRepeatTimer.setRepeats(true); // Just one shot at a time
			autoRepeatTimer.setInitialDelay(autoRepeatInitialDelay);
		}
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

	@Override
	public void mouseClicked(MouseEvent arg0) {}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	@Override
	public void mousePressed(MouseEvent event) {
		if (autoRepeatDelay > 0)
			autoRepeatTimer.restart();
	}
	
	@Override
	public void mouseReleased(MouseEvent arg0) {
		autoRepeatTimer.stop();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// Repeat action while button is held down
		getAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
	}
}
