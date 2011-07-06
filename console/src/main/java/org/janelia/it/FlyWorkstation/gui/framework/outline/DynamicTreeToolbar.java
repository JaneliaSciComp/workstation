package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Position.Bias;

import org.janelia.it.FlyWorkstation.gui.util.Icons;

/**
 * A toolbar which sits on top of a DynamicTree and provides generic tree-related functions such as 
 * expanding/collapsing all nodes in the tree, and searching in the tree.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DynamicTreeToolbar extends JPanel implements ActionListener {
	
	private static final String EXPAND_ALL = "expand_all";
	private static final String COLLAPSE_ALL = "collapse_all";
	private static final String NEXT_MATCH = "next_match";
	private static final String PREVIOUS_MATCH = "previous_match";
    
    private final DynamicTree tree;
	private JTextField textField;
    
	public DynamicTreeToolbar(final DynamicTree tree) {
        super(new BorderLayout());

        this.tree = tree;
        
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        
        JButton button = new JButton(Icons.expandAllIcon);
        button.setActionCommand(EXPAND_ALL);
        button.setToolTipText("Expand all the nodes in the tree.");
        button.addActionListener(this);
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        toolBar.add(button);

        button = new JButton(Icons.collapseAllIcon);
        button.setActionCommand(COLLAPSE_ALL);
        button.setToolTipText("Collapse all the nodes in the tree.");
        button.addActionListener(this);
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        toolBar.add(button);
        
        toolBar.addSeparator();

        JLabel label = new JLabel("Find:");
        toolBar.add(label);
        
        textField = new JTextField();
        textField.setColumns(10);
        textField.addActionListener(this);
        JPanel innerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        innerPanel.add(textField);
        toolBar.add(innerPanel);
        
		textField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
		        tree.navigateToNodeStartingWith(textField.getText(), null);
			}

			public void removeUpdate(DocumentEvent e) {
				changedUpdate(e);
			}

			public void insertUpdate(DocumentEvent e) {
				changedUpdate(e);
			}
		});
        	  
        button = new JButton("Next");
        button.setActionCommand(NEXT_MATCH);
        button.setToolTipText("Find the next occurence of the phrase.");
        button.addActionListener(this);
        innerPanel.add(button);

        
        button = new JButton("Previous");
        button.setActionCommand(PREVIOUS_MATCH);
        button.setToolTipText("Find the previous occurence of the phrase.");
        button.addActionListener(this);
        innerPanel.add(button);
        
        add(toolBar, BorderLayout.PAGE_START);
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (EXPAND_ALL.equals(cmd)) { 
        	tree.expandAll(true);
        } 
        else if (COLLAPSE_ALL.equals(cmd)) { 
        	tree.expandAll(false);
        } 
        else if (NEXT_MATCH.equals(cmd)) { 
            tree.navigateToNodeStartingWith(textField.getText(), Bias.Forward);

        } 
        else if (PREVIOUS_MATCH.equals(cmd)) { 
            tree.navigateToNodeStartingWith(textField.getText(), Bias.Backward);
        } 
    }

}
