package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
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
	private final Map<String,JPanel> panelMap = new HashMap<String,JPanel>();
	
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
	
	public void addPanel(final String title, final Icon icon, final String tooltip, final JPanel panel) {
		
		JToggleButton button = new JToggleButton();
		TextIcon ti = new TextIcon(button, title);
		RotatedIcon ri = new RotatedIcon(ti, RotatedIcon.Rotate.DOWN);
		CompoundIcon ci = new CompoundIcon(Axis.Y_AXIS, 5, icon, ri);
		button.setFocusable(false);
		button.setIcon(ci);
		button.setToolTipText(tooltip);
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPanel(title);
			}
		});
		
		buttonPanel.add(button);
		buttonGroup.add(button);
		
		panelMap.put(title, panel);
	}
	
	public void showPanel(String name) {

        JPanel panel = panelMap.get(name);
        if (panel==null) throw new IllegalArgumentException("No such panel: "+name);
        
	    // Deactivate current panel
	    if (mainPanel.getComponentCount()>0) {
	        Component currComponent = mainPanel.getComponent(0);
	        if (currComponent instanceof ActivatableView) {
	            ((ActivatableView)currComponent).deactivate();
	        }
	    }
	    
		mainPanel.removeAll();
		
		// Activate new panel
		mainPanel.add(panel);
		revalidate();
		repaint();
		
		try {
    		if (panel instanceof Refreshable) {
    			((Refreshable)panel).refresh();
    		}
            if (panel instanceof ActivatableView) {
                ((ActivatableView)panel).activate();
            }
		}
		catch (Exception e) {
		    SessionMgr.getSessionMgr().handleException(e);
		}
	}
	
}
