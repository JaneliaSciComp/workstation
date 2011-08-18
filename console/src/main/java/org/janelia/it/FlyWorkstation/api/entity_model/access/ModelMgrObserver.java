package org.janelia.it.FlyWorkstation.api.entity_model.access;


public interface ModelMgrObserver {

    public void ontologySelected(long rootId);

    public void entitySelected(long entityId);

    public void annotationsChanged(long entityId);
    
}
