package org.janelia.it.workstation.browser.api.lifecycle;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.gui.progress.ProgressMeterMgr;
import org.janelia.it.workstation.browser.nb_action.NavigateBack;
import org.janelia.it.workstation.browser.nb_action.NavigateForward;
import org.janelia.it.workstation.browser.nb_action.StartPageMenuAction;
import org.janelia.it.workstation.browser.options.ApplicationOptions;
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

        // Disable the navigation actions until there is some history to navigate
        CallableSystemAction.get(NavigateBack.class).setEnabled(false);
        CallableSystemAction.get(NavigateForward.class).setEnabled(false);

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
    }
}
