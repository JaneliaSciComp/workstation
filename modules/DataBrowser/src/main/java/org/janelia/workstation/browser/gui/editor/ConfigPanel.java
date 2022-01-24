package org.janelia.workstation.browser.gui.editor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.common.gui.support.Icons;
import org.janelia.workstation.common.gui.support.MouseHandler;
import org.janelia.workstation.common.gui.support.WrapLayout;

/**
 * Top panel for viewers/editors which provides some common features:
 * 1) Large title display
 * 2) Collapsible configuration panel 
 * 3) Wrap layout for all components 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConfigPanel extends JPanel {
	
	private static final Font TITLE_FONT = new Font("Sans Serif", Font.BOLD, 15);
	private static final ImageIcon EXPAND_ICON = Icons.getIcon("chevron-expand-icon.png");
	private static final ImageIcon COLLAPSE_ICON = Icons.getIcon("chevron-collapse-icon.png");
	
	private final JButton showConfigPanelButton;
    private final JPanel titlePanel;
    private final JLabel titleLabel;
    private final JPanel configPanel;
    
    private List<JComponent> collapsedComponents = new ArrayList<>();
    private List<JComponent> expandedComponents = new ArrayList<>();

    private boolean includeTitle;
    private boolean configExpanded;

    public ConfigPanel(boolean expandedByDefault) {
        this(true, expandedByDefault, 2, 3);
    }
    
	public ConfigPanel(boolean includeTitle, boolean expandedByDefault, int hgap, int vgap) {
		
	    this.includeTitle = includeTitle;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		this.configExpanded = !expandedByDefault; // We'll toggle it later
		
        showConfigPanelButton = new JButton();
        showConfigPanelButton.setBorderPainted(false);
        showConfigPanelButton.setPreferredSize(new Dimension(16, 16));
        showConfigPanelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
				ActivityLogHelper.logUserAction("ConfigPanel.setExpanded", !configExpanded);
                toggleCriteriaPanelState();
            }
        });
        
        this.titleLabel = new JLabel("");
        titleLabel.setFont(TITLE_FONT);

		titleLabel.addMouseListener(new MouseHandler() {
			@Override
			protected void popupTriggered(MouseEvent e) {
				if (e.isConsumed()) {
					return;
				}
				LabelContextMenu popupMenu = new LabelContextMenu("Name", titleLabel);
				popupMenu.addMenuItems();
				popupMenu.show(e);
				e.consume();
			}

            @Override
            public void mouseClicked(MouseEvent e) {
                titleClicked(e);
            }
			
		});
                
        this.titlePanel = new JPanel(new WrapLayout(false, WrapLayout.LEFT, 2, 2));
        addDefaultTitleComponents();
        
        this.configPanel = new JPanel(new WrapLayout(false, WrapLayout.LEFT, hgap, vgap));
        configPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        configPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        
        toggleCriteriaPanelState();
	}
	
	protected void titleClicked(MouseEvent e) {
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
        
        if (includeTitle) {
            titlePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(titlePanel, "");

            if (configExpanded) {
                showConfigPanelButton.setIcon(COLLAPSE_ICON);

                JSeparator sep1 = new JSeparator(JSeparator.HORIZONTAL);
                sep1.setAlignmentX(Component.LEFT_ALIGNMENT);
                add(sep1);
                
                add(configPanel);
            }
            else {
                showConfigPanelButton.setIcon(EXPAND_ICON);
            }
            
            JSeparator sep2 = new JSeparator(JSeparator.HORIZONTAL);
            sep2.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(sep2);
        }
        else {
            // if the title is not included, we don't need any of the separators either
            add(configPanel);
        }
        
        updateUI();
    }
}
