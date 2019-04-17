package org.janelia.workstation.browser.api.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.security.util.PermissionTemplate;
import org.janelia.workstation.browser.gui.components.DomainListViewManager;
import org.janelia.workstation.browser.gui.components.DomainListViewTopComponent;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ApplicationClosing;
import org.janelia.workstation.core.model.RecentFolder;
import org.janelia.workstation.core.util.ImageCache;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton for tracking the state of various data browser UI components.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataBrowserMgr {

    private static final Logger log = LoggerFactory.getLogger(DataBrowserMgr.class);

    public final static String RECENTLY_OPENED_HISTORY = "Browser.RecentlyOpenedHistory";
    public static final int MAX_RECENTLY_OPENED_HISTORY = 10;
    public static final String ADD_TO_FOLDER_HISTORY = "ADD_TO_FOLDER_HISTORY";
    public static final String ADD_TO_RESULTSET_HISTORY = "ADD_TO_RESULTSET_HISTORY";
    public static final int MAX_ADD_TO_ROOT_HISTORY = 5;
    private static final String AUTO_SHARE_TEMPLATE = "Browser.AutoShareTemplate";

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
    private final ImageCache imageCache = new ImageCache();
    private PermissionTemplate autoShareTemplate;


    private DataBrowserMgr() {
        this.autoShareTemplate = (PermissionTemplate) FrameworkAccess.getModelProperty(AUTO_SHARE_TEMPLATE);
    }

    public ImageCache getImageCache() {
        return imageCache;
    }

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

    public PermissionTemplate getAutoShareTemplate() {
        return autoShareTemplate;
    }

    public void setAutoShareTemplate(PermissionTemplate autoShareTemplate) {
        this.autoShareTemplate = autoShareTemplate;
        FrameworkAccess.setModelProperty(AUTO_SHARE_TEMPLATE, autoShareTemplate);
    }

    @Subscribe
    public void cleanup(ApplicationClosing e) {
        log.info("Saving auto-share template");
        FrameworkAccess.setModelProperty(AUTO_SHARE_TEMPLATE, autoShareTemplate);
    }

    public List<String> getRecentlyOpenedHistory() {
        return getHistoryProperty(DataBrowserMgr.RECENTLY_OPENED_HISTORY);
    }

    public void updateRecentlyOpenedHistory(String ref) {
        updateHistoryProperty(DataBrowserMgr.RECENTLY_OPENED_HISTORY, DataBrowserMgr.MAX_RECENTLY_OPENED_HISTORY, ref);
    }

    public List<RecentFolder> getAddToFolderHistory() {
        List<String> recentFolderStrs = getHistoryProperty(DataBrowserMgr.ADD_TO_FOLDER_HISTORY);
        List<RecentFolder> recentFolders = new ArrayList<>();

        for(String recentFolderStr : recentFolderStrs) {
            if (recentFolderStr.contains(":")) {
                String[] arr = recentFolderStr.split("\\:");
                String path = arr[0];
                String label = arr[1];
                recentFolders.add(new RecentFolder(path, label));
            }
        }

        return recentFolders;
    }

    public void updateAddToFolderHistory(RecentFolder folder) {
        // TODO: update automatically when a folder is deleted, so it no longer appears in the recent list
        String recentFolderStr = folder.getPath()+":"+folder.getLabel();
        updateHistoryProperty(DataBrowserMgr.ADD_TO_FOLDER_HISTORY, DataBrowserMgr.MAX_ADD_TO_ROOT_HISTORY, recentFolderStr);
    }

    public List<RecentFolder> getAddToResultSetHistory() {
        List<String> recentFolderStrs = getHistoryProperty(DataBrowserMgr.ADD_TO_RESULTSET_HISTORY);
        List<RecentFolder> recentFolders = new ArrayList<>();

        for(String recentFolderStr : recentFolderStrs) {
            if (recentFolderStr.contains(":")) {
                String[] arr = recentFolderStr.split(":");
                String path = arr[0];
                String label = arr[1];
                recentFolders.add(new RecentFolder(path, label));
            }
        }

        return recentFolders;
    }

    public void updateAddToResultSetHistory(RecentFolder folder) {
        String recentFolderStr = folder.getPath()+":"+folder.getLabel();
        updateHistoryProperty(DataBrowserMgr.ADD_TO_RESULTSET_HISTORY, DataBrowserMgr.MAX_ADD_TO_ROOT_HISTORY, recentFolderStr);
    }

    private List<String> getHistoryProperty(String prop) {
        @SuppressWarnings("unchecked")
        List<String> history = (List<String>) FrameworkAccess.getModelProperty(prop);
        if (history == null) return new ArrayList<>();
        // Must make a copy of the list so that we don't use the same reference that's in the cache.
        log.debug("History property {} contains {}",prop,history);
        return new ArrayList<>(history);
    }

    private void updateHistoryProperty(String prop, int maxItems, String value) {
        List<String> history = getHistoryProperty(prop);
        if (history.contains(value)) {
            log.debug("Recently opened history already contains {}. Bringing it forward.",value);
            history.remove(value);
        }
        if (history.size()>=maxItems) {
            history.remove(history.size()-1);
        }
        history.add(0, value);
        log.debug("Adding {} to recently opened history",value);
        // Must make a copy of the list so that our reference doesn't go into the cache.
        List<String> copy = new ArrayList<>(history);
        FrameworkAccess.setModelProperty(prop, copy);
    }

}
