package org.janelia.it.FlyWorkstation.api.entity_model.access;

public class ModelMgrAdapter implements ModelMgrObserver {

	@Override
	public void ontologySelected(long rootId) {
	}

	@Override
	public void ontologyChanged(long rootId) {
	}

	@Override
	public void entitySelected(long entityId, boolean outline) {
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
