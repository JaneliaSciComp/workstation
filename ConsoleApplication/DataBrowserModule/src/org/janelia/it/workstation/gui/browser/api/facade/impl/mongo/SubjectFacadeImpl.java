package org.janelia.it.workstation.gui.browser.api.facade.impl.mongo;

import java.util.List;

import org.janelia.it.jacs.model.domain.Preference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.SubjectFacade;

public class SubjectFacadeImpl implements SubjectFacade {

    private final DomainDAO dao;

    public SubjectFacadeImpl() throws Exception {
        this.dao = DomainDAOManager.getInstance().getDao();
    }

    @Override
    public List<Subject> getSubjects() {
        return dao.getSubjects();
    }

    @Override
    public Subject getSubjectByKey(String key) {
        return dao.getSubjectByKey(key);
    }

    @Override
    public Subject loginSubject(String username, String password) {
        /*BasicAuthToken userInfo = new BasicAuthToken();
        userInfo.setUsername(username);
        userInfo.setPassword(password);
        */return dao.getSubjectByKey("user:" + username);
     }

    @Override
    public List<Preference> getPreferences() {
        return dao.getPreferences(AccessManager.getSubjectKey());
    }

    @Override
    public Preference savePreference(Preference preference) throws Exception {
        return dao.save(AccessManager.getSubjectKey(), preference);
    }
}
