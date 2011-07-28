package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

/**
 * Wrapper for AnnotationSessionTask which keeps track of associated entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationSession {
	
	private final AnnotationSessionTask task;
	
	private class StringHolder {
		String str;
	}
	
	// Derived properties
	private StringHolder name;
	private StringHolder owner;
	private List<Entity> entities;
	private List<OntologyElement> categories;
	private List<Entity> annotations;
	private Map<Entity,List<Entity>> annotationMap;
	
	public AnnotationSession(AnnotationSessionTask task) {
		super();
		this.task = task;
	}

	@Override
	public String toString() {
		String trueName = getName();
		if (trueName == null) return task.getDisplayName()+" "+task.getObjectId();
		return trueName;
	}

	public AnnotationSessionTask getTask() {
		return task;
	}

	public String getName() {
		if (name == null) {
			name = new StringHolder();
			name.str = task.getParameter(AnnotationSessionTask.PARAM_sessionName);
		}
		return name.str;
	}

	public String getOwner() {
		if (owner == null) {
			owner = new StringHolder();
			owner.str = task.getOwner();
		}
		return owner.str;
	}

	public List<Entity> getEntities() {
		if (entities==null) {
			try {
				entities = EJBFactory.getRemoteAnnotationBean().getEntitiesForSession(task.getObjectId());	
			}
			catch (ComputeException e) {
				e.printStackTrace();
				return new ArrayList<Entity>(); 
			}
		}
		return entities;
	}

	public List<OntologyElement> getCategories() {
		if (categories == null) {
			try {
				categories = new ArrayList<OntologyElement>();
				List<Entity> tmps = EJBFactory.getRemoteAnnotationBean().getCategoriesForSession(task.getObjectId());	
				for(Entity tmp : tmps) {
					categories.add(new OntologyElement(tmp, null));
				}
			}
			catch (ComputeException e) {
				e.printStackTrace();
				return new ArrayList<OntologyElement>(); 
			}
		}
		return categories;
	}

	public List<Entity> getAnnotations() {
		if (annotations == null) {
			annotations = EJBFactory.getRemoteAnnotationBean().getAnnotationsForEntities(
	                (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME), entities);
		}
		return annotations;
	}

	public Map<Entity, List<Entity>> getAnnotationMap() {
		if (annotationMap == null) {
			annotationMap = new HashMap<Entity,List<Entity>>();
	        for(Entity annotation : getAnnotations()) {
	        	EntityData ed = annotation.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
	        	if (ed == null) continue;
	    		for(Entity entity : entities) {
	    			boolean found = false;
	    			if (entity.getId().toString().equals(ed.getValue())) {
	    				List<Entity> entityAnnots = annotationMap.get(entity);
	    				if (entityAnnots == null) {
	    					entityAnnots = new ArrayList<Entity>();
	    					annotationMap.put(entity, entityAnnots);
	    				}
	    				entityAnnots.add(annotation);
	    				found = true;
	    			}
	    			if (!found) {
	    				System.out.println("Could not find this annotated entity in its session: "+entity.getId());
	    			}
	    		}
	        }
		}
		return annotationMap;
	}

	public void clearDerivedProperties() {
		name = null;
		owner = null;
		entities = null;
		categories = null;
		annotations = null;
		annotationMap = null;
	}
}