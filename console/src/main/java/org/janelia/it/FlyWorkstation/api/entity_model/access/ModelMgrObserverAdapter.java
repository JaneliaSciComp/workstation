package org.janelia.it.FlyWorkstation.api.entity_model.access;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 2:27 PM
 */
public class ModelMgrObserverAdapter implements org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver {
    @Override
    public void ontologyAdded(Entity ontology) {}

    @Override
    public void ontologyRemoved(Entity ontology) {}

    @Override
    public void ontologySelected(Entity ontology) {}

    @Override
    public void ontologyUnselected(Entity ontology) {}

    @Override
    public void annotationSessionCreated(Task annotationSession) {}

    @Override
    public void annotationSessionRemoved(Task annotationSession) {}

    @Override
    public void annotationSessionSelected(Task annotationSession) {}

    @Override
    public void annotationSessionUnselected(Task annotationSession) {}

    @Override
    public void annotationSessionCriteriaChanged(Task annotationSession) {}
}
