package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.*;

import loci.plugins.config.SpringUtilities;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * A dialog for viewing details about a task.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TaskDetailsDialog extends JDialog {

    private static final String CLICKED_OK = "clicked_ok";
    
    protected static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    
    private JPanel attrPanel;
    
    private JLabel nameLabel;
    private JLabel ownerLabel;
    private JLabel lastStatusLabel;

    private JLabel addAttribute(String name) {
        JLabel nameLabel = new JLabel(name);
        JLabel valueLabel = new JLabel();
        nameLabel.setLabelFor(valueLabel);
        attrPanel.add(nameLabel);
        attrPanel.add(valueLabel);
        return valueLabel;
    }
    
    public TaskDetailsDialog() {

    	setModalityType(ModalityType.APPLICATION_MODAL);
        setTitle("Task Details");
        getContentPane().setLayout(new BorderLayout());

        attrPanel = new JPanel(new SpringLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        		BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Task Properties")));

        nameLabel = addAttribute("Name: ");
        ownerLabel = addAttribute("Task Owner: ");
        lastStatusLabel = addAttribute("Last Status: ");
        
        add(attrPanel, BorderLayout.CENTER);
        SpringUtilities.makeCompactGrid(attrPanel, attrPanel.getComponentCount()/2, 2, 6, 6, 6, 6);

        JButton okButton = new JButton("OK");
        okButton.setActionCommand(CLICKED_OK);
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

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                setVisible(false);
            }
        });
    }
    
    public void showForTask(Task task) {

    	nameLabel.setText(task.getDisplayName());
    	ownerLabel.setText(task.getOwner());
    	lastStatusLabel.setText(task.getLastEvent().getDescription());
        
        pack();

        setLocationRelativeTo(SessionMgr.getSessionMgr().getActiveBrowser());
        SwingUtilities.updateComponentTreeUI(this);
        setVisible(true);
    }
}
