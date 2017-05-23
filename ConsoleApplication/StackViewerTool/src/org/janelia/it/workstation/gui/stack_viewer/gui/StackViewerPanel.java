/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.stack_viewer.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
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
	
	private static final Logger logger = Logger.getLogger(StackViewerPanel.class.getSimpleName());

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
						String selectedFile = chooseFile();
						if (selectedFile != null) {
							setBusyState(true);
                            stackViewer = new Mip3dStackViewer();
							stackViewer.prepareWidget(selectedFile);
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
	
	/**
	 * Let user select a file.  If they do, return its absolute path;
	 * if they do not, return null.
	 * 
	 * @return file path or null value.
	 */
	private String chooseFile() {
		String rtnVal = null;
		JFileChooser jfc = new JFileChooser();
		jfc.setAcceptAllFileFilterUsed(false);
		jfc.addChoosableFileFilter(new ViableLoadFileFilter());
		int option = jfc.showOpenDialog(parent);
		if (option == JFileChooser.APPROVE_OPTION) {
			rtnVal = jfc.getSelectedFile().getAbsolutePath();
		}
		return rtnVal;
	}
	
	/**
	 * This filter will apply constraints based upon success or failure of load
	 * tests as below.
	 * 
	 * file load tests.
     * old cons label v3draw file from Yuy: black screen
     * old lsm: failed
     * old mp4 file: viewable
     * newly downloaded mp4 file: viewable
     * newly downloaded h5j file: viewable
     * newly downloaded (Wolfft) v3dpbd consolidated label: viewable
     * newly downloaded (Wolfft) v3dpbd consolidated signal: very slow, fails with stack trace (see other comment).
     * newly downloaded (Wolfft) v3dpbd Reference: viewable in grayscale
     * newly downloaded (Wolfft) v3dpbd cons signal converted to TIF. Fails, with stack trace (see other comment).
	 * 
	 */
	private static class ViableLoadFileFilter extends FileFilter {

		// This is a capture pattern for the extension.
		private static final Pattern FILENAME_PATTERN = Pattern.compile(".+\\.([^.]+)$");
				//Pattern.compile(".+([^.]+)");
		
		// This is a set of acceptable extensions--regardless of file name.
		private static final Set<String> ACCEPTED_EXTENSIONS = new HashSet<>();		
		static {
			ACCEPTED_EXTENSIONS.add("mp4");
			ACCEPTED_EXTENSIONS.add("h5j");
		}
		
		// These are endings of file names shown to be successfully loaded.
		private static final Set<String> ACCEPTED_NAME_ENDINGS = new HashSet<>();
		static {
			ACCEPTED_NAME_ENDINGS.add("ConsolidatedLabel.v3dpbd");
			ACCEPTED_NAME_ENDINGS.add("Reference.v3dpbd");
		}
		
		/**
		 * We use the acceptable/working file name patterns as a guide for
		 * what may be loaded. These lists can/will be extended as bugs
		 * are found and eliminated from the viewer.
		 * 
		 * @param pathname candidate path.
		 * @return T=this can be opened; F=this cannot be opened.
		 */
		@Override
		public boolean accept(File pathname) {
					
			boolean rtnVal = false;
			final String filename = pathname.getName();
			String[] filenameParts = filename.split("\\.");
			if (filenameParts.length > 1) {
				logger.log(Level.INFO, "Got extension for {0}", filename);
				String extension = filenameParts[filenameParts.length - 1];
				if (extension != null  &&  extension.length() > 0) {
					if (ACCEPTED_EXTENSIONS.contains(extension)) {
						rtnVal = true;
					} else {
						for (String filenameEnding : ACCEPTED_NAME_ENDINGS) {
							if (filename.endsWith(filenameEnding)) {
								rtnVal = true;
								break;
							}
						}
					}
				}
			}
			if (pathname.isDirectory()) {
				rtnVal = true;
			}
			return rtnVal;
		}

		@Override
		public String getDescription() {
			return "All types currently supported";
		}
		
	}
}
