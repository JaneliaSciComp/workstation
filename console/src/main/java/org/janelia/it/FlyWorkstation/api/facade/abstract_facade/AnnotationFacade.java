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
    public List<Entity> getAnnotationsForEntity(Long entityId) throws Exception;

    public List<Entity> getAnnotationsForEntities(List<Long> entityIds) throws Exception;

    public List<Entity> getEntitiesForAnnotationSession(Long annotationSessionId) throws Exception;

    public List<Entity> getAnnotationsForSession(Long annotationSessionId) throws Exception;

    public List<Entity> getCategoriesForAnnotationSession(Long annotationSessionId) throws Exception;

    public void deleteAnnotation(Long annotatedEntityId, String tag);

    public void removeAllOntologyAnnotationsForSession(Long annotationSessionId) throws Exception;
}
