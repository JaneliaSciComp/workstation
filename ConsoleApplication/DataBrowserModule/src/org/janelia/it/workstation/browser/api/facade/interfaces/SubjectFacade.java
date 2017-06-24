package org.janelia.it.workstation.browser.api.facade.interfaces;

import java.util.List;

import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.subjects.Group;
import org.janelia.it.jacs.model.domain.subjects.GroupRole;
import org.janelia.it.jacs.model.domain.subjects.User;

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
     * Returns a specific subject given it's human-readable key
     * @return the Subject object if it exists
     */
    public Subject getSubjectByKey(String subjectKey) throws Exception;
    
    /**
     * Saves changes to the given subject's metadata.
     * @param subject subject whose metadata to save
     * @return the updated subject
     * @throws Exception 
     */
    public <T extends Subject> T save(T subject) throws Exception;
    
    /**
     * Set the user's role in the given group.
     * @param user
     * @param group
     * @param groupRole
     * @return
     * @throws Exception
     */
    public User addUserGroupRole(User user, Group group, GroupRole groupRole) throws Exception;
    
    /**
     * Remove all the user's roles in the given group.
     * @param user
     * @param group
     * @return
     * @throws Exception
     */
    public User removeUserGroupRoles(User user, Group group) throws Exception;
    
    /**
     * authenticates the user against LDAP then loads the user subject
     * @return authenticated Subject
     */
    public Subject loginSubject(String username, String password) throws Exception;

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
