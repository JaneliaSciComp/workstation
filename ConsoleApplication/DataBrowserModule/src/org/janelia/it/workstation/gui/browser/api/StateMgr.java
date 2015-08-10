package org.janelia.it.workstation.gui.browser.api;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.api.entity_model.management.UserColorMapping;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.selection.OntologySelectionEvent;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
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
    public static final String NEURON_ANNOTATOR_CLIENT_NAME = "NeuronAnnotator";
    public static final String CATEGORY_KEYBINDS_GENERAL = "Keybind:General";
    public static final String CATEGORY_KEYBINDS_ONTOLOGY = "Keybind:Ontology:";
    public static final String CATEGORY_SORT_CRITERIA = "SortCriteria:";
    
    private final UserColorMapping userColorMapping = new UserColorMapping();
    
    private StateMgr() {
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

    
    
//    public String getSortCriteria(Long entityId) {
//        Subject subject = SessionMgr.getSessionMgr().getSubject();
//        Map<String, SubjectPreference> prefs = subject.getCategoryPreferences(CATEGORY_SORT_CRITERIA);
//        String entityIdStr = entityId.toString();
//        for (SubjectPreference pref : prefs.values()) {
//            if (pref.getName().equals(entityIdStr)) {
//                return pref.getValue();
//            }
//        }
//        return null;
//    }
//
//    public void saveSortCriteria(Long entityId, String sortCriteria) throws Exception {
//        Subject subject = ModelMgr.getModelMgr().getSubjectWithPreferences(SessionMgr.getSessionMgr().getSubject().getKey());
//        if (StringUtils.isEmpty(sortCriteria)) {
//            subject.getPreferenceMap().remove(CATEGORY_SORT_CRITERIA + ":" + entityId);
//            log.debug("Removed user preference: " + CATEGORY_SORT_CRITERIA + ":" + entityId);
//        }
//        else {
//            subject.setPreference(new SubjectPreference(entityId.toString(), CATEGORY_SORT_CRITERIA, sortCriteria));
//            log.debug("Saved user preference: " + CATEGORY_SORT_CRITERIA + ":" + entityId + "=" + sortCriteria);
//        }
//        Subject newSubject = ModelMgr.getModelMgr().saveOrUpdateSubject(subject);
//        SessionMgr.getSessionMgr().setSubject(newSubject);
//    }
    
    
    
//    public OntologyKeyBindings loadOntologyKeyBindings(long ontologyId) throws Exception {
//        String category = CATEGORY_KEYBINDS_ONTOLOGY + ontologyId;
//        DomainMgr domainMgr = DomainMgr.getDomainMgr();
//        Subject subject = domainMgr.getSubject(SessionMgr.getSessionMgr().getSubject().getKey());
//        Map<String, SubjectPreference> prefs = subject.getCategoryPreferences(category);
//
//        OntologyKeyBindings ontologyKeyBindings = new OntologyKeyBindings(subject.getKey(), ontologyId);
//        for (SubjectPreference pref : prefs.values()) {
//            ontologyKeyBindings.addBinding(pref.getName(), Long.parseLong(pref.getValue()));
//        }
//
//        return ontologyKeyBindings;
//    }
//
//    public void saveOntologyKeyBindings(OntologyKeyBindings ontologyKeyBindings) throws Exception {
//
//        String category = CATEGORY_KEYBINDS_ONTOLOGY + ontologyKeyBindings.getOntologyId();
//        Subject subject = ModelMgr.getModelMgr().getSubjectWithPreferences(SessionMgr.getSessionMgr().getSubject().getKey());
//
//        // First delete all keybinds for this ontology
//        for (String key : subject.getCategoryPreferences(category).keySet()) {
//            subject.getPreferenceMap().remove(category + ":" + key);
//        }
//
//        // Now re-add all the current key bindings
//        Set<OntologyKeyBind> keybinds = ontologyKeyBindings.getKeybinds();
//        for (OntologyKeyBind bind : keybinds) {
//            subject.setPreference(new SubjectPreference(bind.getKey(), category, bind.getOntologyTermId().toString()));
//        }
//
//        Subject newSubject = ModelMgr.getModelMgr().saveOrUpdateSubject(subject);
//        SessionMgr.getSessionMgr().setSubject(newSubject);
//        notifyOntologyChanged(ontologyKeyBindings.getOntologyId());
//    }
//
//    public void removeOntologyKeyBindings(long ontologyId) throws Exception {
//        ModelMgr.getModelMgr().removePreferenceCategory(CATEGORY_KEYBINDS_ONTOLOGY + ontologyId);
//    }
    
    

}
