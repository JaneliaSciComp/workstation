package org.janelia.it.FlyWorkstation.gui.application;

import org.apache.commons.io.FileUtils;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.framework.exception_handlers.ExitHandler;
import org.janelia.it.FlyWorkstation.gui.framework.exception_handlers.UserNotificationExceptionHandler;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.gui.util.FakeProgressWorker;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.gui.util.SystemInfo;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.MissingResourceException;

/**
 * Check version against the JACS server and update the entire FlySuite if needed.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AutoUpdater extends JFrame implements PropertyChangeListener {

	private static final int padding = 20;
	
	private JPanel mainPane;
	private JLabel mainLabel;
	private JProgressBar progressBar;
	private File remoteFile;
    private File downloadsDir;
	private File downloadFile;
	private File extractedDir;
	private File packageDir=null;
	
	public AutoUpdater() {
        getContentPane().setLayout(new BorderLayout());
        setSize(400, 200);
        mainPane = new JPanel(new BorderLayout());
        mainPane.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        add(mainPane, BorderLayout.CENTER);
	}
	
	public void checkVersions() throws Exception {

		mainLabel = new JLabel("Checking for updates...");
		mainLabel.setHorizontalAlignment(SwingConstants.CENTER);
		mainLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		mainPane.add(mainLabel, BorderLayout.CENTER);
		
		mainPane.revalidate();
		mainPane.repaint();
		
		Thread.sleep(300);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					showVersionCheck();
				}
				catch (Exception e) {
					SessionMgr.getSessionMgr().handleException(e);
					System.exit(1);
				}
			}
		});
	}
	
	public void showVersionCheck() throws Exception {
		
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        final String serverVersion = computeBean.getAppVersion();
        final String clientVersion = ConsoleProperties.getString("console.versionNumber");

    	if (!FacadeManager.isDataSourceConnectivityValid()) {
    		String message = FacadeManager.getDataSourceHelpInformation();
    		throw new MissingResourceException(message, ConsoleApp.class.getName(), "Missing Data Mount");
    	}
    	
        if (!serverVersion.equals(clientVersion)) {

            File releaseNotesFile;
            if (SystemInfo.isMac) {
            	String suiteDir = "FlySuite_"+serverVersion;
            	remoteFile = new File(FacadeManager.getOsSpecificRootPath(), "FlySuite/"+suiteDir+".tgz");
            	releaseNotesFile = new File(FacadeManager.getOsSpecificRootPath(), "FlySuite/"+suiteDir+"/releaseNotes.txt");
        		downloadsDir = new File(System.getProperty("user.home"),"Downloads/");
            	downloadFile = new File(downloadsDir, remoteFile.getName());
            	extractedDir = new File(downloadsDir, suiteDir);
            	packageDir = new File(extractedDir, "FlySuite.app");
        	}
        	else if (SystemInfo.isLinux) {
            	String suiteDir = "FlySuite_linux_"+serverVersion;
            	remoteFile = new File(FacadeManager.getOsSpecificRootPath(), "FlySuite/"+suiteDir+".tgz");
            	releaseNotesFile = new File(FacadeManager.getOsSpecificRootPath(), "FlySuite/"+suiteDir+"/releaseNotes.txt");
        		downloadsDir = new File("/tmp/");
            	downloadFile = new File(downloadsDir, remoteFile.getName());
            	extractedDir = new File(downloadsDir, suiteDir);
            	packageDir = extractedDir;
        	}
            else if (SystemInfo.isWindows) {
                String suiteDir = "FlySuite_windows_"+serverVersion;
                remoteFile = new File(FacadeManager.getOsSpecificRootPath(), "FlySuite/"+suiteDir+".zip");
                releaseNotesFile = new File(FacadeManager.getOsSpecificRootPath(), "FlySuite/"+suiteDir+"/releaseNotes.txt");
                downloadsDir = new File(SessionMgr.getSessionMgr().getApplicationOutputDirectory()+"/tmp/");
                downloadFile = new File(downloadsDir, remoteFile.getName());
                extractedDir = new File(downloadsDir, suiteDir);
                packageDir = extractedDir;
            }
            else {
        		throw new IllegalStateException("Operation system not supported: "+SystemInfo.OS_NAME);
        	}
        	
        	
        	if (!remoteFile.exists() || !remoteFile.canRead()) {
        		throw new Exception("Cannot access "+remoteFile.getAbsolutePath());
        	}

        	mainPane.removeAll();

        	mainPane.add(new JLabel("A new version of FlyWorkstation is available."), BorderLayout.NORTH);
        	
        	JPanel attrPanel = new JPanel(new GridBagLayout());
        	GridBagConstraints c = new GridBagConstraints();
        	c.anchor = GridBagConstraints.FIRST_LINE_START;
        	c.insets = new Insets(10,0,0,0);
        	
            JLabel currVersionKeyLabel = new JLabel("Current Version: ");
        	c.gridx = 0;
        	c.gridy = 0;
            attrPanel.add(currVersionKeyLabel, c);
            
            JLabel currVersionValueLabel = new JLabel(clientVersion);
        	c.gridx = 1;
        	c.gridy = 0;
            attrPanel.add(currVersionValueLabel, c);
            
            JLabel latestVersionKeyLabel = new JLabel("Latest Version: ");
            c.gridx = 0;
        	c.gridy = 1;
            attrPanel.add(latestVersionKeyLabel, c);
            
            JLabel latestVersionValueLabel = new JLabel(serverVersion);
            c.gridx = 1;
        	c.gridy = 1;
            attrPanel.add(latestVersionValueLabel, c);
            
            if (releaseNotesFile.exists() && releaseNotesFile.canRead()) {
            	JLabel releaseNotesKeyLabel = new JLabel("Release Notes: ");
            	c.gridx = 0;
            	c.gridy = 2;
            	attrPanel.add(releaseNotesKeyLabel, c);
            	
            	JTextArea releaseNotesArea = new JTextArea(FileUtils.readFileToString(releaseNotesFile));
            	releaseNotesArea.setEditable(false);
            	releaseNotesArea.setBackground((Color)UIManager.get("Panel.background"));
            	c.gridx = 1;
            	c.gridy = 2;
            	attrPanel.add(releaseNotesArea, c);
            }
            
            mainPane.add(attrPanel, BorderLayout.CENTER);
            
            JButton okButton = new JButton("Update");
            okButton.setToolTipText("Update and launch the FlyWorkstation");
            okButton.addActionListener(new ActionListener() {
    			@Override
    			public void actionPerformed(ActionEvent e) {
    				update();
    			}
    		});
            
            JButton cancelButton = new JButton("Cancel");
            cancelButton.setToolTipText("Cancel update and launch the FlyWorkstation");
            cancelButton.addActionListener(new ActionListener() {
    			@Override
    			public void actionPerformed(ActionEvent e) {
    	            System.exit(0);
    			}
    		});

            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
            buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            buttonPane.add(Box.createHorizontalGlue());
            buttonPane.add(okButton);
            buttonPane.add(cancelButton);
            
            add(buttonPane, BorderLayout.SOUTH);
            
    		mainPane.revalidate();
    		mainPane.repaint();
        }
        else {
        	mainLabel.setText("Already at latest version. Launching...");
        	mainPane.revalidate();
    		mainPane.repaint();
    		
    		SwingUtilities.invokeLater(new Runnable() {
    			@Override
    			public void run() {
    				try {
    					Thread.sleep(500);
    				} catch (InterruptedException e) {
    					// Ignore
    				}
    	    		System.exit(0);
    			}
    		});
        }
        
        pack();
        setLocationRelativeTo(null);
		setVisible(true);
	}
    
	public void update() {

    	mainPane.removeAll();

    	mainLabel.setText("Updating...");
    	mainPane.add(mainLabel, BorderLayout.NORTH);
    	
    	progressBar = new JProgressBar(1, 100);
    	mainPane.add(progressBar, BorderLayout.CENTER);
    	mainPane.revalidate();
		mainPane.repaint();
		
		SimpleWorker updater = new FakeProgressWorker() {

			@Override
			protected void doStuff() throws Exception {
							
				if (downloadFile.exists()) {
					FileUtil.deleteDirectory(downloadFile);
				}
				
				if (extractedDir.exists()) {
					FileUtil.deleteDirectory(extractedDir);
				}

                if (SystemInfo.isLinux || SystemInfo.isMac) {
                    if (runShellCommand("cp "+remoteFile+" "+downloadFile, downloadsDir) != 0) {
                        throw new Exception("Error downloading archive: "+downloadFile.getAbsolutePath());
                    }

                    if (runShellCommand("tar xvfz "+downloadFile, downloadsDir) != 0) {
                        throw new Exception("Error extracting archive: "+downloadFile.getAbsolutePath());
                    }
                }
                else if (SystemInfo.isWindows) {
                    if (runShellCommand("copy "+remoteFile+" "+downloadFile, downloadsDir) != 0) {
                        throw new Exception("Error downloading archive: "+downloadFile.getAbsolutePath());
                    }

                    FileUtil.zipUncompress(downloadFile, downloadsDir.getAbsolutePath());
                }

            }
			
			@Override
			protected void hadSuccess() {

				try {					
					if (null==packageDir || !packageDir.exists()) {
						throw new Exception("Error retrieving update. "+packageDir);
					}

                    File[] tmpPkgFiles = packageDir.listFiles();
                    if (null==tmpPkgFiles || tmpPkgFiles.length<1) {
                        throw new Exception("Error retrieving update. "+packageDir);
                    }

					// The last line of output is agree to be the downloaded package
					System.out.println(packageDir.getAbsolutePath());
					
					mainLabel.setText("Update complete. Launching the FlyWorkstation...");
					mainPane.revalidate();
					mainPane.repaint();
					Thread.sleep(1000);	
					System.exit(75); // TEMP_FAILURE
				}
				catch (InterruptedException e) {
					// Ignore
				}
				catch (Exception e) {
		            SessionMgr.getSessionMgr().handleException(e);
					System.exit(1);
				}
			}
			
			@Override
			protected void hadError(Throwable error) {
	            SessionMgr.getSessionMgr().handleException(error);
				System.exit(1);
			}
		};

		updater.addPropertyChangeListener(this);
		updater.execute();
	}
	
	/**
     * Invoked when the workers progress property changes.
     */
    public void propertyChange(PropertyChangeEvent e) {
    	
        if ("progress".equals(e.getPropertyName())) {
            int progress = (Integer) e.getNewValue();
            progressBar.setValue(progress);
            mainLabel.setText(String.format("%d%% complete", progress));
        	mainPane.revalidate();
    		mainPane.repaint();
        }
    }

	private int runShellCommand(String command, File dir) throws Exception {

		//System.out.println("RUN: "+command);
		
		String[] args = command.split("\\s+");

        StringBuffer stdout = new StringBuffer();
        StringBuffer stderr = new StringBuffer();
        SystemCall call = new SystemCall(stdout, stderr);

        //System.out.println("STDOUT: "+stdout);
		//System.out.println("STDERR: "+stderr);
		
		return call.emulateCommandLine(args, null, dir, 3600);
	}
	
    public static void main(final String[] args) {

        try {
            ConsoleProperties.load();
            
            final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
            sessionMgr.registerExceptionHandler(new UserNotificationExceptionHandler());
            sessionMgr.registerExceptionHandler(new ExitHandler());
            
        	AutoUpdater updater = new AutoUpdater();
            updater.checkVersions();
            
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
			System.exit(1);
        }
    }
}
