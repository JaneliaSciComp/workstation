package org.janelia.it.workstation.browser.api.lifecycle;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.*;

import org.janelia.it.workstation.browser.gui.dialogs.LoginDialog;
import org.janelia.it.workstation.browser.gui.dialogs.ReleaseNotesDialog;
import org.janelia.it.workstation.browser.options.ApplicationOptions;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.gui.progress.ProgressMeterMgr;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.browser.logging.EDTExceptionInterceptor;
import org.janelia.it.workstation.browser.nb_action.StartPageMenuAction;
import org.janelia.it.workstation.browser.nb_action.NavigateBack;
import org.janelia.it.workstation.browser.nb_action.NavigateForward;
import org.janelia.it.workstation.browser.util.BrandingConfig;
import org.openide.filesystems.FileUtil;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.OnShowing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This carries out tasks that must be done at startup, but may only be done 
 * when the application is ready to show.
 * 
 * @author fosterl
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@OnShowing
public class ShowingHook implements Runnable {
    
    private static final Logger log = LoggerFactory.getLogger(ShowingHook.class);

    // Lazily initialized
    private static ReleaseNotesDialog releaseNotesDialog;

    public void run() {
        
        JFrame frame = WindowLocator.getMainFrame();
        
        // Set the title
        String title = ConsoleApp.getConsoleApp().getApplicationTitle();
        log.info("App title: {}", title);
        frame.setTitle(title);
        
        // Inject special exception handling for uncaught exceptions on the EDT so that they are shown to the user 
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EDTExceptionInterceptor());

        // Disable the navigation actions until there is some history to navigate
        CallableSystemAction.get(NavigateBack.class).setEnabled(false);
        CallableSystemAction.get(NavigateForward.class).setEnabled(false);

        // Things that can be lazily initialized
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                releaseNotesDialog = new ReleaseNotesDialog();
                releaseNotesDialog.showIfFirstRunSinceUpdate();
            }
        });

        log.info("Showing main window");
        frame.setVisible(true);

        // Instantiate singletons so that they register on the event bus
        ProgressMeterMgr.getProgressMeterMgr();

        // Open the start page, if necessary
        try {
            if (ApplicationOptions.getInstance().isShowStartPageOnStartup()) {
                StartPageMenuAction action = new StartPageMenuAction();
                action.actionPerformed(null);
            }
        }
        catch (Throwable e) {
            FrameworkImplProvider.handleExceptionQuietly(e);
        }

        try {
            if (frame.getExtendedState()==JFrame.MAXIMIZED_BOTH) {
                // Workaround for a framework bug. Ensure the window doesn't cover the Windows toolbar. 
                log.info("Window is maximized. Resizing to make sure it doesn't cover Windows toolbar.");
                GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
                frame.setSize(env.getMaximumWindowBounds().getSize());
                frame.setMaximizedBounds(env.getMaximumWindowBounds());
                frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
            }
            else {
                Dimension currSize = frame.getSize();
                if (currSize.width<20 || currSize.height<20) {
                    log.info("Window is too small. Resetting to 80% of screen size.");
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    double width = screenSize.getWidth();
                    double height = screenSize.getHeight();
                    frame.setLocation(new Point(0, 30)); // 30 pixels down to avoid Mac toolbar at the top of the screen
                    frame.setSize(new Dimension((int)Math.round(width*0.8), (int)Math.round(height*0.8)));
                    resetWindows();
                }
            }
        }
        catch (Throwable e) {
            FrameworkImplProvider.handleExceptionQuietly(e);
        }
        
        ConsoleState.setCurrState(ConsoleState.WINDOW_SHOWN);

        try {
            if (Startup.isBrandingValidationException()) {
                JOptionPane.showMessageDialog(
                        WindowLocator.getMainFrame(),
                        "Could not initialize configuration. Please reinstall the application.",
                        "Error initializing configuration",
                        JOptionPane.ERROR_MESSAGE,
                        null
                );
            }
        }
        catch (Throwable e) {
            FrameworkImplProvider.handleExceptionQuietly(e);
        }

        // If there were any issues with auto-login before, resolve them now by showing the login dialog
        if (AccessManager.getAccessManager().hadLoginIssue()) {
            SwingUtilities.invokeLater(() -> {
                LoginDialog.getInstance().showDialog(AccessManager.getAccessManager().getLoginIssue());
            });
        }

//        if (SystemInfo.getJavaInfo().contains("1.7")) {
//
//            String html = "<html><body width='420'>" +
//                "<p>You are using Java 7, which will be unsupported in the near future. It is recommended that you upgrade to Java 8.</p>" +
//                "<br>" +
//                "<p>You can download and install Java 8 on your own, or contact the Workstation Team for assistance.</p>" +
//                "</body></html>";
//            
//            String[] buttons = { "Download Java 8", "Request Assistance", "Ignore For Now" };
//            int selectedOption = JOptionPane.showOptionDialog(WindowLocator.getMainFrame(), html,
//                    "Java Upgrade Recommended", JOptionPane.INFORMATION_MESSAGE, 0, null, buttons, buttons[2]);
//
//            if (selectedOption==0) {
//                Utils.openUrlInBrowser("http://wiki.int.janelia.org/wiki/display/JW/Java+Installation");
//            }
//            else if (selectedOption==1) {                
//                
//                String email = (String) FrameworkImplProvider.getModelProperty(AccessManager.USER_EMAIL);
//                
//                MailDialogueBox popup = MailDialogueBox.newDialog(WindowLocator.getMainFrame(), email)
//                        .withTitle("Create A Ticket")
//                        .withPromptText("Problem Description:")
//                        .withEmailSubject("Java Upgrade Request")
//                        .withTextAreaBody("I need help upgrading my Java version.")
//                        .appendStandardPrefix()
//                        .append("\n\nMessage:\n");
//                
//                String desc = popup.showPopup();
//                if (desc!=null) {
//                    popup.appendLine(desc);
//                    popup.sendEmail();
//                }
//                
//            }
//        }

        if (ApplicationOptions.getInstance().isAutoDownloadUpdates()) {
            AutoUpdater updater = new AutoUpdater() {
                @Override
                protected void hadSuccess() {
                    super.hadSuccess();
                    if (!isRestarting()) {
                        // If we're not already restarting for an updated, check to 
                        // see if we need to restart for branding config changes
                        restartIfBrandingChanged();
                    }
                }  
            };
            updater.execute();
        }
        else {
            restartIfBrandingChanged();
        }
    }
    
    private void restartIfBrandingChanged() {
        if (BrandingConfig.getBrandingConfig().isNeedsRestart()) {
            JOptionPane.showMessageDialog(
                    WindowLocator.getMainFrame(),
                    "Configuration has been updated. Please restart the application.",
                    "Configuration updated",
                    JOptionPane.WARNING_MESSAGE,
                    null
            );
        }
    }

    private void resetWindows() {
        log.info("Resetting windows");
        Action action = FileUtil.getConfigObject("Actions/Window/org-netbeans-core-windows-actions-ResetWindowsAction.instance", Action.class);
        action.actionPerformed(null);
    }

    public static ReleaseNotesDialog getReleaseNotesDialog() {
        return releaseNotesDialog;
    }
}
