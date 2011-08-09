package org.janelia.it.FlyWorkstation.gui.framework.outline;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyElement;

import java.util.ArrayList;
import java.util.List;

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
            List<Long> entityIds = new ArrayList<Long>();
            for (Entity entity : entities) {
                entityIds.add(entity.getId());
            }
            try {
                annotations = ModelMgr.getModelMgr().getAnnotationsForEntities(entityIds);
            }
            catch (Exception e) {
                SessionMgr.getSessionMgr().handleException(e);
                return new ArrayList<Entity>();
            }
        }
        return annotations;
    }

    @Override
    public void clearDerivedProperties() {
        annotations = null;
        annotationMap = null;
    }
}
