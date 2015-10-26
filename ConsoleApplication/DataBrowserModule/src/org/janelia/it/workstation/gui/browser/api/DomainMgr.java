package org.janelia.it.workstation.gui.browser.api;

import java.util.List;

import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.gui.browser.api.facade.impl.MongoDomainFacade;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * Singleton for managing the Domain Model and related data access. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainMgr {
    
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
    }
    
    public DomainModel getModel() {
        if (model == null) {
            model = new DomainModel(facade);
        }
        return model;
    }
    
    public List<Subject> getSubjects() {
        List<Subject> subjects = facade.getSubjects();
        DomainUtils.sortSubjects(subjects);
        return subjects;
    }


    
}
