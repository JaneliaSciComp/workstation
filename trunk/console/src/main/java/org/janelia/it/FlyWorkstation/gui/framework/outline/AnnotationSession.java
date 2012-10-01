package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.ontology.OntologyElement;
import org.janelia.it.jacs.model.tasks.annotation.AnnotationSessionTask;

/**
 * Wrapper for AnnotationSessionTask which keeps track of associated entities.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AnnotationSession {

    protected final AnnotationSessionTask task;

    // Derived properties
    protected List<Entity> entities;
    protected List<OntologyElement> categories;
    protected Set<Long> completedEntityIds;

    public AnnotationSession() {
    	this.task = null;
    }
    
    public AnnotationSession(AnnotationSessionTask task) {
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

	@XmlAttribute
    public Long getId() {
        return task.getObjectId();
    }

	@XmlAttribute
    public String getName() {
        return task.getParameter(AnnotationSessionTask.PARAM_sessionName);
    }

	@XmlAttribute
    public String getOwner() {
        return task.getOwner();
    }

    @XmlTransient
    public synchronized List<Entity> getEntities() {
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

    @XmlTransient
    public synchronized List<OntologyElement> getCategories() {
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

    @XmlTransient
    public synchronized Set<Long> getCompletedEntityIds() {
        if (completedEntityIds == null) {
            try {
            	completedEntityIds = ModelMgr.getModelMgr().getCompletedEntityIds(task.getObjectId());
            }
            catch (Exception e) {
                e.printStackTrace();
                return new HashSet<Long>();
            }
        }
    	return completedEntityIds;
    }

    public synchronized void clearCompletedIds() {
        completedEntityIds = null;
    }
    
    public void clearDerivedProperties() {
        entities = null;
        categories = null;
        completedEntityIds = null;
    }
    
    /**
     * Returns true if all of the categories have been completed for the given entity id.
     * @param entityId
     * @return
     */
    public synchronized boolean isCompleted(Long entityId) {
    	return getCompletedEntityIds().contains(entityId);
    }
    
    /**
     * Returns the percentage of entities (targets) which have been annotated with the required categories. 
     * @return percentage as a real number
     */
    @XmlTransient
	public synchronized double getPercentComplete() {
		return (double)getCompletedEntityIds().size() / (double)getEntities().size();
	}


}