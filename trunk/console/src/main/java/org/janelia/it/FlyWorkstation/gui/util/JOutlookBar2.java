package org.janelia.it.FlyWorkstation.gui.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.*;

/**
 * Updated JOutlookBar which looks more like the latest Outlook. No more accordion.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class JOutlookBar2 extends JOutlookBar implements ActionListener {

	private ButtonGroup group = new ButtonGroup();
	
    /**
     * Causes the outlook bar component to rebuild itself; this means that
     * it rebuilds the top and bottom panels of bars as well as making the
     * currently selected bar's panel visible
     */
    public void render() {

        if (visibleComponent != null) {
            remove(visibleComponent);
        }
        
        String visibleBar = getVisibleBarName();
        BarInfo barInfo = (BarInfo) bars.get(visibleBar);
        visibleComponent = barInfo.getComponent();
        add(visibleComponent, BorderLayout.CENTER);
        
        bottomPanel.removeAll();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.PAGE_AXIS));
//        bottomPanel.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.CENTER);
        
        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new BoxLayout(iconPanel, BoxLayout.LINE_AXIS));
        bottomPanel.add(iconPanel);

        iconPanel.add(Box.createHorizontalGlue());
        
        for (String barName : bars.keySet()) {
            barInfo = (BarInfo) bars.get(barName);
            JToggleButton button = new JToggleButton(barName);
            if (barInfo.getIcon()!=null) {
            	button.setIcon(barInfo.getIcon());
            	button.setVerticalTextPosition(SwingConstants.TOP);
            	button.setHorizontalTextPosition(SwingConstants.CENTER);
            }
            button.addActionListener(this);
            barInfo.setButton(button);
            button.setPreferredSize(new Dimension(90, 70));
            group.add(button);
            iconPanel.add(button);
            if (visibleBar.equals(barName)) {
            	button.setSelected(true);
            }
        }
        
        iconPanel.add(Box.createHorizontalGlue());
        
        // Don't let the bottom panel limit the panel size
        iconPanel.setMinimumSize(new Dimension(20, 20));
        
        revalidate();
        repaint();
    }

}