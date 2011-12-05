package org.janelia.it.FlyWorkstation.ws;

import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

public class EmbeddedAxisServer implements ModelMgrObserver {
    
    private final int port;
    private Endpoint obs;
    private Endpoint cds;
    
    public EmbeddedAxisServer(int port) throws Exception {
    	this.port = port;
    }

    public void start() {
        obs = Endpoint.publish("http://localhost:"+port+"/axis2/services/obs", new ConsoleObserverImpl());
        cds = Endpoint.publish("http://localhost:"+port+"/axis2/services/cds", new ConsoleDataServiceImpl());
    }

    public void stop() {
    	if (obs != null) obs.stop();
    	if (cds != null) cds.stop();
    }

	public Endpoint getObserverEndpoint() {
		return obs;
	}

	public Endpoint getDataServiceEndpoint() {
		return cds;
	}

	@Override
	public void ontologySelected(long rootId) {
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put("rootId",rootId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("ontologySelected", parameters);
	}

	@Override
	public void ontologyChanged(long rootId) {
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put("rootId",rootId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("ontologyChanged", parameters);
	}

	@Override
	public void entitySelected(long entityId, boolean outline) {
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put("entityId",entityId);
		parameters.put("outline",outline);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("entitySelected", parameters);
	}

	@Override
	public void entityViewRequested( long entityId) {
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put("entityId",entityId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("entityViewRequested", parameters);		
	}

	@Override
	public void annotationsChanged(long entityId) {
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put("entityId",entityId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("annotationsChanged", parameters);
	}

	@Override
	public void sessionSelected(long sessionId) {
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put("sessionId",sessionId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("sessionSelected", parameters);
	}

	@Override
	public void sessionDeselected() {
		Map<String,Object> parameters = new HashMap<String,Object>();
		SessionMgr.getSessionMgr().sendMessageToExternalClients("sessionDeselected", parameters);
	}

}