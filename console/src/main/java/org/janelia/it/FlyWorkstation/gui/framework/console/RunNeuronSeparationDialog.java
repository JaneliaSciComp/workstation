package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.gui.framework.outline.AnnotationSessionPropertyDialog;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

/**
 * A dialog for starting a continuous neuron separation pipeline. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RunNeuronSeparationDialog extends JDialog {
    
	private static final int DEFAULT_REFRESH_INTERVAL = 60;
	
    private final JPanel attrPanel;    
    private final JTextField inputDirectoryField;
    private final JTextField rerunIntervalField;

    public RunNeuronSeparationDialog() {

        setTitle("Annotation Details");
        setSize(750, 200);
        getContentPane().setLayout(new BorderLayout());

        setLocationRelativeTo(SessionMgr.getSessionMgr().getActiveBrowser());
        
        attrPanel = new JPanel(new GridBagLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10), 
        		BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Task Parameters")));

        GridBagConstraints c = new GridBagConstraints();
        
        JLabel nameLabel = new JLabel("Input Directory (Linux mounted)");
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 0);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        attrPanel.add(nameLabel, c);

        inputDirectoryField = new JTextField(40);
        inputDirectoryField.setText("/groups/flylight/flylight/SingleNeuronPilotData");
        c.gridx = 1;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 10, 0);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        attrPanel.add(inputDirectoryField, c);
        
//        JButton findDirButton = new JButton("Choose...");
//        findDirButton.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				JFileChooser chooser = new JFileChooser(); 
//				chooser.setDialogTitle("Choose the input directory");
//			    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//			    chooser.setAcceptAllFileFilterUsed(false);
//			    if (chooser.showOpenDialog(RunNeuronSeparationDialog.this) == JFileChooser.APPROVE_OPTION) { 
//			    	inputDirectoryField.setText(chooser.getCurrentDirectory().getAbsolutePath());
//			    }
//			}
//		});
//        
//        c.gridx = 2;
//        c.gridy = 0;
//        c.insets = new Insets(0, 0, 10, 0);
//        c.anchor = GridBagConstraints.LINE_START;
//        c.fill = GridBagConstraints.HORIZONTAL;
//        attrPanel.add(findDirButton, c);
        

        JLabel nameLabel2 = new JLabel("Refresh Interval (minutes)");
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 10, 0);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        attrPanel.add(nameLabel2, c);

        rerunIntervalField = new JTextField(10);
        rerunIntervalField.setText(DEFAULT_REFRESH_INTERVAL+"");
        c.gridx = 1;
        c.gridy = 1;
        c.insets = new Insets(0, 0, 10, 0);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        attrPanel.add(rerunIntervalField, c);
        
        
        add(attrPanel, BorderLayout.CENTER);

        JButton okButton = new JButton("Run");
        okButton.setToolTipText("Run the neuron separation");
        okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				runNeuronSeparation();
			}
		});
        

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Cancel and close this dialog");
        cancelButton.addActionListener(new ActionListener() {
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
        buttonPane.add(cancelButton);
        
        add(buttonPane, BorderLayout.SOUTH);

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                setVisible(false);
            }
        });
    }
    
    public void runNeuronSeparation() {
    	
    	Utils.setWaitingCursor(this);
    	
    	final String inputDirPath = inputDirectoryField.getText();
    	int rerunMins = 0;
    	try {
    		rerunMins = Integer.parseInt(rerunIntervalField.getText());
    	}
    	catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
            		"Can't parse refresh interval", "Error", JOptionPane.ERROR_MESSAGE);
    	}
    	
    	final int finalRerunMins = rerunMins;
    	
    	SimpleWorker executeWorker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
				
				// TODO: Run task here

				System.out.println("inputDirPath: "+inputDirPath);
				System.out.println("finalRerunMins: "+finalRerunMins);
				
			}
			
			@Override
			protected void hadSuccess() {
		    	Utils.setDefaultCursor(RunNeuronSeparationDialog.this);
	            Browser browser = SessionMgr.getSessionMgr().getActiveBrowser();
	            browser.getTaskOutline().loadTasks();
	            browser.getOutlookBar().setVisibleBarByName(Browser.BAR_TASKS);
				setVisible(false);
			}
			
			@Override
			protected void hadError(Throwable error) {
				error.printStackTrace();
		    	Utils.setDefaultCursor(RunNeuronSeparationDialog.this);
	            JOptionPane.showMessageDialog(RunNeuronSeparationDialog.this, 
	            		"Error submitting job: "+error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		};
    	
		executeWorker.execute();
    }
    
    public void showDialog() {
        SwingUtilities.updateComponentTreeUI(this);
        setVisible(true);
    }
}
