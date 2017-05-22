/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.stack_viewer.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.workers.SimpleWorker;

/**
 * This is the "top component presence" panel for Stack Viewer.  This one is
 * going to show on the main part of the screen, offering up controls, and
 * showing state, etc.
 *
 * @author Leslie L Foster
 */
public class StackViewerPanel extends JPanel {
	private static final String STACK_VW_LAUNCH_FAIL_MSG = "Failed to launch Stack Viewer";

	private final JButton fileOpenButton = new JButton("Browse and Open Stack");
    private Mip3dStackViewer stackViewer;
	private final JLabel mainLabel = new JLabel("");
	private JLabel busyLabel;

	private final JComponent parent;
	
	public StackViewerPanel(JComponent parent) {
		this.parent = parent;
		guiInit();
	}
	
	public void close() {
        if (stackViewer != null)
            stackViewer.close();
	}
	
	private final void guiInit() {
		setLayout(new BorderLayout());
		add(mainLabel, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());	
		buttonPanel.add(fileOpenButton);
		add(buttonPanel, BorderLayout.SOUTH);
		
        fileOpenButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
				SimpleWorker simpleWorker = new SimpleWorker() {

					@Override
					protected void doStuff() throws Exception {
                        JFileChooser jfc = new JFileChooser();
                        int option = jfc.showOpenDialog(parent);
                        if (option == JFileChooser.APPROVE_OPTION) {
							setBusyState(true);
                            stackViewer = new Mip3dStackViewer();
							stackViewer.prepareWidget(jfc.getSelectedFile().getAbsolutePath());
                        }
					}

					@Override
					protected void hadSuccess() {
						try {
							// If user selected nothing, there will be no viewer.
							if (stackViewer != null) {
								stackViewer.displayWidget();
							}
						} catch (Exception ex) {
							// This error occurs on attempting to display the widget.
							Exception reportEx = new Exception(STACK_VW_LAUNCH_FAIL_MSG, ex);
					        FrameworkImplProvider.handleException(reportEx);		
						}
						setBusyState(false);
					}

					/**
					 * This error occurs on attempting to launch.
					 * @param error 
					 */
					@Override
					protected void hadError(Throwable error) {
						stackViewer.close();
						setBusyState(false);
						Exception reportEx = new Exception(STACK_VW_LAUNCH_FAIL_MSG, error);
						FrameworkImplProvider.handleException(reportEx);
					}
					
				};
				
				simpleWorker.execute();
				
            }
        });
	}
	
	/**
	 * This changes all "busy" aspects of the GUI, depending on the
	 * state chosen.
	 * 
	 * @param busy the target "busy" state. Setting TO this state.
	 */
	private void setBusyState(boolean busy) {
		if (busy) {
			fileOpenButton.setEnabled(false);
			// Anything appearing in the central area would need to be
			// removed at this point.
			
			busyLabel = new JLabel(Icons.getLoadingIcon());
			add(busyLabel, BorderLayout.CENTER);
			revalidate();
			repaint();
		}
		else {
			if (busyLabel != null) {
				remove(busyLabel);
				revalidate();
				repaint();
			}
			fileOpenButton.setEnabled(true);
			busyLabel = null;
		}
	}
}
