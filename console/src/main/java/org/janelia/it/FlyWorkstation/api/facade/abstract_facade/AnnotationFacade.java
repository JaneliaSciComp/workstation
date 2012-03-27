package org.janelia.it.FlyWorkstation.api.facade.abstract_facade;

import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.entity.Entity;

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

    public Set<Long> getCompletedEntityIds(Long annotationSessionId) throws Exception;
    
    public void removeAnnotation(Long annotationId) throws Exception;

    public void removeAllOntologyAnnotationsForSession(Long annotationSessionId) throws Exception;
    
    public void createEntityType(String typeName) throws Exception;
    
    public void createEntityAttribute(String typeName, String attrName) throws Exception;
    
    public Entity getAncestorWithType(Entity entity, String typeName) throws Exception;

	public List<List<Long>> searchTreeForNameStartingWith(Long rootId, String searchString) throws Exception;

    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws Exception;

}
