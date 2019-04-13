package org.janelia.workstation.common.gui.lifecycle;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ConsoleApp;
import org.janelia.workstation.core.api.lifecycle.AutoUpdater;
import org.janelia.workstation.core.api.lifecycle.ConsoleState;
import org.janelia.workstation.common.gui.dialogs.LoginDialog;
import org.janelia.workstation.common.gui.dialogs.ReleaseNotesDialog;
import org.janelia.workstation.common.gui.support.WindowLocator;
import org.janelia.workstation.common.logging.EDTExceptionInterceptor;
import org.janelia.workstation.core.util.BrandingConfig;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.openide.filesystems.FileUtil;
import org.openide.windows.OnShowing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@OnShowing
public class ShowingHook implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ShowingHook.class);

    // Lazily initialized
    private static ReleaseNotesDialog releaseNotesDialog;

    public void run() {

        log.info("Initializing common GUI");
        ConsoleState.setCurrState(ConsoleState.WINDOW_SHOWN);

        JFrame frame = WindowLocator.getMainFrame();

        String title = String.format("%s %s",
                ConsoleProperties.getString("console.Title"),
                ConsoleProperties.getString("console.versionNumber"));
        log.info("App title: {}", title);
        frame.setTitle(title);

        // Inject special exception handling for uncaught exceptions on the EDT so that they are shown to the user
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EDTExceptionInterceptor());

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
            FrameworkAccess.handleExceptionQuietly(e);
        }

        // Things that can be lazily initialized
        SwingUtilities.invokeLater(() -> {
            releaseNotesDialog = new ReleaseNotesDialog();
            releaseNotesDialog.showIfFirstRunSinceUpdate();
        });

        try {
            if (ConsoleApp.isBrandingValidationException()) {
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
            FrameworkAccess.handleExceptionQuietly(e);
        }

        // If there were any issues with auto-login before, resolve them now by showing the login dialog
        if (AccessManager.getAccessManager().hadLoginIssue()) {
            SwingUtilities.invokeLater(() -> {
                LoginDialog.getInstance().showDialog(AccessManager.getAccessManager().getLoginIssue());
            });
        }

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

    public static ReleaseNotesDialog getReleaseNotesDialog() {
        return releaseNotesDialog;
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
}
