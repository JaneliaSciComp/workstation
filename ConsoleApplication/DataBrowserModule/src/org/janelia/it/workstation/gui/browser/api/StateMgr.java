package org.janelia.it.workstation.gui.browser.api;

import java.util.List;
import java.util.Set;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.ontology.Category;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.util.PermissionTemplate;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.UserColorMapping;
import org.janelia.it.workstation.gui.browser.api.navigation.NavigationHistory;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.lifecycle.ApplicationClosing;
import org.janelia.it.workstation.gui.browser.events.model.DomainObjectChangeEvent;
import org.janelia.it.workstation.gui.browser.events.selection.OntologySelectionEvent;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.browser.model.keybind.OntologyKeyBind;
import org.janelia.it.workstation.gui.browser.model.keybind.OntologyKeyBindings;
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
    
    // Singleton
    private static final StateMgr instance = new StateMgr();
    public static StateMgr getStateMgr() {
        return instance;
    }
    
    private static final Logger log = LoggerFactory.getLogger(StateMgr.class);
    
    // TODO: externalize these properties
    private static final String AUTO_SHARE_TEMPLATE = "Browser.AutoShareTemplate";
    public static final String NEURON_ANNOTATOR_CLIENT_NAME = "NeuronAnnotator";
    public static final String CATEGORY_KEYBINDS_ONTOLOGY = "Keybind:Ontology:";

    private final NavigationHistory navigationHistory = new NavigationHistory();
    private final UserColorMapping userColorMapping = new UserColorMapping();
    
    private Annotation currentSelectedOntologyAnnotation;
    private OntologyTerm errorOntology;
    private PermissionTemplate autoShareTemplate;

    private StateMgr() {
        this.autoShareTemplate = (PermissionTemplate)SessionMgr.getSessionMgr().getModelProperty(AUTO_SHARE_TEMPLATE);
    }

    @Subscribe
    public void cleanup(ApplicationClosing e) {
        log.info("Saving auto-share template");
        SessionMgr.getSessionMgr().setModelProperty(AUTO_SHARE_TEMPLATE, autoShareTemplate);
    }

    public NavigationHistory getNavigationHistory() {
        return navigationHistory;
    }

    public UserColorMapping getUserColorMapping() {
        return userColorMapping;
    }
    
    public Long getCurrentOntologyId() {
        String lastSelectedOntology = (String) SessionMgr.getSessionMgr().getModelProperty("lastSelectedOntology");
        if (StringUtils.isEmpty(lastSelectedOntology)) {
            return null;
        }
        log.debug("Current ontology is {}", lastSelectedOntology);
        return Long.parseLong(lastSelectedOntology);
    }

    public void setCurrentOntologyId(Long ontologyId) {
        log.info("Setting current ontology to {}", ontologyId);
        String idStr = ontologyId==null?null:ontologyId.toString();
        SessionMgr.getSessionMgr().setModelProperty("lastSelectedOntology", idStr);
        Events.getInstance().postOnEventBus(new OntologySelectionEvent(ontologyId));
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
            
        }
        return errorOntology;
    }

    public OntologyKeyBindings loadOntologyKeyBindings(long ontologyId) throws Exception {
        String category = CATEGORY_KEYBINDS_ONTOLOGY + ontologyId;
        List<Preference> prefs = DomainMgr.getDomainMgr().getPreferences(category);
        OntologyKeyBindings ontologyKeyBindings = new OntologyKeyBindings(AccessManager.getSubjectKey(), ontologyId);
        for (Preference pref : prefs) {
            log.debug("Found preference: {}", pref);
            ontologyKeyBindings.addBinding(pref.getKey(), Long.parseLong((String)pref.getValue()));
        }
        log.debug("Loaded {} key bindings for ontology {}", ontologyKeyBindings.getKeybinds().size(), ontologyKeyBindings.getOntologyId());
        return ontologyKeyBindings;
    }

    public void saveOntologyKeyBindings(OntologyKeyBindings ontologyKeyBindings) throws Exception {
        String category = CATEGORY_KEYBINDS_ONTOLOGY + ontologyKeyBindings.getOntologyId();
        boolean changed = false;
        Set<OntologyKeyBind> keybinds = ontologyKeyBindings.getKeybinds();
        log.debug("Saving {} key bindings for ontology {}", keybinds.size(), ontologyKeyBindings.getOntologyId());
        for (OntologyKeyBind bind : keybinds) {
            Preference pref = DomainMgr.getDomainMgr().getPreference(category, bind.getKey());
            String value = bind.getOntologyTermId().toString();
            if (pref==null) {
                // Create
                pref = new Preference(AccessManager.getSubjectKey(), category, bind.getKey(), value);
                log.debug("Creating new preference: {}", pref);
                DomainMgr.getDomainMgr().savePreference(pref);
                changed = true;
            }
            else if (!StringUtils.areEqual(pref.getValue(), value)) {
                // Update
                log.debug("Updating value for preference {}: {}={}", pref.getId(), pref.getKey(), value);
                pref.setValue(value);
                DomainMgr.getDomainMgr().savePreference(pref);
                changed = true;
            }
            else {
                log.debug("Preference already exists: {}", pref);
            }
        }

        if (changed) {
            Ontology ontology = DomainMgr.getDomainMgr().getModel().getDomainObject(Ontology.class, ontologyKeyBindings.getOntologyId());
            Events.getInstance().postOnEventBus(new DomainObjectChangeEvent(ontology));
        }
    }

    public PermissionTemplate getAutoShareTemplate() {
        return autoShareTemplate;
    }

    public void setAutoShareTemplate(PermissionTemplate autoShareTemplate) {
        this.autoShareTemplate = autoShareTemplate;
        SessionMgr.getSessionMgr().setModelProperty(AUTO_SHARE_TEMPLATE, autoShareTemplate);
    }
}
