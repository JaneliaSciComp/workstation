package org.janelia.it.workstation.gui.browser.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.ReflectionsHelper;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.OntologyFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.SampleFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.SubjectFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.WorkspaceFacade;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.lifecycle.RunAsEvent;
import org.janelia.it.workstation.gui.browser.events.model.PreferenceChangeEvent;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Singleton for managing the Domain Model and related data access. 
 * 
 * Listens for session events and invalidates every object in the model if the user changes. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainMgr {

    private static final Logger log = LoggerFactory.getLogger(DomainMgr.class);

    private static final String DOMAIN_FACADE_PACKAGE_NAME = ConsoleProperties.getInstance().getProperty("domain.facade.package");
    
    // Singleton
    private static DomainMgr instance;
    
    public static DomainMgr getDomainMgr() {
        if (instance==null) {            
            instance = new DomainMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }
    
    private DomainFacade domainFacade;
    private OntologyFacade ontologyFacade;
    private SampleFacade sampleFacade;
    private SubjectFacade subjectFacade;
    private WorkspaceFacade workspaceFacade;
    
    private DomainModel model;
    private Map<String,Preference> preferenceMap;
    
    private DomainMgr() {
        try {
            final Reflections reflections = ReflectionsHelper.getReflections(DOMAIN_FACADE_PACKAGE_NAME, getClass());
            domainFacade = getNewInstance(reflections, DomainFacade.class);
            ontologyFacade = getNewInstance(reflections, OntologyFacade.class);
            sampleFacade = getNewInstance(reflections, SampleFacade.class);
            subjectFacade = getNewInstance(reflections, SubjectFacade.class);
            workspaceFacade = getNewInstance(reflections, WorkspaceFacade.class);
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
    
    private <T> T getNewInstance(Reflections reflections, Class<T> clazz) {
        for(Class<? extends T> implClass : reflections.getSubTypesOf(clazz)) {
            try {
                return implClass.newInstance();
            }
            catch (InstantiationException | IllegalAccessException e) {
                log.error("Cannot instantiate "+implClass.getName(),e);
            }
        }
        throw new IllegalStateException("No implementation for "+clazz.getName()+" found in "+DOMAIN_FACADE_PACKAGE_NAME);
    }
    
    @Subscribe
    public void runAsUserChanged(RunAsEvent event) {
        log.info("User changed, resetting model");
        model.invalidateAll();
    }
    
    /**
     * Returns a lazy domain model instance. 
     * @return domain model
     */
    public DomainModel getModel() {
        if (model == null) {
            model = new DomainModel(domainFacade, ontologyFacade, sampleFacade, subjectFacade, workspaceFacade);
        }
        return model;
    }
    
    /**
     * Queries the backend and returns a list of subjects sorted by: 
     * groups then users, alphabetical by full name. 
     * @return sorted list of subjects
     */
    public List<Subject> getSubjects() {
        List<Subject> subjects = subjectFacade.getSubjects();
        DomainUtils.sortSubjects(subjects);
        return subjects;
    }
    
    /**
     * Queries the backend and returns the list of preferences for the given subject.
     * @param subjectId
     * @return
     */
    public Preference getPreference(String category, String key) {
        if (preferenceMap==null) {
            preferenceMap = new HashMap<>();
            log.info("AAAAAA");
            log.info(subjectFacade.getPreferences().toString());
            for(Preference preference : subjectFacade.getPreferences()) {
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
        Preference updated = subjectFacade.savePreference(preference);
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
