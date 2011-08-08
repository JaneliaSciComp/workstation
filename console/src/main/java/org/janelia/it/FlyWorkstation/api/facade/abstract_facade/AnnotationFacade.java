package org.janelia.it.FlyWorkstation.api.facade.abstract_facade;

import org.janelia.it.jacs.model.entity.Entity;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/8/11
 * Time: 9:33 AM
 */
public interface AnnotationFacade extends EntityFacade {
    public List<Entity> getAnnotationsForEntity(String username, Long entityId) throws Exception;

    public List<Entity> getAnnotationsForEntities(String username, List<Long> entityIds) throws Exception;

    public List<Entity> getEntitiesForAnnotationSession(String username, Long annotationSessionId) throws Exception;

    public List<Entity> getAnnotationsForSession(String username, Long annotationSessionId) throws Exception;

    public List<Entity> getCategoriesForAnnotationSession(String username, Long annotationSessionId) throws Exception;

    public void deleteAnnotation(String userlogin, Long annotatedEntityId, String tag);

    public void removeAllOntologyAnnotationsForSession(String username, Long annotationSessionId) throws Exception;
}
