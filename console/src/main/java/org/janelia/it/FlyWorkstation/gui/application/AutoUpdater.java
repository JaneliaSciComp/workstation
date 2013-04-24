package org.janelia.it.FlyWorkstation.gui.application;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URL;

import javax.swing.*;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFacadeManager;
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFactory;
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.FlyWorkstation.gui.framework.exception_handlers.ExitHandler;
import org.janelia.it.FlyWorkstation.gui.framework.exception_handlers.UserNotificationExceptionHandler;
import org.janelia.it.FlyWorkstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.util.panels.ApplicationSettingsPanel;
import org.janelia.it.FlyWorkstation.gui.util.panels.DataSourceSettingsPanel;
import org.janelia.it.FlyWorkstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.FlyWorkstation.shared.filestore.PathTranslator;
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties;
import org.janelia.it.FlyWorkstation.shared.util.SystemInfo;
import org.janelia.it.FlyWorkstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.IOUtils;
import org.janelia.it.jacs.shared.utils.SystemCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check version against the JACS server and update the entire FlySuite if needed.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AutoUpdater extends JFrame implements PropertyChangeListener {

	private static final Logger log = LoggerFactory.getLogger(AutoUpdater.class);
	
	private static final int DEFAULT_BUFFER_SIZE = 1024;
	
	private static final String UPDATE_STATE_PROPERTY = "updateState";
	private static final int STATE_DOWNLOAD = 1;
	private static final int STATE_DECOMPRESS = 2;
	private static final int STATE_APPLY = 2;
	
    // Obligatory Mac garbage in case the user is cursed with that OS
    static {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Workstation AutoUpdate");
        System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
    }

	private static final int padding = 20;
	
	private JPanel mainPane;
	private JLabel mainLabel;
	private JProgressBar progressBar;
	
	private String serverVersion;
	private String clientVersion;
	private String suiteDir;
	
	public AutoUpdater() {
		
	    try {
	        // This try block is copied from ConsoleApp. We may want to consolidate these in the future.
	        
            ConsoleProperties.load();
            
            FacadeManager.registerFacade(FacadeManager.getEJBProtocolString(), EJBFacadeManager.class, "JACS EJB Facade Manager");
            
            final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
            sessionMgr.registerExceptionHandler(new UserNotificationExceptionHandler());
            sessionMgr.registerExceptionHandler(new ExitHandler());        
            sessionMgr.registerPreferenceInterface(ApplicationSettingsPanel.class, ApplicationSettingsPanel.class);
            sessionMgr.registerPreferenceInterface(DataSourceSettingsPanel.class, DataSourceSettingsPanel.class);
            sessionMgr.registerPreferenceInterface(ViewerSettingsPanel.class, ViewerSettingsPanel.class);
    
            // Assuming that the user has entered the login/password information, now validate
            String username = (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME);
            String email = (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL);
            
            if (username==null || email==null) {
                Object[] options = {"Enter Login", "Exit Program"};
                final int answer = JOptionPane.showOptionDialog(null, "Please enter your login and email information.", "Information Required",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (answer == 0) {
                    PrefController.getPrefController().getPrefInterface(DataSourceSettingsPanel.class, null);
                }
                else {
                    SessionMgr.getSessionMgr().systemExit();
                }
            }
            
            SessionMgr.getSessionMgr().loginSubject();
	    }
	    catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
            SessionMgr.getSessionMgr().systemExit();
	    }
        
        setTitle("Workstation AutoUpdate");
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
		repaint();
		
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
        this.serverVersion = computeBean.getAppVersion();
        this.clientVersion = ConsoleProperties.getString("console.versionNumber");

        log.info("Client version: {}",clientVersion);
        log.info("Server version: {}",serverVersion);
    	
        if (!serverVersion.equals(clientVersion)) {

            log.info("Client/server versions do not match");
            
            if (SystemInfo.isMac) {
            	log.info("Configuring for Mac...");
            	suiteDir = "FlySuite_"+serverVersion;
        	}
        	else if (SystemInfo.isLinux) {
        		log.info("Configuring for Linux...");
        		suiteDir = "FlySuite_linux_"+serverVersion;
        	}
            else if (SystemInfo.isWindows) {
            	log.info("Configuring for Windows...");
                suiteDir = "FlySuite_windows_"+serverVersion;
            }
            else {
        		throw new IllegalStateException("Operation system not supported: "+SystemInfo.OS_NAME);
        	}
            
            String releaseNotes = null;
            try {
                URL releaseNotesURL = SessionMgr.getURL(PathTranslator.JACS_DATA_PATH_NFS + "/FlySuite/"+suiteDir+"/releaseNotes.txt");
                releaseNotes = IOUtils.readInputStream(releaseNotesURL.openStream());
            }
            catch (Exception e) {
                log.warn("Could not get release notes: "+e.getMessage());
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
            
            if (releaseNotes!=null) {
            	
            	log.info("Found release notes");
            	
            	JLabel releaseNotesKeyLabel = new JLabel("Release Notes: ");
            	c.gridx = 0;
            	c.gridy = 2;
            	attrPanel.add(releaseNotesKeyLabel, c);
            	
            	JTextArea releaseNotesArea = new JTextArea(releaseNotes);
            	releaseNotesArea.setEditable(false);
            	releaseNotesArea.setBackground((Color)UIManager.get("Panel.background"));
            	c.gridx = 1;
            	c.gridy = 2;
            	attrPanel.add(releaseNotesArea, c);
            }
            
            mainPane.add(attrPanel, BorderLayout.CENTER);
            
            final JButton okButton = new JButton("Update");
            okButton.setToolTipText("Update and launch the FlyWorkstation");
            okButton.addActionListener(new ActionListener() {
    			@Override
    			public void actionPerformed(ActionEvent e) {
    				okButton.setVisible(false);
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
        	log.info("Already at latest version");
        	
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
    				printPathAndExit("",0);
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
    	
    	JPanel progressPanel = new JPanel();
    	progressPanel.add(progressBar);
    	
    	mainPane.add(progressPanel, BorderLayout.CENTER);
    	mainPane.revalidate();
    	mainPane.repaint();
		
    	pack();
        setLocationRelativeTo(null);
    	
		SimpleWorker updater = new SimpleWorker() {

		    private File remoteFile;
		    private File downloadsDir;
		    private File downloadFile;
		    private File extractedDir;
		    private File packageDir;
		    
			@Override
			protected void doStuff() throws Exception {
			    
			    firePropertyChange(UPDATE_STATE_PROPERTY, 0, STATE_DOWNLOAD);
			    
	            downloadsDir = SystemInfo.getDownloadsDir();
	            
	            if (SystemInfo.isMac) {
	                log.info("Configuring for Mac...");
	                suiteDir = "FlySuite_"+serverVersion;
                    remoteFile = new File(PathTranslator.JACS_DATA_PATH_NFS,"FlySuite/"+suiteDir+".tgz");
	                downloadFile = new File(downloadsDir, remoteFile.getName());
	                extractedDir = new File(downloadsDir, suiteDir);
	                packageDir = new File(extractedDir, "FlySuite.app");
	            }
	            else if (SystemInfo.isLinux) {
	                log.info("Configuring for Linux...");
	                suiteDir = "FlySuite_linux_"+serverVersion;
	                remoteFile = new File(PathTranslator.JACS_DATA_PATH_NFS,"FlySuite/"+suiteDir+".tgz");
	                downloadFile = new File(downloadsDir, remoteFile.getName());
	                extractedDir = new File(downloadsDir, suiteDir);
	                packageDir = extractedDir;
	            }
	            else if (SystemInfo.isWindows) {
	                log.info("Configuring for Windows...");
	                suiteDir = "FlySuite_windows_"+serverVersion;
	                remoteFile = new File(PathTranslator.JACS_DATA_PATH_NFS,"FlySuite/"+suiteDir+".zip");
	                downloadFile = new File(downloadsDir, remoteFile.getName());
	                extractedDir = new File(downloadsDir, suiteDir);
	                packageDir = extractedDir;
	            }
	            else {
	                throw new IllegalStateException("Operation system not supported: "+SystemInfo.OS_NAME);
	            }

	            log.info("  remoteFile: {}",remoteFile);
	            log.info("  downloadsDir: {}",downloadsDir);
	            log.info("  downloadFile: {}",downloadFile);
	            log.info("  extractedDir: {}",extractedDir);
	            log.info("  packageDir: {}",packageDir);
	            
				if (downloadFile.exists()) {
					FileUtil.deleteDirectory(downloadFile);
				}
				
				if (extractedDir.exists()) {
					FileUtil.deleteDirectory(extractedDir);
				}

				try {
    				URL remoteFileURL = SessionMgr.getURL(remoteFile.getAbsolutePath());
    				log.info("Downloading update from {}",remoteFileURL);
    				copyURLToFile(remoteFileURL, downloadFile);
				}
				catch (Exception e) {
				    throw new Exception("Error downloading new version",e);
				}
				
				firePropertyChange(UPDATE_STATE_PROPERTY, STATE_DOWNLOAD, STATE_DECOMPRESS);
				        
				if (SystemInfo.isWindows) {
                    log.info("Unzipping {}",downloadFile);
                    FileUtil.zipUncompress(downloadFile, downloadsDir.getAbsolutePath());
                }
				else {
                    log.info("Decompressing {}",downloadFile);
                    if (runShellCommand("tar xvfz "+downloadFile, downloadsDir) != 0) {
                        throw new Exception("Error extracting archive: "+downloadFile.getAbsolutePath());
                    }
                } 

				firePropertyChange(UPDATE_STATE_PROPERTY, STATE_DECOMPRESS, STATE_APPLY);
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
                    
                    log.info("Update is ready");
                    
					mainLabel.setText("Update complete. Launching the FlyWorkstation...");
					mainPane.revalidate();
					mainPane.repaint();
					Thread.sleep(1000);	
					printPathAndExit(packageDir.getAbsolutePath(),75); // TEMP_FAILURE
				}
				catch (InterruptedException e) {
					// Ignore
				}
				catch (Exception e) {
		            SessionMgr.getSessionMgr().handleException(e);
		            printPathAndExit("",1);
				}
			}
			
			@Override
			protected void hadError(Throwable error) {
	            SessionMgr.getSessionMgr().handleException(error);
	            printPathAndExit("",1);
			}

		    /**
		     * Copied from Apache's commons-io, so that we could add progress indication
		     */
			private void copyURLToFile(URL source, File destination) throws IOException {
		        //does destination directory exist ?
		        if (destination.getParentFile() != null
		            && !destination.getParentFile().exists()) {
		            destination.getParentFile().mkdirs();
		        }

		        //make sure we can write to destination
		        if (destination.exists() && !destination.canWrite()) {
		            String message =
		                "Unable to open file " + destination + " for writing.";
		            throw new IOException(message);
		        }

		        InputStream input = null;
		        long length = 10000000;
		        if (!source.getProtocol().equals("file")) {
	                HttpClient client = SessionMgr.getSessionMgr().getWebDavClient().getHttpClient();

	                GetMethod get = new GetMethod(source.toString());
	                client.executeMethod(get);

	                
	                Header contentLength = get.getResponseHeader("Content-Length");
	                if (contentLength!=null) {
	                    length = Long.parseLong(contentLength.getValue());
	                }
	                input = get.getResponseBodyAsStream();
		        }
		        else {
		            File sourceFile = new File(source.getPath());
		            length = sourceFile.length();
		            input = source.openStream();
		        }
                
		        try {
		            FileOutputStream output = new FileOutputStream(destination);
		            try {
		                copy(input, output, length);
		            } finally {
		                org.apache.commons.io.IOUtils.closeQuietly(output);
		            }
		        } finally {
		            org.apache.commons.io.IOUtils.closeQuietly(input);
		        }
		    }

            /**
             * Copied from Apache's commons-io, so that we could add progress indication
             */
		    private int copy(InputStream input, OutputStream output, long length) throws IOException {
		        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		        int count = 0;
		        int n = 0;
		        while (-1 != (n = input.read(buffer))) {
		            output.write(buffer, 0, n);
		            count += n;
		            setProgress(count, (int)length);
		        }
		        return count;
		    }
		};

		updater.addPropertyChangeListener(this);
		updater.execute();
	}
        
	private int state = 0;
	
	/**
     * Invoked when the workers progress property changes.
     */
    public void propertyChange(PropertyChangeEvent e) {
        if ("progress".equals(e.getPropertyName())) {;
            if (state==STATE_DOWNLOAD) {
                int progress = (Integer) e.getNewValue();
                progressBar.setValue(progress);
                mainLabel.setText(String.format("%d%% complete", progress));
            }
        }
        else if (UPDATE_STATE_PROPERTY.equals(e.getPropertyName())) {
            this.state = (Integer)e.getNewValue();
            if (state==STATE_DOWNLOAD) {
                mainLabel.setText("Downloading...");
            }
            else if (state==STATE_DECOMPRESS) {
                mainLabel.setText("Decompressing...");
            }
            else if (state==STATE_APPLY) {
                mainLabel.setText("Applying update...");
            }
        }
        mainPane.revalidate();
        mainPane.repaint();
    }

	private int runShellCommand(String command, File dir) throws Exception {
		
		String[] args = command.split("\\s+");

        StringBuffer stdout = new StringBuffer();
        StringBuffer stderr = new StringBuffer();
        SystemCall call = new SystemCall(stdout, stderr);

        try {
		    int result = call.emulateCommandLine(args, null, dir, 3600);
            if ( result != 0 ) {
                log.error("Command " + command + " failed.  Error=" + result);
            }
            return result;
        } catch ( Exception ex ) {
            log.error( "Failed to execute command '" + command + "', exception " + ex.getMessage() + " thrown." );
            throw ex;
        }
	}


	private static void printPathAndExit(String path, int exitCode) {
		System.out.println(path);
		System.exit(exitCode);
	}
	
    public static void main(final String[] args) {
        try {
        	AutoUpdater updater = new AutoUpdater();
            updater.checkVersions();
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
			printPathAndExit("",1);
        }
    }
}
