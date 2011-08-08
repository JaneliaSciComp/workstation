package org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.AnnotationFacade;
import org.janelia.it.jacs.model.entity.Entity;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/8/11
 * Time: 9:31 AM
 */
public class EJBAnnotationFacade extends EJBEntityFacade implements AnnotationFacade {

    @Override
    public List<Entity> getAnnotationsForEntity(String username, Long entityId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getAnnotationsForEntity(username, entityId);
    }

    @Override
    public List<Entity> getAnnotationsForEntities(String username, List<Long> entityIds) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getAnnotationsForEntities(username, entityIds);
    }

    @Override
    public List<Entity> getEntitiesForAnnotationSession(String username, Long annotationSessionId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getEntitiesForAnnotationSession(username, annotationSessionId);
    }

    @Override
    public List<Entity> getAnnotationsForSession(String username, Long annotationSessionId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getAnnotationsForSession(username, annotationSessionId);
    }

    @Override
    public List<Entity> getCategoriesForAnnotationSession(String username, Long annotationSessionId) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getCategoriesForAnnotationSession(username, annotationSessionId);
    }

    @Override
    public void deleteAnnotation(String userlogin, Long annotatedEntityId, String tag) {
        EJBFactory.getRemoteAnnotationBean().deleteAnnotation(userlogin, annotatedEntityId.toString(), tag);
    }

    @Override
    public void removeAllOntologyAnnotationsForSession(String username, Long annotationSessionId) throws Exception {
        EJBFactory.getRemoteAnnotationBean().removeAllOntologyAnnotationsForSession(username, annotationSessionId);
    }


}
