package org.janelia.it.FlyWorkstation.api.entity_model.access;

public interface ModelMgrObserver {

    public void ontologySelected(long rootId);
    
    public void ontologyChanged(long rootId);

    public void entitySelected(long entityId, boolean outline);

    public void entityViewRequested(long entityId);
    
    public void annotationsChanged(long entityId);    
    
    public void sessionSelected(long sessionId);
    
    public void sessionDeselected();
}
