package org.janelia.it.workstation.gui.browser.api.facade.interfaces;

import java.util.List;

import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Subject;

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
    public List<Subject> getSubjects();

    /**
     * Returns a specific subject given it's human-readable key
     * @return the Subject object if it exists
     */
    public Subject getSubjectByKey(String subjectKey);

    /**
     * authenticates the user against LDAP then loads the user subject
     * @return authenticated Subject
     */
    public Subject loginSubject(String username, String password);

    /**
     * Returns the current subject's preferences.
     * @return
     */
    public List<Preference> getPreferences();

    /**
     * Saves the given preferences.
     * @param preference
     */
    public Preference savePreference(Preference preference) throws Exception;
    
}
