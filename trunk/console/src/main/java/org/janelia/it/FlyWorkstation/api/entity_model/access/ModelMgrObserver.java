package org.janelia.it.FlyWorkstation.api.entity_model.access;

public interface ModelMgrObserver {

    public void ontologySelected(long rootId);
    
    public void ontologyChanged(long rootId);
    
    public void entitySelected(String category, String entityId, boolean clearAll);

    public void entityDeselected(String category, String entityId);

    public void entityChanged(long entityId);
    
    public void entityRemoved(long entityId);
    
    public void entityDataRemoved(long entityDataId);
    
    public void entityViewRequested(long entityId);
    
    public void annotationsChanged(long entityId);    
    
    public void sessionSelected(long sessionId);
    
    public void sessionDeselected();
}
