package org.janelia.it.FlyWorkstation.api.entity_model.access;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.tasks.Task;

public interface ModelMgrObserver {

    public void ontologyAdded(Entity ontology);
    public void ontologyRemoved(Entity ontology);
    public void ontologySelected(Entity ontology);
    public void ontologyUnselected(Entity ontology);

    public void annotationSessionCreated(Task annotationSession);
    public void annotationSessionRemoved(Task annotationSession);
    public void annotationSessionSelected(Task annotationSession);
    public void annotationSessionUnselected(Task annotationSession);
    public void annotationSessionCriteriaChanged(Task annotationSession);
}
