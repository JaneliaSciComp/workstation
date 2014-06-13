package org.janelia.it.workstation.gui.application;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFacadeManager;
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.workstation.gui.framework.exception_handlers.ExitHandler;
import org.janelia.it.workstation.gui.framework.exception_handlers.UserNotificationExceptionHandler;
import org.janelia.it.workstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.panels.ApplicationSettingsPanel;
import org.janelia.it.workstation.gui.util.panels.DataSourceSettingsPanel;
import org.janelia.it.workstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.workstation.gui.util.server_status.ServerStatusReportManager;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.workstation.shared.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.security.ProtectionDomain;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:10 PM
 * This is the main class for the workstation client. 
 */
public class ConsoleApp {
	
	private static final Logger log = LoggerFactory.getLogger(ConsoleApp.class);
	
    static {
    	log.info("Java version: " + System.getProperty("java.version"));
        ProtectionDomain pd = ConsoleApp.class.getProtectionDomain();
        log.debug("Code Source: " + pd.getCodeSource().getLocation());
        // Establish some OS-specific stuff
        // Set these, Mac may use - // take the menu bar off the jframe
//        System.setProperty("apple.laf.useScreenMenuBar", "true");
        // set the name of the application menu item
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", ConsoleProperties.getString("console.Title"));
        System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
    }

    public static void main(final String[] args) {
//    	Toolkit.getDefaultToolkit().getSystemEventQueue().push(new TimedEventQueue());
    	SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
		        newBrowser();
			}
		});
    }

    public static void newBrowser() {
        
        final SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash == null) {
            log.warn("No splash screen image configured");
        }
        
        // Prime the tool-specific properties before the Session is invoked
        ConsoleProperties.load();
        
        // Protocol Registration - Adding more than one type should automatically switch over to the Aggregate Facade
        FacadeManager.registerFacade(FacadeManager.getEJBProtocolString(), EJBFacadeManager.class, "JACS EJB Facade Manager");
        
        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        try {
            //Browser Setup
            final String versionString = ConsoleProperties.getString("console.versionNumber");
            final boolean internal = (versionString != null) && (versionString.toLowerCase().contains("internal"));

            sessionMgr.setNewBrowserTitle(ConsoleProperties.getString("console.Title") + " " + ConsoleProperties.getString("console.versionNumber"));
            sessionMgr.setApplicationName(ConsoleProperties.getString("console.Title"));
            sessionMgr.setApplicationVersion(ConsoleProperties.getString("console.versionNumber"));
            sessionMgr.setNewBrowserImageIcon(Utils.getClasspathImage("workstation_128_icon.png"));
            sessionMgr.setNewBrowserSize(.8f);
            sessionMgr.startExternalHttpListener(30000);
            sessionMgr.startAxisServer(ConsoleProperties.getString("console.WebServiceURL"));
            sessionMgr.startWebServer(ConsoleProperties.getInt("console.WebServer.port"));
            sessionMgr.setModelProperty("ShowInternalDataSourceInDialogs", internal);
            sessionMgr.setModelProperty(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY, false);
            sessionMgr.setModelProperty(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY, false);
            //Exception Handler Registration
//            sessionMgr.registerExceptionHandler(new PrintStackTraceHandler());
            sessionMgr.registerExceptionHandler(new UserNotificationExceptionHandler());
            sessionMgr.registerExceptionHandler(new ExitHandler()); //should be last so that other handlers can complete first.
        	
            final ModelMgr modelMgr = ModelMgr.getModelMgr();
            
            
            // Editor Registration
            //      sessionMgr.registerEditorForType(api.entity_model.model.genetics.Species.class,
            //        client.gui.components.assembly.genome_view.GenomeView.class,"Genome View", "ejb");
            //      sessionMgr.registerEditorForType(api.entity_model.model.assembly.GenomicAxis.class,
            //        client.gui.components.annotation.debug_view.DebugView.class,"Annotation Debug View", "ejb");
//            final Class vizardEditor = client.gui.components.annotation.axis_annotation.GenomicAxisAnnotationEditor.class;
//            // OMIT for CONVERSION
//            sessionMgr.registerEditorForType(
//                    api.entity_model.model.assembly.GenomicAxis.class,
//                    vizardEditor, "Genomic Axis Annotation", "xmlgenomicaxis", true);

//            final Class[] editorClasses = new Class[]{vizardEditor};
//            Class editorClass;
//            for (int i = 0; i < editorClasses.length; i++) {
//                editorClass = editorClasses[i];
//                //Sub-Editor Registration
////                sessionMgr.registerSubEditorForMainEditor(editorClass,
////                        client.gui.components.annotation.consensus_sequence_view.ConsensusSequenceView.class);
//            }

            // This is for Preference Controller panels
//            sessionMgr.registerPreferenceInterface(
//                    client.gui.other.panels.BackupPanel.class,
//                    client.gui.other.panels.BackupPanel.class);
            sessionMgr.registerPreferenceInterface(ApplicationSettingsPanel.class, ApplicationSettingsPanel.class);
            sessionMgr.registerPreferenceInterface(DataSourceSettingsPanel.class, DataSourceSettingsPanel.class);
//            sessionMgr.registerPreferenceInterface(SystemSettingsPanel.class, SystemSettingsPanel.class);
            sessionMgr.registerPreferenceInterface(ViewerSettingsPanel.class, ViewerSettingsPanel.class);
//            sessionMgr.registerPreferenceInterface(ToolSettingsPanel.class, ToolSettingsPanel.class);
//            sessionMgr.registerPreferenceInterface(
//                    client.gui.other.panels.ViewSettingsPanel.class,
//                    client.gui.other.panels.ViewSettingsPanel.class);
//            sessionMgr.registerPreferenceInterface(
//                    client.gui.other.panels.TransTransPanel.class,
//                    client.gui.other.panels.TransTransPanel.class);
//
//            sessionMgr.registerPreferenceInterface(
//                    GroupSettingsPanel.class,
//                    GroupSettingsPanel.class);


            ServerStatusReportManager.getReportManager().startCheckingForReport();

            FacadeManager.addProtocolToUseList(FacadeManager.getEJBProtocolString());
//            FacadeManager.addProtocolToUseList("sage");

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
            
            if (!SessionMgr.getSessionMgr().isLoggedIn()) {
                Object[] options = {"Enter Login", "Exit Program"};
                final int answer = JOptionPane.showOptionDialog(null, "Please enter your login information.", "Information Required",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (answer == 0) {
                    PrefController.getPrefController().getPrefInterface(DataSourceSettingsPanel.class, null);
                }
                else {
                    SessionMgr.getSessionMgr().systemExit();
                }
            }
            else {
            	log.info("Successfully logged in user "+SessionMgr.getUsername());
            }
            
            // Init data 
            modelMgr.initErrorOntology();
            modelMgr.addModelMgrObserver(sessionMgr.getAxisServer());
                        
            if (splash!=null) splash.close();
            
            Component mainFrame = SessionMgr.getMainFrame();
            sessionMgr.newBrowser();
            mainFrame.setVisible(true);
            //browser.toFront();
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
            SessionMgr.getSessionMgr().systemExit();
        }
    }
}
