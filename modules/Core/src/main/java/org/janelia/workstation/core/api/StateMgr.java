package org.janelia.workstation.core.api;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.Preference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Category;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.workstation.core.api.facade.impl.ejb.LegacyFacadeImpl;
import org.janelia.workstation.core.api.facade.interfaces.LegacyFacade;
import org.janelia.workstation.core.api.state.UserColorMapping;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.selection.OntologySelectionEvent;
import org.janelia.workstation.core.model.keybind.OntologyKeyBind;
import org.janelia.workstation.core.model.keybind.OntologyKeyBindings;
import org.janelia.workstation.core.options.OptionConstants;
import org.janelia.workstation.core.util.RendererType2D;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final LegacyFacade legacyFacade;
    private final UserColorMapping userColorMapping = new UserColorMapping();
    
    private Annotation currentSelectedOntologyAnnotation;
    private OntologyTerm errorOntology;
    
    private StateMgr() {     
        log.info("Initializing State Manager");
        this.legacyFacade = new LegacyFacadeImpl();
        try {
    
            if (FrameworkAccess.getModelProperty(OptionConstants.DISPLAY_RENDERER_2D) == null) {
                FrameworkAccess.setModelProperty(OptionConstants.DISPLAY_RENDERER_2D, RendererType2D.IMAGE_IO.toString());
            }
            
            log.debug("Using 2d renderer: {}", FrameworkAccess.getModelProperty(OptionConstants.DISPLAY_RENDERER_2D));
        }
        catch (Throwable e) {
            // Catch all exceptions, because anything failing to init here cannot be allowed to prevent the Workstation from starting
            FrameworkAccess.handleException(e);
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

    public UserColorMapping getUserColorMapping() {
        return userColorMapping;
    }

    public Color getUserAnnotationColor(String username) {
        return userColorMapping.getColor(username);
    }
    
    public Long getCurrentOntologyId() {
        String lastSelectedOntology = (String) FrameworkAccess.getModelProperty("lastSelectedOntology");
        if (StringUtils.isEmpty(lastSelectedOntology)) {
            return null;
        }
        log.debug("Current ontology is {}", lastSelectedOntology);
        return Long.parseLong(lastSelectedOntology);
    }

    public void setCurrentOntologyId(Long ontologyId) {
        log.info("Setting current ontology to {}", ontologyId);
        String idStr = ontologyId==null?null:ontologyId.toString();
        FrameworkAccess.setModelProperty("lastSelectedOntology", idStr);
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
                FrameworkAccess.handleException(ex);
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

    public Task getTaskById(Long taskId) throws Exception {
        // TODO: use web service
        return legacyFacade.getTaskById(taskId);
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
