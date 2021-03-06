package org.janelia.workstation.browser.api.lifecycle;

import javax.swing.SwingUtilities;

import com.google.common.eventbus.Subscribe;
import org.janelia.workstation.browser.gui.progress.ProgressMeterMgr;
import org.janelia.workstation.browser.actions.NavigateBack;
import org.janelia.workstation.browser.actions.NavigateForward;
import org.janelia.workstation.browser.actions.StartPageMenuAction;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ConsolePropsLoaded;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.OnShowing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data browser initialization done when the main window is shown.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@OnShowing
public class ShowingHook implements Runnable {
    
    private static final Logger log = LoggerFactory.getLogger(ShowingHook.class);

    public void run() {

        log.info("Initializing data browser");

        // Wait for events which are generated by connection to a data server
        Events.getInstance().registerOnEventBus(this);

        // Disable the navigation actions until there is some history to navigate
        CallableSystemAction.get(NavigateBack.class).setEnabled(false);
        CallableSystemAction.get(NavigateForward.class).setEnabled(false);

        // Instantiate singletons so that they register on the event bus
        ProgressMeterMgr.getProgressMeterMgr();
    }


    @Subscribe
    public void propsLoaded(ConsolePropsLoaded event) {

        // Open the start page, if necessary
        SwingUtilities.invokeLater(() -> {
            try {
                if (ApplicationOptions.getInstance().isShowStartPageOnStartup()) {
                    StartPageMenuAction action = new StartPageMenuAction();
                    action.actionPerformed(null);
                }
            }
            catch (Throwable e) {
                FrameworkAccess.handleExceptionQuietly(e);
            }
        });

        // This is only at start up, no need to listen after the first one
        Events.getInstance().unregisterOnEventBus(this);
    }
}
