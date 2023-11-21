package org.janelia.workstation.common.gui.lifecycle;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.common.gui.dialogs.ConnectDialog;
import org.janelia.workstation.common.gui.dialogs.LoginDialog;
import org.janelia.workstation.common.gui.dialogs.ReleaseNotesDialog;
import org.janelia.workstation.common.gui.support.WindowLocator;
import org.janelia.workstation.common.logging.EDTExceptionInterceptor;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.ConnectionMgr;
import org.janelia.workstation.core.api.lifecycle.AutoUpdater;
import org.janelia.workstation.core.api.lifecycle.ConsoleState;
import org.janelia.workstation.core.api.lifecycle.GracefulBrick;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ConsolePropsLoaded;
import org.janelia.workstation.core.model.ConnectionResult;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.core.util.BrandingConfig;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.workers.IndeterminateProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileUtil;
import org.openide.windows.OnShowing;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
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

        log.info("Initializing common GUI module");
        ConsoleState.setCurrState(ConsoleState.WINDOW_SHOWN);
        JFrame frame = WindowLocator.getMainFrame();

        StatusDisplayer.getDefault().setStatusText("Initializing...");

        // Inject special exception handling for uncaught exceptions on the EDT so that they are shown to the user
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EDTExceptionInterceptor());

        // Set the application title
        String title = String.format("%s %s",
                ConsoleProperties.getString("client.Title"),
                ConsoleProperties.getString("client.versionNumber"));
        log.info("Application title: {}", title);
        frame.setTitle(title);

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

        try {
            if (BrandingConfig.isBrandingValidationException()) {
                JOptionPane.showMessageDialog(
                        FrameworkAccess.getMainFrame(),
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

        // Wait for events which are generated by connection to a data server
        Events.getInstance().registerOnEventBus(this);

        // This thread is already running in an EDT, but it's NetBean's EDT, which causes some problems if you
        // try to display certain GUI elements. To be safe, let's move to the Swing EDT.
        SwingUtilities.invokeLater(() -> {

            // Connect to the data server
            StatusDisplayer.getDefault().setStatusText("Connecting to server...");

            SimpleWorker worker = new SimpleWorker() {

                private ConnectionResult connectionResult;

                @Override
                protected void doStuff() throws Exception {
                    String connectionString = ConnectionMgr.getConnectionMgr().getConnectionString();

                    if (StringUtils.isBlank(connectionString)) {
                        log.debug("Connection string is blank, asking for user input");
                        ConnectDialog connectDialog = new ConnectDialog();
                        connectDialog.showDialog();
                    }
                    else {
                        connectionResult = ConnectionMgr.getConnectionMgr().connect(connectionString);
                    }
                }

                @Override
                protected void hadSuccess() {
                    if (connectionResult != null && connectionResult.getErrorText() != null) {
                        ConnectDialog connectDialog = new ConnectDialog();
                        connectDialog.showDialog(connectionResult);
                    }
                }

                @Override
                protected void hadError(Throwable e) {
                    log.error("Unknown connection error", e);
                }
            };

            worker.execute();
        });
    }

    /**
     * After connecting to a data server, the remote properties must be fetched from the server, and updated locally.
     * Once that is done, this method will hear the event that is generated, and we can pick up where we left off.
     */
    @Subscribe
    public void propsLoaded(ConsolePropsLoaded event) {

        StatusDisplayer.getDefault().setStatusText("Connected");

        // Set the update center URL
        String updateCenterURL = ConsoleProperties.getString("updates.url");
        if (!StringUtils.isBlank(updateCenterURL) && !SystemInfo.isDev) {
            AutoUpdater.setUpdateCenterURL(updateCenterURL);
        }

        // Check for potential remote brick
        try {
            GracefulBrick uninstaller = new GracefulBrick();
            uninstaller.brickAndUninstall();
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }

        if (ApplicationOptions.getInstance().isAutoDownloadUpdates()) {
            if (SystemInfo.isDev || SystemInfo.isTest) {
                log.info("Skipping updates on non-production build");
            }
            else {
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
        }
        else {
            restartIfBrandingChanged();
        }

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                // Auto-login if credentials were saved from a previous session
                AccessManager.getAccessManager().loginUsingSavedCredentials();
            }

            @Override
            protected void hadSuccess() {
                if (isCancelled()) {
                    AccessManager.getAccessManager().logout();
                }

                // If there were any issues with auto-login before, resolve them now by showing the login dialog
                if (AccessManager.getAccessManager().hadLoginIssue()) {
                    SwingUtilities.invokeLater(() -> {
                        LoginDialog.getInstance().showDialog(AccessManager.getAccessManager().getLoginIssue());
                    });
                }

                // Things that can be done lazily
                SwingUtilities.invokeLater(() -> getReleaseNotesDialog().showIfFirstRunSinceUpdate());

                // TODO: this shouldn't be in here, it should be in a separate ShowingHook for the Horta module
                if (ApplicationOptions.getInstance().isShowHortaOnStartup()) {
                    TopComponent tc = WindowManager.getDefault().findTopComponent("InfoPanelTopComponent");
                    if (tc != null) {
                        tc.open();
                        tc.requestActive();
                    }
                }
            }

            @Override
            protected void hadError(Throwable e) {
                FrameworkAccess.handleException(e);
            }
        };

        worker.setProgressMonitor(new IndeterminateProgressMonitor(FrameworkAccess.getMainFrame(), "Logging in...", ""));
        worker.execute();
    }

    public static synchronized ReleaseNotesDialog getReleaseNotesDialog() {
        if (releaseNotesDialog==null) {
            releaseNotesDialog = new ReleaseNotesDialog();
        }
        return releaseNotesDialog;
    }

    private void restartIfBrandingChanged() {
        if (BrandingConfig.getBrandingConfig().isNeedsRestart()) {
            JOptionPane.showMessageDialog(
                    FrameworkAccess.getMainFrame(),
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
