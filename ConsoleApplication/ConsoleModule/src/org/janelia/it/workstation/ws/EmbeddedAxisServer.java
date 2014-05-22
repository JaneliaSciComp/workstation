package org.janelia.it.workstation.ws;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;

/**
 * This Axis server contains the web service end-points for both the console observer service and the data service. 
 * It publishes the end-points using JAX-WS.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EmbeddedAxisServer implements org.janelia.it.workstation.api.entity_model.access.ModelMgrObserver {
    
    private final String baseUrl;
    private Endpoint obs;
    private Endpoint cds;
    
    public EmbeddedAxisServer(String baseUrl) throws Exception {
    	this.baseUrl = baseUrl==null?"http://localhost:30001":baseUrl;
    }

    public void start() {
        obs = Endpoint.publish(baseUrl+"/axis2/services/obs", new ConsoleObserverImpl());
        cds = Endpoint.publish(baseUrl+"/axis2/services/cds", new ConsoleDataServiceImpl());
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
		org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().sendMessageToExternalClients("ontologySelected", parameters);
	}

	@Override
	public void ontologyChanged(long rootId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("rootId",rootId);
		org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().sendMessageToExternalClients("ontologyChanged", parameters);
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
		org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().sendMessageToExternalClients("entityChanged", parameters);
	}

	@Override
    public void entityChildrenChanged(long entityId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityId",entityId);
		org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().sendMessageToExternalClients("entityChildrenChanged", parameters);
	}

	@Override
    public void entityRemoved(long entityId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityId",entityId);
		org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().sendMessageToExternalClients("entityRemoved", parameters);
    }

	@Override
    public void entityDataRemoved(long entityDataId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityDataId",entityDataId);
		org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().sendMessageToExternalClients("entityDataRemoved", parameters);
    }
	
	@Override
	public void entityViewRequested(long entityId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityId",entityId);
		org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().sendMessageToExternalClients("entityViewRequested", parameters);
	}

	@Override
	public void annotationsChanged(long entityId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("entityId",entityId);
		org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().sendMessageToExternalClients("annotationsChanged", parameters);
	}

	@Override
	public void sessionSelected(long sessionId) {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		parameters.put("sessionId",sessionId);
		org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().sendMessageToExternalClients("sessionSelected", parameters);
	}

	@Override
	public void sessionDeselected() {
		Map<String,Object> parameters = new LinkedHashMap<String,Object>();
		org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr.getSessionMgr().sendMessageToExternalClients("sessionDeselected", parameters);
	}

}