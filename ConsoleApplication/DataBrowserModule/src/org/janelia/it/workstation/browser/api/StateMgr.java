package org.janelia.it.workstation.browser.api;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.state.NavigationHistory;
import org.janelia.it.workstation.browser.api.state.UserColorMapping;
import org.janelia.it.workstation.browser.components.DomainListViewManager;
import org.janelia.it.workstation.browser.components.DomainListViewTopComponent;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.ApplicationClosing;
import org.janelia.it.workstation.browser.events.selection.OntologySelectionEvent;
import org.janelia.it.workstation.browser.gui.options.OptionConstants;
import org.janelia.it.workstation.browser.model.RecentFolder;
import org.janelia.it.workstation.browser.model.keybind.OntologyKeyBind;
import org.janelia.it.workstation.browser.model.keybind.OntologyKeyBindings;
import org.janelia.it.workstation.browser.util.RendererType2D;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.Preference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Category;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.security.util.PermissionTemplate;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.workstation.browser.api.facade.impl.ejb.LegacyFacadeImpl;
import org.janelia.it.workstation.browser.api.facade.interfaces.LegacyFacade;

/**
 * Singleton for tracking and restoring the current state of the GUI. The
 * state may be tracked on a per-user or per-installation basis, but nothing 
 * here will ever modify the actual data. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StateMgr {

    private static final Logger log = LoggerFactory.getLogger(StateMgr.class);
    
    // Singleton
    private static StateMgr instance;
    public static synchronized StateMgr getStateMgr() {
        if (instance==null) {
            instance = new StateMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }
    
    public static final String AUTO_SHARE_TEMPLATE = "Browser.AutoShareTemplate";
    public final static String RECENTLY_OPENED_HISTORY = "Browser.RecentlyOpenedHistory";
    public static final int MAX_RECENTLY_OPENED_HISTORY = 10;
    public static final String ADD_TO_FOLDER_HISTORY = "ADD_TO_FOLDER_HISTORY";
    public static final String ADD_TO_RESULTSET_HISTORY = "ADD_TO_RESULTSET_HISTORY";
    public static final int MAX_ADD_TO_ROOT_HISTORY = 5;
    
    private final LegacyFacade legacyFacade;
    private final Map<TopComponent,NavigationHistory> navigationHistoryMap = new HashMap<>();
    private final UserColorMapping userColorMapping = new UserColorMapping();
    
    private Annotation currentSelectedOntologyAnnotation;
    private OntologyTerm errorOntology;
    private PermissionTemplate autoShareTemplate;

    public static boolean isDarkLook = true;
    
    private StateMgr() {     
        log.info("Initializing State Manager");
        this.legacyFacade = new LegacyFacadeImpl();
        try {
            this.autoShareTemplate = (PermissionTemplate)FrameworkImplProvider.getModelProperty(AUTO_SHARE_TEMPLATE);
            
            if (FrameworkImplProvider.getModelProperty(OptionConstants.UNLOAD_IMAGES_PROPERTY) == null) {
                FrameworkImplProvider.setModelProperty(OptionConstants.UNLOAD_IMAGES_PROPERTY, false);
            }
    
            if (FrameworkImplProvider.getModelProperty(OptionConstants.DISPLAY_RENDERER_2D) == null) {
                FrameworkImplProvider.setModelProperty(OptionConstants.DISPLAY_RENDERER_2D, RendererType2D.IMAGE_IO.toString());
            }
            
            log.debug("Using 2d renderer: {}", FrameworkImplProvider.getModelProperty(OptionConstants.DISPLAY_RENDERER_2D));
        }
        catch (Throwable e) {
            // Catch all exceptions, because anything failing to init here cannot be allowed to prevent the Workstation from starting
            ConsoleApp.handleException(e);
        }
    }

    public void initLAF() {
        
        UIDefaults uiDefaults = UIManager.getDefaults();

        // Workstation LAF defaults. These are overridden by our included Darcula LAF, but should have decent defaults for other LAFs.
        
        if (!uiDefaults.containsKey("ws.ComponentBorderColor")) {
            uiDefaults.put("ws.ComponentBorderColor", uiDefaults.get("windowBorder"));
        }
        
        if (!uiDefaults.containsKey("ws.TreeSecondaryLabel")) {
            uiDefaults.put("ws.TreeSecondaryLabel", new Color(172, 145, 83));
        }
        
        if (!uiDefaults.containsKey("ws.TreeExtraLabel")) {
            uiDefaults.put("ws.TreeExtraLabel", uiDefaults.get("Label.disabledForeground"));
        }
    }
    
    public boolean isDarkLook() {
        Boolean dark = (Boolean) UIManager.get("nb.dark.theme");
        return (dark != null && dark);
    }
    
    @Subscribe
    public void cleanup(ApplicationClosing e) {
        log.info("Saving auto-share template");
        FrameworkImplProvider.setModelProperty(AUTO_SHARE_TEMPLATE, autoShareTemplate);
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
    
    public UserColorMapping getUserColorMapping() {
        return userColorMapping;
    }

    public Color getUserAnnotationColor(String username) {
        return userColorMapping.getColor(username);
    }
    
    public Long getCurrentOntologyId() {
        String lastSelectedOntology = (String) FrameworkImplProvider.getModelProperty("lastSelectedOntology");
        if (StringUtils.isEmpty(lastSelectedOntology)) {
            return null;
        }
        log.debug("Current ontology is {}", lastSelectedOntology);
        return Long.parseLong(lastSelectedOntology);
    }

    public void setCurrentOntologyId(Long ontologyId) {
        log.info("Setting current ontology to {}", ontologyId);
        String idStr = ontologyId==null?null:ontologyId.toString();
        FrameworkImplProvider.setModelProperty("lastSelectedOntology", idStr);
        Events.getInstance().postOnEventBus(new OntologySelectionEvent(ontologyId));
    }

    public Ontology getCurrentOntology() throws Exception {
        Long currentOntologyId = getCurrentOntologyId();
        if (currentOntologyId==null) return null;
        return DomainMgr.getDomainMgr().getModel().getDomainObject(Ontology.class, currentOntologyId);
    }
    
    public Annotation getCurrentSelectedOntologyAnnotation() {
        return currentSelectedOntologyAnnotation;
    }

    public void setCurrentSelectedOntologyAnnotation(Annotation currentSelectedOntologyAnnotation) {
        this.currentSelectedOntologyAnnotation = currentSelectedOntologyAnnotation;
    }
    
    public OntologyTerm getErrorOntology() {
        // TODO: use DomainDAO.getErrorOntologyCategory
        if (errorOntology == null) {
            try {
                List<Ontology> ontologies = DomainMgr.getDomainMgr().getModel().getDomainObjects(Ontology.class, DomainConstants.ERROR_ONTOLOGY_NAME);

                for (Ontology ontology : ontologies) {
                    if (DomainConstants.GENERAL_USER_GROUP_KEY.equals(ontology.getOwnerKey())) {
                        OntologyTerm term = ontology.findTerm(DomainConstants.ERROR_ONTOLOGY_CATEGORY);
                        if (term instanceof Category) {
                            errorOntology = term;
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                ConsoleApp.handleException(ex);
            }
            
        }
        return errorOntology;
    }

    public OntologyKeyBindings loadOntologyKeyBindings(long ontologyId) throws Exception {
        String category = DomainConstants.PREFERENCE_CATEGORY_KEYBINDS_ONTOLOGY + ontologyId;
        List<Preference> prefs = DomainMgr.getDomainMgr().getPreferences(category);
        OntologyKeyBindings ontologyKeyBindings = new OntologyKeyBindings(AccessManager.getSubjectKey(), ontologyId);
        for (Preference pref : prefs) {
            if (pref.getValue()!=null) {
                log.debug("Found preference: {}", pref);
                ontologyKeyBindings.addBinding(pref.getKey(), Long.parseLong((String)pref.getValue()));
            }
        }
        log.debug("Loaded {} key bindings for ontology {}", ontologyKeyBindings.getKeybinds().size(), ontologyKeyBindings.getOntologyId());
        return ontologyKeyBindings;
    }

    public void saveOntologyKeyBindings(OntologyKeyBindings ontologyKeyBindings) throws Exception {
        String category = DomainConstants.PREFERENCE_CATEGORY_KEYBINDS_ONTOLOGY + ontologyKeyBindings.getOntologyId();
        Set<OntologyKeyBind> keybinds = ontologyKeyBindings.getKeybinds();
        log.info("Saving {} key bindings for ontology {}", keybinds.size(), ontologyKeyBindings.getOntologyId());
        
        List<Preference> preferences = DomainMgr.getDomainMgr().getPreferences(category);
        
        for (OntologyKeyBind bind : keybinds) {
            Preference pref = DomainMgr.getDomainMgr().getPreference(category, bind.getKey());
            if (pref!=null) {
                preferences.remove(pref);
            }
            String value = bind.getOntologyTermId().toString();
            if (pref==null) {
                // Create
                pref = new Preference(DomainMgr.getPreferenceSubject(), category, bind.getKey(), value);
                log.info("Creating new preference: {}", pref);
                DomainMgr.getDomainMgr().savePreference(pref);
            }
            else if (!StringUtils.areEqual(pref.getValue(), value)) {
                // Update
                log.info("Updating value for preference {}: {}={}", pref.getId(), pref.getKey(), value);
                pref.setValue(value);
                DomainMgr.getDomainMgr().savePreference(pref);
            }
            else {
                log.info("Preference already exists: {}", pref);
            }
        }
        // Null out the rest of the preferences 
        // TODO: it would be better to delete them, but we would need that implemented in the API 
        for (Preference preference : preferences) {
            preference.setValue(null);
            DomainMgr.getDomainMgr().savePreference(preference);
        }
    }

    public PermissionTemplate getAutoShareTemplate() {
        return autoShareTemplate;
    }

    public void setAutoShareTemplate(PermissionTemplate autoShareTemplate) {
        this.autoShareTemplate = autoShareTemplate;
        FrameworkImplProvider.setModelProperty(AUTO_SHARE_TEMPLATE, autoShareTemplate);
    }
    
    public List<String> getRecentlyOpenedHistory() {
        return getHistoryProperty(RECENTLY_OPENED_HISTORY);
    }

    public void updateRecentlyOpenedHistory(String ref) {
        updateHistoryProperty(RECENTLY_OPENED_HISTORY, MAX_RECENTLY_OPENED_HISTORY, ref);
    }

    public List<RecentFolder> getAddToFolderHistory() {
        List<String> recentFolderStrs = getHistoryProperty(ADD_TO_FOLDER_HISTORY);
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
        updateHistoryProperty(ADD_TO_FOLDER_HISTORY, MAX_ADD_TO_ROOT_HISTORY, recentFolderStr);
    }

    public List<RecentFolder> getAddToResultSetHistory() {
        List<String> recentFolderStrs = getHistoryProperty(ADD_TO_RESULTSET_HISTORY);
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
    
    public void updateAddToResultSetHistory(RecentFolder folder) {
        String recentFolderStr = folder.getPath()+":"+folder.getLabel();
        updateHistoryProperty(ADD_TO_RESULTSET_HISTORY, MAX_ADD_TO_ROOT_HISTORY, recentFolderStr);
    }
    
    private List<String> getHistoryProperty(String prop) {
        @SuppressWarnings("unchecked")
        List<String> history = (List<String>)FrameworkImplProvider.getModelProperty(prop);
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
        FrameworkImplProvider.setModelProperty(prop, copy); 
    }
    
    public Task getTaskById(Long taskId) throws Exception {
        // TODO: use web service
        return DomainMgr.getDomainMgr().getLegacyFacade().getTaskById(taskId);
    }
    
    public Task submitJob(String processName, String displayName, HashSet<TaskParameter> parameters) throws Exception {
        return submitJob(AccessManager.getSubjectKey(), processName, displayName, parameters);
    }
    
    public Task submitJob(String owner, String processName, String displayName, HashSet<TaskParameter> parameters) throws Exception {
        GenericTask task = new GenericTask(new HashSet<Node>(), owner, new ArrayList<Event>(),
                parameters, processName, displayName);
        JsonTask jsonTask = new JsonTask(task);
        Long taskId = DomainMgr.getDomainMgr().getModel().dispatchTask(jsonTask, processName);
        return getTaskById(taskId);
    }
}
