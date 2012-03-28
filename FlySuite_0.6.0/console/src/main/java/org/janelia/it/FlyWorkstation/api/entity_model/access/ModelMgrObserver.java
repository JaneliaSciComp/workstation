package org.janelia.it.FlyWorkstation.api.entity_model.access;

public interface ModelMgrObserver {

    public void ontologySelected(long rootId);
    
    public void ontologyChanged(long rootId);

	public void entityOutlineSelected(String uniqueId, boolean clearAll);
	
	public void entityOutlineDeselected(String uniqueId);

	public void entitySelected(long entityId, boolean clearAll);

    public void entityDeselected(long entityId);

    public void entityChanged(long entityId);
    
    public void entityViewRequested(long entityId);
    
    public void annotationsChanged(long entityId);    
    
    public void sessionSelected(long sessionId);
    
    public void sessionDeselected();
}
