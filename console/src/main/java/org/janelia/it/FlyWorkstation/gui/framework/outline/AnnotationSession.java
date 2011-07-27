package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

/**
 * Wrapper for AnnotationSessionTask which keeps track of associated entities.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationSession {
	
	private final AnnotationSessionTask task;
	private final List<Entity> entities = new ArrayList<Entity>();
	private final List<Entity> categories = new ArrayList<Entity>();
	private final Map<Entity,List<Entity>> annotationMap = new HashMap<Entity,List<Entity>>();
	
	public AnnotationSession(AnnotationSessionTask task) {
		super();
		this.task = task;
	}

	@Override
	public String toString() {
		String name = task.getParameter(AnnotationSessionTask.PARAM_sessionName);
		if (name == null) return task.getDisplayName()+" "+task.getObjectId();
		return name;
	}

	public AnnotationSessionTask getTask() {
		return task;
	}

	public List<Entity> getEntities() {
		return entities;
	}

	public List<Entity> getCategories() {
		return categories;
	}

	public Map<Entity, List<Entity>> getAnnotationMap() {
		return annotationMap;
	}
}