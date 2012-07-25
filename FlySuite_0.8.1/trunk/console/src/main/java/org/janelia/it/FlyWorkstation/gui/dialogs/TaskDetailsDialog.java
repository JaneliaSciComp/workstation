package org.janelia.it.FlyWorkstation.gui.dialogs;

import loci.plugins.config.SpringUtilities;
import org.janelia.it.jacs.model.tasks.Task;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * A dialog for viewing details about a task.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TaskDetailsDialog extends ModalDialog {
    
    protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    
    private JPanel attrPanel;
    
    private JLabel addAttribute(String name) {
        JLabel nameLabel = new JLabel(name);
        JLabel valueLabel = new JLabel();
        nameLabel.setLabelFor(valueLabel);
        attrPanel.add(nameLabel);
        attrPanel.add(valueLabel);
        return valueLabel;
    }
    
    public TaskDetailsDialog() {

        setTitle("Task Details");

        attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        		BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Task Properties")));

        add(attrPanel, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
	            setVisible(false);
			}
		});

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        
        add(buttonPane, BorderLayout.SOUTH);
    }
    
    public void showForTask(Task task) {

    	attrPanel.removeAll();
        addAttribute("Name: ").setText(task.getDisplayName());
        addAttribute("Task Owner: ").setText(task.getOwner());
        addAttribute("Task Id: ").setText(task.getObjectId().toString());
        addAttribute("Last Status: ").setText(task.getLastEvent().getDescription());
        
        for(String key : task.getParameterKeySet()) {
        	String value = task.getParameter(key);
        	addAttribute(key).setText(value);
        }

        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);
        packAndShow();
    }
}
