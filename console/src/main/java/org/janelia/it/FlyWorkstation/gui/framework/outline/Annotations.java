package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

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

    public List<Entity> getEntities() {
		return entities;
	}
	
    public void init(List<Entity> entities) {
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
    
    public void setFilter(AnnotationFilter filter) {
		this.filter = filter;
	}
    
	public List<OntologyAnnotation> getAnnotations() {
		return annotations;
	}
    
	public List<OntologyAnnotation> getFilteredAnnotations() {
    	List<OntologyAnnotation> filtered = new ArrayList<OntologyAnnotation>();
        for(OntologyAnnotation annotation : annotations) {
        	if (filter!=null && !filter.accept(annotation)) continue;
        	filtered.add(annotation);
        }
        return filtered;
    }

    public Map<Long, List<OntologyAnnotation>> getFilteredAnnotationMap() {
    	Map<Long, List<OntologyAnnotation>> filteredMap = new HashMap<Long, List<OntologyAnnotation>>();
    	
        Map<Long, Entity> entityMap = new HashMap<Long, Entity>();
        for (Entity entity : entities) {
            entityMap.put(entity.getId(), entity);
        }

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
}
