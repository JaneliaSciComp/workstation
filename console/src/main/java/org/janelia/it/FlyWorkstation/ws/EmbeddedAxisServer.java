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
	public void entityOutlineSelected(String uniqueId, boolean clearAll) {
		// Using a LinkedHashMap is necessary because some clients require the parameters to be in the same order
		// defined in the operation's parameterOrder attribute (which is generated from the method signature). 
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("uniqueId",uniqueId);
		parameters.put("clearAll",clearAll);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("entityOutlineSelected", parameters);
	}

	@Override
    public void entityOutlineDeselected(String uniqueId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("uniqueId",uniqueId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("entityOutlineDeselected", parameters);
	}
	
	@Override
	public void entitySelected(long entityId, boolean clearAll) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityId",entityId);
		parameters.put("clearAll",clearAll);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("entitySelected", parameters);
	}

	@Override
    public void entityDeselected(long entityId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityId",entityId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("entityDeselected", parameters);
	}

	@Override
    public void entityChanged(long entityId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityId",entityId);
		SessionMgr.getSessionMgr().sendMessageToExternalClients("entityChanged", parameters);
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