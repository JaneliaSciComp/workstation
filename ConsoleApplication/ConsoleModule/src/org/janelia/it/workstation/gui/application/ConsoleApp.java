package org.janelia.it.workstation.gui.application;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFacadeManager;
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager;
import org.janelia.it.workstation.gui.framework.exception_handlers.ExitHandler;
import org.janelia.it.workstation.gui.framework.exception_handlers.UserNotificationExceptionHandler;
import org.janelia.it.workstation.gui.framework.pref_controller.PrefController;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.util.panels.ApplicationSettingsPanel;
import org.janelia.it.workstation.gui.util.panels.UserAccountSettingsPanel;
import org.janelia.it.workstation.gui.util.panels.ViewerSettingsPanel;
import org.janelia.it.workstation.gui.util.server_status.ServerStatusReportManager;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.janelia.it.workstation.shared.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.security.ProtectionDomain;
import org.janelia.it.workstation.shared.workers.SimpleWorker;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:10 PM
 * This is the main class for the workstation client. 
 */
public class ConsoleApp {
	
    private static final Logger log = LoggerFactory.getLogger(ConsoleApp.class);

    public static void newBrowser() {
        
        // Prime the tool-specific properties before the Session is invoked
        ConsoleProperties.load();
        
        log.info("Java version: "+System.getProperty("java.version"));
        
        ProtectionDomain pd = ConsoleApp.class.getProtectionDomain();
        log.debug("Code Source: "+pd.getCodeSource().getLocation());
        
        System.setProperty("apple.laf.useScreenMenuBar", "false");
        System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", ConsoleProperties.getString("console.Title"));
        
        // Protocol Registration - Adding more than one type should automatically switch over to the Aggregate Facade
        FacadeManager.registerFacade(FacadeManager.getEJBProtocolString(), EJBFacadeManager.class, "JACS EJB Facade Manager");
        
        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        
        try {
            //Browser Setup
            final String versionString = ConsoleProperties.getString("console.versionNumber");
            final boolean internal = (versionString != null) && (versionString.toLowerCase().contains("internal"));

            sessionMgr.setApplicationName(ConsoleProperties.getString("console.Title"));
            sessionMgr.setApplicationVersion(versionString);
            sessionMgr.setNewBrowserImageIcon(Utils.getClasspathImage("workstation_128_icon.png"));
            sessionMgr.setModelProperty("ShowInternalDataSourceInDialogs", internal);
            sessionMgr.setModelProperty(SessionMgr.DISPLAY_FREE_MEMORY_METER_PROPERTY, false);
            sessionMgr.setModelProperty(SessionMgr.DISPLAY_SUB_EDITOR_PROPERTY, false);
            
            //Exception Handler Registration
            sessionMgr.registerExceptionHandler(new UserNotificationExceptionHandler());
            sessionMgr.registerExceptionHandler(new ExitHandler()); //should be last so that other handlers can complete first.
        	
            final ModelMgr modelMgr = ModelMgr.getModelMgr();
            
            sessionMgr.registerPreferenceInterface(ApplicationSettingsPanel.class, ApplicationSettingsPanel.class);
            sessionMgr.registerPreferenceInterface(UserAccountSettingsPanel.class, UserAccountSettingsPanel.class);
            sessionMgr.registerPreferenceInterface(ViewerSettingsPanel.class, ViewerSettingsPanel.class);

            ServerStatusReportManager.getReportManager().startCheckingForReport();

            FacadeManager.addProtocolToUseList(FacadeManager.getEJBProtocolString());

            // Assuming that the user has entered the login/password information, now validate
            String username = (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME);
            String email = (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_EMAIL);
            
            if (username==null || email==null) {
                Object[] options = {"Enter Login", "Exit Program"};
                final int answer = JOptionPane.showOptionDialog(null, "Please enter your login and email information.", "Information Required",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (answer == 0) {
                    PrefController.getPrefController().getPrefInterface(UserAccountSettingsPanel.class, null);
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
                    PrefController.getPrefController().getPrefInterface(UserAccountSettingsPanel.class, null);
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
                        
            sessionMgr.newBrowser();
            
            log.info("Displaying main frame");
            SessionMgr.getMainFrame().setVisible(true);

            // Once the main frame is visible, we can do some things in the background
            SimpleWorker worker = new SimpleWorker() {

                @Override
                protected void doStuff() throws Exception {
                    //sessionMgr.startExternalHttpListener(30000);
                    sessionMgr.startAxisServer(ConsoleProperties.getString("console.WebServiceURL"));
                    sessionMgr.startWebServer(ConsoleProperties.getInt("console.WebServer.port"));
                }

                @Override
                protected void hadSuccess() {
                }

                @Override
                protected void hadError(Throwable error) {
                    SessionMgr.getSessionMgr().handleException(error);
                }
            };

            worker.execute();
        }
        catch (Exception ex) {
            SessionMgr.getSessionMgr().handleException(ex);
            SessionMgr.getSessionMgr().systemExit();
        }
    }
}
