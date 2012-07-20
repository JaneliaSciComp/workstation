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

/**
 * Annotations about the entities which the user is currently interacting with.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Annotations {
	
    protected List<OntologyAnnotation> annotations = new ArrayList<OntologyAnnotation>();
    protected AnnotationFilter filter;
    
	public Annotations() {
	}
    
    public synchronized void init(Long parentId) {
    	
    	if (SwingUtilities.isEventDispatchThread()) throw new RuntimeException("Method must run outside of the EDT");
    	
    	annotations.clear();
    	
        try {
            for(Entity entityAnnot : ModelMgr.getModelMgr().getAnnotationsForChildren(parentId)) {
            	OntologyAnnotation annotation = new OntologyAnnotation();
            	annotation.init(entityAnnot);
            	annotations.add(annotation);
            }
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
    }

    public void reload(Long entityId) {

    	if (SwingUtilities.isEventDispatchThread()) throw new RuntimeException("Method must run outside of the EDT");

		synchronized(this) {
	    	// Remove all the annotations for this entity			
	    	if (annotations!=null) {
		    	List<OntologyAnnotation> copy = new ArrayList<OntologyAnnotation>(annotations);
		    	for(OntologyAnnotation annotation : copy) {
		    		if (annotation.getTargetEntityId()!=null && annotation.getTargetEntityId().equals(entityId)) {
		    			annotations.remove(annotation);
		    		}
		    	}
	    	}
	    	else {
	    		this.annotations = new ArrayList<OntologyAnnotation>();
	    	}
		}
    	
    	
    	// Reload them
        try {
            for(Entity entityAnnot : ModelMgr.getModelMgr().getAnnotationsForEntity(entityId)) {
            	OntologyAnnotation annotation = new OntologyAnnotation();
            	annotation.init(entityAnnot);
                if(annotation.getTargetEntityId()!=null) {
                	synchronized(this) {
                		annotations.add(annotation);
                	}
                }
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
    	
        for (OntologyAnnotation annotation : getFilteredAnnotations()) {
            List<OntologyAnnotation> oas = filteredMap.get(annotation.getTargetEntityId());
            if (oas == null) {
                oas = new ArrayList<OntologyAnnotation>();
                filteredMap.put(annotation.getTargetEntityId(), oas);
            }
            oas.add(annotation);
        }
        
        return filteredMap;
    }
}
