package org.janelia.it.workstation.browser.api.state;

import org.janelia.it.workstation.browser.components.DomainListViewManager;
import org.janelia.it.workstation.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.browser.events.Events;
import org.openide.windows.TopComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton for tracking the state of various data browser UI components.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataBrowserMgr {

    // Singleton
    private static DataBrowserMgr instance;
    public static synchronized DataBrowserMgr getDataBrowserMgr() {
        if (instance==null) {
            instance = new DataBrowserMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    private final Map<TopComponent,NavigationHistory> navigationHistoryMap = new HashMap<>();

    public NavigationHistory getNavigationHistory(DomainListViewTopComponent topComponent) {
        if (topComponent==null) return null;
        NavigationHistory navigationHistory = navigationHistoryMap.get(topComponent);
        if (navigationHistory==null) {
            navigationHistory = new NavigationHistory();
            navigationHistoryMap.put(topComponent, navigationHistory);
        }
        return navigationHistory;
    }

    public NavigationHistory getNavigationHistory() {
        return getNavigationHistory(DomainListViewManager.getInstance().getActiveViewer());
    }

    public void updateNavigationButtons(DomainListViewTopComponent topComponent) {
        NavigationHistory navigationHistory = getNavigationHistory(topComponent);
        if (navigationHistory!=null) {
            navigationHistory.updateButtons();
        }
    }

}
