package org.janelia.it.workstation.browser.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.exceptions.SystemError;
import org.janelia.it.workstation.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.OntologyFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.SampleFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.SubjectFacade;
import org.janelia.it.workstation.browser.api.facade.interfaces.WorkspaceFacade;
import org.janelia.it.workstation.browser.api.web.AuthServiceClient;
import org.janelia.it.workstation.browser.api.web.SageRestClient;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.SessionStartEvent;
import org.janelia.it.workstation.browser.events.model.PreferenceChangeEvent;
import org.janelia.it.workstation.browser.options.ApplicationOptions;
import org.janelia.it.workstation.browser.options.OptionConstants;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.Preference;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.util.SubjectUtils;
import org.janelia.model.util.ReflectionsFixer;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Singleton for managing the Domain Model and related data access. 
 * 
 * Listens for session events and invalidates every object in the model if the current user changes.
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

    private AuthServiceClient authClient;
    private DomainFacade domainFacade;
    private OntologyFacade ontologyFacade;
    private SampleFacade sampleFacade;
    private SubjectFacade subjectFacade;
    private WorkspaceFacade workspaceFacade;
    private SageRestClient sageClient;
    
    private DomainModel model;
    private Map<String,Preference> preferenceMap;
    
    private DomainMgr() {
        log.info("Initializing Domain Manager");
        try {
            authClient = new AuthServiceClient();
            final Reflections reflections = ReflectionsFixer.getReflections(DOMAIN_FACADE_PACKAGE_NAME, getClass());
            domainFacade = getNewInstance(reflections, DomainFacade.class);
            ontologyFacade = getNewInstance(reflections, OntologyFacade.class);
            sampleFacade = getNewInstance(reflections, SampleFacade.class);
            subjectFacade = getNewInstance(reflections, SubjectFacade.class);
            workspaceFacade = getNewInstance(reflections, WorkspaceFacade.class);
            sageClient = new SageRestClient();
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
        
        ApplicationOptions.getInstance().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(OptionConstants.USE_RUN_AS_USER_PREFERENCES)) {
                    preferenceMap = null;
                    model.invalidateAll();
                }
            }
        });   
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

    public AuthServiceClient getAuthClient() {
        return authClient;
    }

    public DomainFacade getDomainFacade() {
        return domainFacade;
    }

    public OntologyFacade getOntologyFacade() {
        return ontologyFacade;
    }

    public SampleFacade getSampleFacade() {
        return sampleFacade;
    }

    public SubjectFacade getSubjectFacade() {
        return subjectFacade;
    }

    public WorkspaceFacade getWorkspaceFacade() {
        return workspaceFacade;
    }
    
    public SageRestClient getSageClient() {
        return sageClient;
    }

    @Subscribe
    public void runAsUserChanged(SessionStartEvent event) {
        log.info("User changed, resetting model");
        preferenceMap = null;
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
     * Returns a list of subjects sorted by groups then users, alphabetical by full name. 
     * This method runs a remote query and thus should be run in a background thread.
     * @return sorted list of all subjects
     */
    public List<Subject> getSubjects() throws Exception {
        List<Subject> subjects = subjectFacade.getSubjects();
        DomainUtils.sortSubjects(subjects);
        return subjects;
    }

    /**
     * Returns a list of subjects in a specified group, sorted by full name.
     * This method runs a remote query and thus should be run in a background thread. 
     * @param groupKey
     * @return list of users in the given group
     * @throws Exception
     */
    public List<User> getUsersInGroup(String groupKey) throws Exception {
        return DomainMgr.getDomainMgr().getSubjects().stream()
                .filter(s -> SubjectUtils.subjectIsInGroup(s, groupKey))
                .filter(s -> s instanceof User)
                .map(s -> (User)s)
                .collect(Collectors.toList());
    }
    
    private void loadPreferences() throws Exception {
        if (preferenceMap==null) {
            preferenceMap = new HashMap<>();
            for (Preference preference : subjectFacade.getPreferences()) {
                log.debug("Loaded preference: {}",preference);
                preferenceMap.put(getPreferenceMapKey(preference), preference);
            }
            log.info("Loaded {} user preferences", preferenceMap.size());
        }
    }
    
    /**
     * Queries the backend and returns the list of preferences for the given subject.
     * @param category
     * @param key
     * @return
     */
    Preference getPreference(String category, String key) throws Exception {
        loadPreferences();
        String mapKey = category+":"+key;
        return preferenceMap.get(mapKey);
    }

    @SuppressWarnings("unchecked")
    <T> T getPreferenceValue(String category, String key, T defaultValue) throws Exception {
        Preference preference = getPreference(category, key);
        if (preference==null) return defaultValue;
        return (T)preference.getValue();
    }

    List<Preference> getPreferences(String category) throws Exception {
        loadPreferences();
        List<Preference> categoryPreferences = new ArrayList<>();
        for(Preference preference : preferenceMap.values()) {
            if (preference.getCategory().equals(category)) {
                categoryPreferences.add(preference);
            }
        }
        return categoryPreferences;
    }
    
    /**
     * Saves the given preference. 
     * @param preference
     * @throws Exception
     */
    void savePreference(Preference preference) throws Exception {
        Preference updated = subjectFacade.savePreference(preference);
        preferenceMap.put(getPreferenceMapKey(preference), updated);
        notifyPreferenceChanged(updated);
        log.info("Saved preference {} in category {} with {}={}",preference.getId(), preference.getCategory(), preference.getKey(), preference.getValue());
    }

    public static String getPreferenceSubject() {
        if (!AccessManager.loggedIn()) throw new SystemError("Not logged in");
        return ApplicationOptions.getInstance().isUseRunAsUserPreferences() ? AccessManager.getSubjectKey() : AccessManager.getAccessManager().getAuthenticatedSubject().getKey();
    }
    
    /**
     * Set the given preference value, creating the preference if necessary.
     * @param category
     * @param key
     * @param value
     * @throws Exception
     */
    void setPreference(String category, String key, Object value) throws Exception {
        Preference preference = DomainMgr.getDomainMgr().getPreference(category, key);
        if (preference==null) {
            preference = new Preference(getPreferenceSubject(), category, key, value);
        }
        else {
            preference.setValue(value);
        }
        savePreference(preference);
    }

    public Map<String,String> loadPreferencesAsMap(String category) throws Exception {
        List<Preference> titlePreferences = DomainMgr.getDomainMgr().getPreferences(category);
        Map<String,String> map = new HashMap<>();
        for(Preference preference : titlePreferences) {
            if (preference.getValue()!=null) {
                map.put(preference.getKey(), (String)preference.getValue());
            }
        }
        return map;
    }

    public void saveMapAsPreferences(Map<String,String> map, String category) throws Exception {
        for(String key : map.keySet()) {
            String value = map.get(key);
            if (value!=null) {
                Preference preference = DomainMgr.getDomainMgr().getPreference(category, key);
                if (preference==null) {
                    preference = new Preference(getPreferenceSubject(), category, key, value);
                    DomainMgr.getDomainMgr().savePreference(preference);
                }
                else if (!StringUtils.areEqual(preference.getValue(), value)) {
                    preference.setValue(value);
                    DomainMgr.getDomainMgr().savePreference(preference);
                }
            }
        }
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
