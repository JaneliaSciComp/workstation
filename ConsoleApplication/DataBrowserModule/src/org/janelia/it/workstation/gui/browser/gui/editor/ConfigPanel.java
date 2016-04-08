package org.janelia.it.workstation.gui.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.janelia.it.workstation.gui.util.Icons;
import org.janelia.it.workstation.gui.util.WrapLayout;

/**
 * Top panel for viewers/editors which provides some common features:
 * 1) Large title display
 * 2) Collapsible configuration panel 
 * 3) Wrap layout for all components 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class ConfigPanel extends JPanel {
	
	private static final Font TITLE_FONT = new Font("Sans Serif", Font.BOLD, 15);
	private static final ImageIcon EXPAND_ICON = Icons.getIcon("chevron-expand-icon.png");
	private static final ImageIcon COLLAPSE_ICON = Icons.getIcon("chevron-collapse-icon.png");
	
	private final JButton showConfigPanelButton;
    private final JPanel titlePanel;
    private final JLabel titleLabel;
    private final JPanel configPanel;
    
    private List<JComponent> collapsedComponents = new ArrayList<>();
    private List<JComponent> expandedComponents = new ArrayList<>();
    
    private boolean configExpanded;
    
	public ConfigPanel(boolean expandedByDefault) {
		
		setLayout(new BorderLayout());

		this.configExpanded = !expandedByDefault; // We'll toggle it later
		
        showConfigPanelButton = new JButton();
        showConfigPanelButton.setBorderPainted(false);
        showConfigPanelButton.setPreferredSize(new Dimension(16, 16));
        showConfigPanelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleCriteriaPanelState();
            }
        });
        
        this.titleLabel = new JLabel("");
        titleLabel.setFont(TITLE_FONT);
                
        this.titlePanel = new JPanel(new WrapLayout(false, WrapLayout.LEFT, 2, 3));
        addDefaultTitleComponents();
        
        this.configPanel = new JPanel(new WrapLayout(false, WrapLayout.LEFT, 2, 3));
        configPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        
        toggleCriteriaPanelState();
	}
	
	private void addDefaultTitleComponents() {
		titlePanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        titlePanel.add(showConfigPanelButton);
        titlePanel.add(titleLabel);
	}

	public void addTitleComponent(JComponent comp, boolean collapsed, boolean expanded) {
		if (collapsed) collapsedComponents.add(comp);
		if (expanded) expandedComponents.add(comp);
		refillTitleComponents();
	}
	
	private void refillTitleComponents() {
		titlePanel.removeAll();
		addDefaultTitleComponents();
		if (configExpanded) {
			for(JComponent comp : expandedComponents) {
				titlePanel.add(comp);
			}
		}
		else {
			for(JComponent comp : collapsedComponents) {
				titlePanel.add(comp);
			}
		}
	}
	
	public void removeAllTitleComponents() {
		expandedComponents.clear();
		collapsedComponents.clear();
		refillTitleComponents();
	}

	public void addConfigComponent(JComponent comp) {
		configPanel.add(comp);
	}
	
	public void removeAllConfigComponents() {
		configPanel.removeAll();
	}
	
	public void setTitle(String title) {
		titleLabel.setText(title);
	}

    private void toggleCriteriaPanelState() {
    	setExpanded(!configExpanded);
    }
    
	public void setExpanded(boolean configExpanded) {
		this.configExpanded = configExpanded;
    	refillTitleComponents();
        removeAll();
        if (configExpanded) {
        	showConfigPanelButton.setIcon(COLLAPSE_ICON);
            add(titlePanel, BorderLayout.NORTH);
            add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.CENTER);
            add(configPanel, BorderLayout.SOUTH);
        }
        else {
        	showConfigPanelButton.setIcon(EXPAND_ICON);
            add(titlePanel, BorderLayout.CENTER);
        }
        updateUI();
    }
}
