package org.janelia.it.FlyWorkstation.api.entity_model.access;

public class ModelMgrAdapter implements ModelMgrObserver {

	@Override
	public void ontologySelected(long rootId) {
	}

	@Override
	public void ontologyChanged(long rootId) {
	}

	@Override
	public void entitySelected(String category, String entityId, boolean clearAll) {
		
	}

	@Override
    public void entityDeselected(String category, String entityId) {
    	
    }
    
	@Override
	public void entityOutlineSelected(String uniqueId, boolean clearAll) {
	}
	
	@Override
	public void entityOutlineDeselected(String uniqueId) {
	}
	
	@Override
	public void entitySelected(long entityId, boolean clearAll) {
	}

	@Override
    public void entityDeselected(long entityId) {
    }

	@Override
	public void entityChanged(long entityId) {
	}
	
	@Override
    public void entityRemoved(long entityId) {
    	
    }
	
    @Override
    public void entityDataRemoved(long entityDataId) {
    	
    }
    
	@Override
	public void entityViewRequested(long entityId) {
	}

	@Override
	public void annotationsChanged(long entityId) {
	}

	@Override
	public void sessionSelected(long sessionId) {
	}
	
	@Override
	public void sessionDeselected() {
	}
}
