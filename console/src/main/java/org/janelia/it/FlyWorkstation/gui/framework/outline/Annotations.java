package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Annotations about the entities which the user is currently interacting with.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Annotations {
	
    protected List<Entity> entities;
    protected List<OntologyAnnotation> annotations;
    protected AnnotationFilter filter;
    
	public Annotations() {
	}

    public synchronized List<Entity> getEntities() {
		return entities;
	}
	
    public synchronized void init(List<Entity> entities) {
    	
    	if (SwingUtilities.isEventDispatchThread()) throw new RuntimeException("Method must run outside of the EDT");
    	
		this.entities = entities;
		this.annotations = new ArrayList<OntologyAnnotation>();
		
        List<Long> entityIds = new ArrayList<Long>();
        for (Entity entity : entities) {
            entityIds.add(entity.getId());
        }
        try {
            for(Entity entityAnnot : ModelMgr.getModelMgr().getAnnotationsForEntities(entityIds)) {
            	OntologyAnnotation annotation = new OntologyAnnotation();
            	annotation.init(entityAnnot);
            	annotations.add(annotation);
            }
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    public synchronized void reload(Long entityId) {

    	if (SwingUtilities.isEventDispatchThread()) throw new RuntimeException("Method must run outside of the EDT");
    	
    	// Remove all the annotations for this entity
    	List<OntologyAnnotation> copy = new ArrayList<OntologyAnnotation>(annotations);
    	for(OntologyAnnotation annotation : copy) {
    		if (null!=annotation.getTargetEntityId() && annotation.getTargetEntityId().equals(entityId)) {
    			annotations.remove(annotation);
    		}
    	}
    	
    	// Reload them
        try {
            for(Entity entityAnnot : ModelMgr.getModelMgr().getAnnotationsForEntity(entityId)) {
            	OntologyAnnotation annotation = new OntologyAnnotation();
            	annotation.init(entityAnnot);
                if(null!=annotation.getTargetEntityId())
            	    annotations.add(annotation);
            }
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }
    
    public synchronized void setFilter(AnnotationFilter filter) {
		this.filter = filter;
	}
    
	public synchronized List<OntologyAnnotation> getAnnotations() {
		if (annotations==null) return new ArrayList<OntologyAnnotation>();
    	// Copy to avoid concurrent modification issues
    	return new ArrayList<OntologyAnnotation>(annotations);
	}
    
	public synchronized List<OntologyAnnotation> getFilteredAnnotations() {
    	List<OntologyAnnotation> filtered = new ArrayList<OntologyAnnotation>();
        for(OntologyAnnotation annotation : annotations) {
        	if (filter!=null && !filter.accept(annotation)) continue;
        	filtered.add(annotation);
        }
        return filtered;
    }

    public synchronized Map<Long, List<OntologyAnnotation>> getFilteredAnnotationMap() {
    	Map<Long, List<OntologyAnnotation>> filteredMap = new HashMap<Long, List<OntologyAnnotation>>();
        Map<Long, Entity> entityMap = EntityUtils.getEntityMap(entities);

        for (OntologyAnnotation annotation : getFilteredAnnotations()) {
            Entity entity = entityMap.get(annotation.getTargetEntityId());
            if (entity == null) continue;
            List<OntologyAnnotation> oas = filteredMap.get(entity.getId());
            if (oas == null) {
                oas = new ArrayList<OntologyAnnotation>();
                filteredMap.put(entity.getId(), oas);
            }
            oas.add(annotation);
        }
        
        return filteredMap;
    }

    /**
     * Returns all the annotations on the given entity, with the specified term.
     * @param entity
     * @param finalTerm
     * @return
     */
	public List<OntologyAnnotation> getTermAnnotations(Entity entity, OntologyElement finalTerm) {
		
		List<OntologyAnnotation> matchingAnnotations = new ArrayList<OntologyAnnotation>();
        for (OntologyAnnotation annotation : getAnnotations()) {
        	if (entity.getId().equals(annotation.getTargetEntityId()) && 
        			(annotation.getKeyEntityId().equals(finalTerm.getId()) || 
        					(annotation.getValueEntityId()!=null && annotation.getValueEntityId().equals(finalTerm.getId())))) {
        		matchingAnnotations.add(annotation);
        	}
        }
		
		return matchingAnnotations;
	}
}
