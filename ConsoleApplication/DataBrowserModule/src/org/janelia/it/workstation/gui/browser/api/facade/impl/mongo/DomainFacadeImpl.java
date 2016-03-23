package org.janelia.it.workstation.gui.browser.api.facade.impl.mongo;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.DomainFacade;

public class DomainFacadeImpl implements DomainFacade {

    private final DomainDAO dao;

    public DomainFacadeImpl() throws Exception {
        this.dao = DomainDAOManager.getInstance().getDao();
    }
    
    @Override
    public <T extends DomainObject> T getDomainObject(Class<T> domainClass, Long id) {
        return dao.getDomainObject(AccessManager.getSubjectKey(), domainClass, id);
    }

    @Override
    public <T extends DomainObject> List<T> getDomainObjects(Class<T> domainClass, String name) {
        return dao.getDomainObjectsByName(AccessManager.getSubjectKey(), domainClass, name);
    }
    
    @Override
    public DomainObject getDomainObject(Reference reference) {
        return dao.getDomainObject(AccessManager.getSubjectKey(), reference);
    }

    @Override
    public List<DomainObject> getDomainObjects(List<Reference> references) {
        return dao.getDomainObjects(AccessManager.getSubjectKey(), references);
    }

    @Override
    public List<DomainObject> getDomainObjects(String className, Collection<Long> ids) {
        return dao.getDomainObjects(AccessManager.getSubjectKey(), className, ids);
    }

    @Override
    public List<DomainObject> getDomainObjects(ReverseReference reference) {
        return dao.getDomainObjects(AccessManager.getSubjectKey(), reference);
    }

    @Override
    public DomainObject updateProperty(DomainObject domainObject, String propName, Object propValue) throws Exception {
        return updateIndex(dao.updateProperty(AccessManager.getSubjectKey(), domainObject.getClass().getName(),
                    domainObject.getId(), propName, propValue));
    }

    @Override
    public DomainObject changePermissions(DomainObject domainObject, String granteeKey, String rights, boolean grant) throws Exception {
        dao.changePermissions(AccessManager.getSubjectKey(), domainObject.getClass().getName(), Arrays.asList(domainObject.getId()), granteeKey, rights, grant);
        return dao.getDomainObject(AccessManager.getSubjectKey(), domainObject);
    }

    private DomainObject updateIndex(DomainObject obj) throws Exception {
        ModelMgr.getModelMgr().updateIndex(obj);
        return obj;
    }

    @Override
    public void remove(List<Reference> deleteObjectRefs) throws Exception {
        for (Reference objectRef : deleteObjectRefs) {
            // first check that it is an objectset or treeNode
            Class<? extends DomainObject> objClass = DomainUtils.getObjectClassByName(objectRef.getTargetClassName());
            if (objClass.equals("org.janelia.it.jacs.model.domain.workspace.TreeNode") ||
                    objClass.equals("org.janelia.it.jacs.model.domain.workspace.ObjectSet")) {
                String subjectKey = AccessManager.getSubjectKey();
                DomainObject domainObj = dao.getDomainObject(subjectKey, objectRef);
                // check whether this subject has permissions to write to this object
                if (domainObj.getWriters().contains(subjectKey)) {
                    dao.remove(subjectKey, domainObj);
                }
            }
        }
    }
    
}
