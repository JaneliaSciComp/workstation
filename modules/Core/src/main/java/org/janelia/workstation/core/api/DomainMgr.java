package org.janelia.workstation.core.api;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Preference;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;
import org.janelia.model.security.util.SubjectUtils;
import org.janelia.workstation.core.api.exceptions.SystemError;
import org.janelia.workstation.core.api.facade.impl.rest.DomainFacadeImpl;
import org.janelia.workstation.core.api.facade.impl.rest.OntologyFacadeImpl;
import org.janelia.workstation.core.api.facade.impl.rest.SampleFacadeImpl;
import org.janelia.workstation.core.api.facade.impl.rest.SubjectFacadeImpl;
import org.janelia.workstation.core.api.facade.impl.rest.WorkspaceFacadeImpl;
import org.janelia.workstation.core.api.facade.interfaces.DomainFacade;
import org.janelia.workstation.core.api.facade.interfaces.OntologyFacade;
import org.janelia.workstation.core.api.facade.interfaces.SampleFacade;
import org.janelia.workstation.core.api.facade.interfaces.SubjectFacade;
import org.janelia.workstation.core.api.facade.interfaces.WorkspaceFacade;
import org.janelia.workstation.core.api.web.AuthServiceClient;
import org.janelia.workstation.core.api.web.SageRestClient;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ConsolePropsLoaded;
import org.janelia.workstation.core.events.lifecycle.SessionStartEvent;
import org.janelia.workstation.core.events.model.PreferenceChangeEvent;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.core.options.OptionConstants;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton for managing the Domain Model and related data access.
 * <p>
 * Listens for session events and invalidates every object in the model if the current user changes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainMgr {

    private static final Logger log = LoggerFactory.getLogger(DomainMgr.class);

    // Singleton
    private static DomainMgr instance;

    public static DomainMgr getDomainMgr() {
        if (instance == null) {
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
    private PropertyChangeListener listener;

    private DomainModel model;
    private Map<String, Preference> preferenceMap;

    private DomainMgr() {
    }

    @Subscribe
    public synchronized void propsLoaded(ConsolePropsLoaded event) {

        // This initialization must be run on the EDT, because other things in the connection sequence depend
        // on these services, namely the authentication which depends on the AuthServiceClient.

        log.info("Initializing Domain Manager");
        try {
            String domainFacadeURL = ConsoleProperties.getInstance().getProperty("domain.facade.rest.url");
            String legacyDomainFacadeURL = ConsoleProperties.getInstance().getProperty("domain.facade.rest.legacyUrl");
            authClient = new AuthServiceClient(ConsoleProperties.getInstance().getProperty("auth.rest.url"));
            domainFacade = new DomainFacadeImpl(domainFacadeURL);
            ontologyFacade = new OntologyFacadeImpl(domainFacadeURL);
            sampleFacade = new SampleFacadeImpl(domainFacadeURL, legacyDomainFacadeURL);
            subjectFacade = new SubjectFacadeImpl(domainFacadeURL);
            workspaceFacade = new WorkspaceFacadeImpl(domainFacadeURL);
            sageClient = new SageRestClient();
            model = new DomainModel(domainFacade, ontologyFacade, sampleFacade, subjectFacade, workspaceFacade);
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    @Subscribe
    public synchronized void runAsUserChanged(SessionStartEvent event) {
        log.info("User changed, resetting model");
        this.preferenceMap = null;
        model.invalidateAll();
    }

    AuthServiceClient getAuthClient() {
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

    /**
     * Returns a lazy domain model instance.
     *
     * @return domain model
     */
    public DomainModel getModel() {
        return model;
    }

    /**
     * Returns a list of subjects sorted by groups then users, alphabetical by full name.
     * This method runs a remote query and thus should be run in a background thread.
     *
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
     *
     * @param groupKey
     * @return list of users in the given group
     * @throws Exception
     */
    public List<User> getUsersInGroup(String groupKey) throws Exception {
        return DomainMgr.getDomainMgr().getSubjects().stream()
                .filter(s -> SubjectUtils.subjectIsInGroup(s, groupKey))
                .filter(s -> s instanceof User)
                .map(s -> (User) s)
                .collect(Collectors.toList());
    }

    private synchronized void loadPreferences() throws Exception {
        if (preferenceMap == null) {
            this.preferenceMap = new HashMap<>();
            for (Preference preference : subjectFacade.getPreferences()) {
                log.debug("Loaded preference: {}", preference);
                preferenceMap.put(getPreferenceMapKey(preference), preference);
            }
            log.info("Loaded {} user preferences", preferenceMap.size());
        }
    }

    /**
     * Queries the backend and returns the list of preferences for the given subject.
     *
     * @param category
     * @param key
     * @return
     */
    synchronized Preference getPreference(String category, String key) throws Exception {
        loadPreferences();
        String mapKey = category + ":" + key;
        return preferenceMap.get(mapKey);
    }

    @SuppressWarnings("unchecked")
    synchronized <T> T getPreferenceValue(String category, String key, T defaultValue) throws Exception {
        Preference preference = getPreference(category, key);
        if (preference == null) return defaultValue;
        return (T) preference.getValue();
    }

    synchronized List<Preference> getPreferences(String category) throws Exception {
        loadPreferences();
        List<Preference> categoryPreferences = new ArrayList<>();
        for (Preference preference : preferenceMap.values()) {
            if (preference.getCategory().equals(category)) {
                categoryPreferences.add(preference);
            }
        }
        return categoryPreferences;
    }

    /**
     * Saves the given preference.
     *
     * @param preference
     * @throws Exception
     */
    void savePreference(Preference preference) throws Exception {
        Preference updated = subjectFacade.savePreference(preference);
        preferenceMap.put(getPreferenceMapKey(preference), updated);
        notifyPreferenceChanged(updated);
        log.info("Saved preference {} in category {} with {}={}", preference.getId(), preference.getCategory(), preference.getKey(), preference.getValue());
    }

    public static String getPreferenceSubject() {
        if (!AccessManager.loggedIn()) throw new SystemError("Not logged in");
        return ApplicationOptions.getInstance().isUseRunAsUserPreferences() ? AccessManager.getSubjectKey() : AccessManager.getAccessManager().getAuthenticatedSubject().getKey();
    }

    /**
     * Set the given preference value, creating the preference if necessary.
     *
     * @param category
     * @param key
     * @param value
     * @throws Exception
     */
    void setPreference(String category, String key, Object value) throws Exception {
        Preference preference = DomainMgr.getDomainMgr().getPreference(category, key);
        if (preference == null) {
            preference = new Preference(getPreferenceSubject(), category, key, value);
        } else {
            preference.setValue(value);
        }
        savePreference(preference);
    }

    public Map<String, String> loadPreferencesAsMap(String category) throws Exception {
        List<Preference> titlePreferences = DomainMgr.getDomainMgr().getPreferences(category);
        Map<String, String> map = new HashMap<>();
        for (Preference preference : titlePreferences) {
            if (preference.getValue() != null) {
                map.put(preference.getKey(), (String) preference.getValue());
            }
        }
        return map;
    }

    public void saveMapAsPreferences(Map<String, String> map, String category) throws Exception {
        for (String key : map.keySet()) {
            String value = map.get(key);
            if (value != null) {
                Preference preference = DomainMgr.getDomainMgr().getPreference(category, key);
                if (preference == null) {
                    preference = new Preference(getPreferenceSubject(), category, key, value);
                    DomainMgr.getDomainMgr().savePreference(preference);
                } else if (!StringUtils.areEqual(preference.getValue(), value)) {
                    preference.setValue(value);
                    DomainMgr.getDomainMgr().savePreference(preference);
                }
            }
        }
    }

    private String getPreferenceMapKey(Preference preference) {
        return preference.getCategory() + ":" + preference.getKey();
    }

    private void notifyPreferenceChanged(Preference preference) {
        if (log.isTraceEnabled()) {
            log.trace("Generating PreferenceChangeEvent for {}", preference);
        }
        Events.getInstance().postOnEventBus(new PreferenceChangeEvent(preference));
    }
}
