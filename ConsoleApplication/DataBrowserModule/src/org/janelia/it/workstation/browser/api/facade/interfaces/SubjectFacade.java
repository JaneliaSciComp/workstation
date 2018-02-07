package org.janelia.it.workstation.browser.api.facade.interfaces;

import java.util.List;

import org.janelia.model.domain.Preference;
import org.janelia.model.security.Subject;
import org.janelia.model.security.User;

/**
 * Implementations provide access to subjects and their preferences. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SubjectFacade {

    /**
     * Returns all the subjects (i.e. users and groups) in the system.
     * @return list of Subject objects
     */
    public List<Subject> getSubjects() throws Exception;

    /**
     * Returns a specific subject given it's name or subject key
     * @return the Subject object if it exists
     */
    public Subject getSubjectByNameOrKey(String subjectNameOrKey) throws Exception;

    /**
     * Returns the User for the current authenticated user. If the user does
     * not exist in the system, it is created based on metadata in LDAP.
     * @return User object
     */
    public User getOrCreateUser(String username) throws Exception;

    /**
     * Returns the current subject's preferences.
     * @return
     */
    public List<Preference> getPreferences() throws Exception;

    /**
     * Saves the given preferences.
     * @param preference
     */
    public Preference savePreference(Preference preference) throws Exception;
    
}
