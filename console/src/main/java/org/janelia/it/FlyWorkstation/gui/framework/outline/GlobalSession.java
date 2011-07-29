package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.FlyWorkstation.gui.framework.api.EJBFactory;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyElement;

/**
 * Special "AnnotationSession" which includes all annotations ever made.  
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GlobalSession extends AnnotationSession {

	public GlobalSession(List<Entity> entities) {
		super(null);
		this.entities = entities;
		this.categories = new ArrayList<OntologyElement>();
	}

	@Override
	public String getName() {
		return "[GlobalAnnotationSession]";
	}

	@Override
	public String getOwner() {
		return "[GlobalUser]";
	}

	@Override
	public List<Entity> getEntities() {
		return entities;
	}

	@Override
	public List<OntologyElement> getCategories() {
		return categories;
	}

	@Override
	public List<Entity> getAnnotations() {
		if (annotations == null) {
			annotations = EJBFactory.getRemoteAnnotationBean().getAnnotationsForEntities(
	                (String)SessionMgr.getSessionMgr().getModelProperty(SessionMgr.USER_NAME), entities);
		}
		return annotations;
	}

	@Override
	public void clearDerivedProperties() {
		annotations = null;
		annotationMap = null;
	}
}
