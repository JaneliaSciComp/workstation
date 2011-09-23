package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for AnnotationSessionTask which keeps track of associated entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationSession {

    protected final AnnotationSessionTask task;

    // Derived properties
    protected String name;
    protected String owner;
    protected List<Entity> entities;
    protected List<OntologyElement> categories;
    protected List<Entity> annotations;
    protected Map<Long, List<Entity>> annotationMap;

    public AnnotationSession(AnnotationSessionTask task) {
        super();
        this.task = task;
    }

    @Override
    public String toString() {
        String trueName = getName();
        if (trueName == null) return task.getDisplayName() + " " + task.getObjectId();
        return trueName;
    }

    public AnnotationSessionTask getTask() {
        return task;
    }

    public Long getId() {
        return task.getObjectId();
    }
    
    public String getName() {
        return task.getParameter(AnnotationSessionTask.PARAM_sessionName);
    }

    public String getOwner() {
        return task.getOwner();
    }

    public List<Entity> getEntities() {
        if (entities == null) {
            try {
                entities = ModelMgr.getModelMgr().getEntitiesForAnnotationSession(task.getObjectId());
            }
            catch (Exception e) {
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
                List<Entity> tmps = ModelMgr.getModelMgr().getCategoriesForAnnotationSession(task.getObjectId());
                for (Entity tmp : tmps) {
                    categories.add(new OntologyElement(tmp, null));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<OntologyElement>();
            }
        }
        return categories;
    }

    public List<Entity> getAnnotations() {
        if (annotations == null) {
            try {
                annotations = ModelMgr.getModelMgr().getAnnotationsForSession(task.getObjectId());
            }
            catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<Entity>();
            }
        }
        return annotations;
    }

    public Map<Long, List<Entity>> getAnnotationMap() {
        if (annotationMap == null) {
            annotationMap = mapAnnotations(getEntities(), getAnnotations());
        }
        return annotationMap;
    }

    public void clearDerivedProperties() {
        entities = null;
        categories = null;
        annotations = null;
        annotationMap = null;
    }

    private Map<Long, List<Entity>> mapAnnotations(List<Entity> entities, List<Entity> annotations) {

        Map<String, Entity> entityMap = new HashMap<String, Entity>();
        Map<Long, List<Entity>> map = new HashMap<Long, List<Entity>>();

        for (Entity entity : entities) {
            entityMap.put(entity.getId().toString(), entity);
        }

        for (Entity annotation : annotations) {
            EntityData ed = annotation.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
            if (ed == null) continue;
            Entity entity = entityMap.get(ed.getValue());
            if (entity == null) continue;
            List<Entity> entityAnnots = map.get(entity.getId());
            if (entityAnnots == null) {
                entityAnnots = new ArrayList<Entity>();
                map.put(entity.getId(), entityAnnots);
            }
            entityAnnots.add(annotation);
        }

        return map;
    }

}