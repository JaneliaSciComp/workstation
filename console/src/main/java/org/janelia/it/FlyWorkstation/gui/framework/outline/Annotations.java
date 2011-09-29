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
    protected Map<Long, List<OntologyAnnotation>> annotationMap;
    
	public Annotations(List<Entity> entities) {
		this.entities = entities;
	}

    public List<Entity> getEntities() {
		return entities;
	}
	
    public void init() {
    	getAnnotationMap();
    }
    
    public List<OntologyAnnotation> getAnnotations() {
    	if (entities == null) return new ArrayList<OntologyAnnotation>();
        if (annotations == null) {
            List<Long> entityIds = new ArrayList<Long>();
            for (Entity entity : entities) {
                entityIds.add(entity.getId());
            }
            try {
                annotations = new ArrayList<OntologyAnnotation>();
                for(Entity entityAnnot : ModelMgr.getModelMgr().getAnnotationsForEntities(entityIds)) {
                	OntologyAnnotation oa = new OntologyAnnotation();
                	oa.init(entityAnnot);
                	annotations.add(oa);
                }
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
                return new ArrayList<OntologyAnnotation>();
            }
        }
        
        return annotations;
    }

    public Map<Long, List<OntologyAnnotation>> getAnnotationMap() {
        if (annotationMap == null) {
            annotationMap = mapAnnotations(entities, getAnnotations());
        }
        return annotationMap;
    }

    private Map<Long, List<OntologyAnnotation>> mapAnnotations(List<Entity> entities, List<OntologyAnnotation> annotations) {

        Map<Long, Entity> entityMap = new HashMap<Long, Entity>();
        Map<Long, List<OntologyAnnotation>> map = new HashMap<Long, List<OntologyAnnotation>>();

        if (entities == null) return map;
        
        for (Entity entity : entities) {
            entityMap.put(entity.getId(), entity);
        }

        for (OntologyAnnotation oa : annotations) {
            Entity entity = entityMap.get(oa.getTargetEntityId());
            if (entity == null) continue;
            List<OntologyAnnotation> oas = map.get(entity.getId());
            if (oas == null) {
                oas = new ArrayList<OntologyAnnotation>();
                map.put(entity.getId(), oas);
            }
            oas.add(oa);
        }

        return map;
    }
}
