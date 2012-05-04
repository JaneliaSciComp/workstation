package org.janelia.it.FlyWorkstation.api.entity_model.access;

public interface ModelMgrObserver {

    public void ontologySelected(long rootId);
    
    public void ontologyChanged(long rootId);
    
    public void entitySelected(String category, String entityId, boolean clearAll);

    public void entityDeselected(String category, String entityId);
    
    @Deprecated
	public void entityOutlineSelected(String uniqueId, boolean clearAll);
	
    @Deprecated
	public void entityOutlineDeselected(String uniqueId);

    @Deprecated
	public void entitySelected(long entityId, boolean clearAll);

    @Deprecated
    public void entityDeselected(long entityId);

    public void entityChanged(long entityId);
    
    public void entityViewRequested(long entityId);
    
    public void annotationsChanged(long entityId);    
    
    public void sessionSelected(long sessionId);
    
    public void sessionDeselected();
}
