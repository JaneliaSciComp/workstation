package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import java.util.List;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.OntologyFacade;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.types.OntologyElementType;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/5/11
 * Time: 10:48 AM
 */
public class EJBOntologyFacade extends EJBEntityFacade implements OntologyFacade {
    
    public List<Entity> getOntologyRootEntities() throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getOntologyRootEntities(SessionMgr.getSubjectKey());
    }

    @Override
    public Entity createOntologyAnnotation(OntologyAnnotation annotation) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().createOntologyAnnotation(SessionMgr.getSubjectKey(), annotation);
    }
    
    @Override
    public void removeOntologyAnnotation(Long annotationId) throws Exception {
        EJBFactory.getRemoteAnnotationBean().removeOntologyAnnotation(SessionMgr.getSubjectKey(), annotationId);
    }

    @Override
    public Entity createOntologyRoot(String ontologyName) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().createOntologyRoot(SessionMgr.getSubjectKey(), ontologyName);
    }

    @Override
    public EntityData createOntologyTerm(Long parentEntityId, String label, OntologyElementType type, Integer orderIndex) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().createOntologyTerm(SessionMgr.getSubjectKey(),
                parentEntityId, label, type, orderIndex);
    }

    @Override
    public Entity getOntologyTree(Long rootEntityId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getOntologyTree(SessionMgr.getSubjectKey(),
                rootEntityId);
    }

    @Override
    public Entity getErrorOntology() throws Exception{
        return EJBFactory.getRemoteAnnotationBean().getErrorOntology();
    }
}
