package org.janelia.it.FlyWorkstation.ws;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

/**
 * This Axis server contains the web service end-points for both the console observer service and the data service. 
 * It publishes the end-points using JAX-WS.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
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
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("rootId",rootId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("ontologySelected", parameters);
	}

	@Override
	public void ontologyChanged(long rootId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("rootId",rootId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("ontologyChanged", parameters);
	}

	@Override
	public void entitySelected(String category, String entityId, boolean clearAll) {
//		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
//		parameters.put("category",category);
//		parameters.put("entityId",entityId);
//		parameters.put("clearAll",clearAll);
//		SessionMgr.getSessionMgr().sendMessageToExternalClients("entitySelected", parameters);
	}

	@Override
    public void entityDeselected(String category, String entityId) {
//		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
//		parameters.put("category",category);
//		parameters.put("entityId",entityId);
//		SessionMgr.getSessionMgr().sendMessageToExternalClients("entityDeselected", parameters);
	}
	
	@Override
    public void entityChanged(long entityId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityId",entityId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("entityChanged", parameters);
	}

	@Override
    public void entityChildrenChanged(long entityId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityId",entityId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("entityChildrenChanged", parameters);
	}

	@Override
    public void entityRemoved(long entityId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityId",entityId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("entityRemoved", parameters);
    }

	@Override
    public void entityDataRemoved(long entityDataId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityDataId",entityDataId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("entityDataRemoved", parameters);
    }
	
	@Override
	public void entityViewRequested(long entityId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityId",entityId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("entityViewRequested", parameters);		
	}

	@Override
	public void annotationsChanged(long entityId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityId",entityId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("annotationsChanged", parameters);
	}

	@Override
	public void sessionSelected(long sessionId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("sessionId",sessionId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("sessionSelected", parameters);
	}

	@Override
	public void sessionDeselected() {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		SessionMgr.getSessionMgr().sendMessageToExternalClients("sessionDeselected", parameters);
	}

}