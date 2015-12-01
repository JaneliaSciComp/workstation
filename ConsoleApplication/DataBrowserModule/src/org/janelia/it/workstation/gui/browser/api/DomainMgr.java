package org.janelia.it.workstation.gui.browser.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.gui.browser.api.facade.impl.MongoDomainFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.model.PreferenceChangeEvent;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelAdapter;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionModelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton for managing the Domain Model and related data access. 
 * 
 * Listens for session events and invalidates every object in the model if the user changes. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainMgr {

    private static final Logger log = LoggerFactory.getLogger(DomainMgr.class);
    
    // Singleton
    private static final DomainMgr instance = new DomainMgr();
    public static DomainMgr getDomainMgr() {
        return instance;
    }
    
    private DomainFacade facade;
    private DomainModel model;
    
    private DomainMgr() {
        try {
            this.facade = new MongoDomainFacade();
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }

        SessionModelListener sessionModelListener = new SessionModelAdapter() {
            @Override
            public void modelPropertyChanged(Object key, Object oldValue, Object newValue) {
                if (key == "RunAs" || key == "console.serverLogin") {
                    log.info("User changed, resetting model");
                    model.invalidateAll();
                }
            }
        };
        
        SessionMgr.getSessionMgr().addSessionModelListener(sessionModelListener);
        
    }
    
    /**
     * Returns a lazy domain model instance. 
     * @return domain model
     */
    public DomainModel getModel() {
        if (model == null) {
            model = new DomainModel(facade);
        }
        return model;
    }
    
    /**
     * Queries the backend and returns a list of subjects sorted by: 
     * groups then users, alphabetical by full name. 
     * @return sorted list of subjects
     */
    public List<Subject> getSubjects() {
        List<Subject> subjects = facade.getSubjects();
        DomainUtils.sortSubjects(subjects);
        return subjects;
    }
    
    private Map<String,Preference> preferenceMap;
    
    /**
     * Queries the backend and returns the list of preferences for the given subject.
     * @param subjectId
     * @return
     */
    public Preference getPreference(String category, String key) {
        if (preferenceMap==null) {
            preferenceMap = new HashMap<>();
            for(Preference preference : facade.getPreferences()) {
                preferenceMap.put(getPreferenceMapKey(preference), preference);
            }
            log.info("Loaded {} user preferences",preferenceMap.size());
        }
        String mapKey = category+":"+key;
        return preferenceMap.get(mapKey);
    }
    
    /**
     * Saves the given preference. 
     * @param preference
     * @throws Exception
     */
    public void savePreference(Preference preference) throws Exception {
        Preference updated = facade.savePreference(preference);
        preferenceMap.put(getPreferenceMapKey(preference), updated);
        notifyPreferenceChanged(updated);
    }
    
    private String getPreferenceMapKey(Preference preference) {
        return preference.getCategory()+":"+preference.getKey();
    }
    
    private void notifyPreferenceChanged(Preference preference) {
        if (log.isTraceEnabled()) {
            log.trace("Generating PreferenceChangeEvent for {}", preference);
        }
        Events.getInstance().postOnEventBus(new PreferenceChangeEvent(preference));
    }
}
