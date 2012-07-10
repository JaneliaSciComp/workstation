package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.util.CompoundIcon;
import org.janelia.it.FlyWorkstation.gui.util.CompoundIcon.Axis;
import org.janelia.it.FlyWorkstation.gui.util.RotatedIcon;
import org.janelia.it.FlyWorkstation.gui.util.TextIcon;

/**
 * An IntelliJ-like side panel with vertical icons to switch between panels.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class VerticalPanelPicker extends JPanel {

	private final JPanel mainPanel;
	private final JPanel buttonPanel;
	private final ButtonGroup buttonGroup;
	
	public VerticalPanelPicker() {
		setLayout(new BorderLayout());
		
		buttonGroup = new ButtonGroup();
		
		mainPanel = new JPanel(new GridLayout(1,1));
		add(mainPanel, BorderLayout.CENTER);
		
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		add(buttonPanel, BorderLayout.EAST);
	}
	
	public void addPanel(Icon icon, String title, String tooltip, final JPanel panel) {
		
		JToggleButton button = new JToggleButton();
		TextIcon ti = new TextIcon(button, title);
		RotatedIcon ri = new RotatedIcon(ti, RotatedIcon.Rotate.DOWN);
		CompoundIcon ci = new CompoundIcon(Axis.Y_AXIS, 5, icon, ri);
		button.setIcon(ci);
		button.setToolTipText(tooltip);
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPanel(panel);
			}
		});
		
		buttonPanel.add(button);
		buttonGroup.add(button);
		
		// Show the first panel that was added
		if (mainPanel.getComponentCount()==0) {
			button.setSelected(true);
			showPanel(panel);
		}
	}
	
	public void showPanel(JPanel panel) {
		mainPanel.removeAll();
		mainPanel.add(panel);
		revalidate();
		repaint();
		if (panel instanceof Refreshable) {
			((Refreshable)panel).refresh();
		}
	}
	
}
