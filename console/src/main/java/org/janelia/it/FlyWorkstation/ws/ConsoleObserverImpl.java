/*
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 8/4/11
 * Time: 1:08 PM
 */
package org.janelia.it.FlyWorkstation.ws;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;

/**
 * The SOAP interface which may be implemented by clients wishing to register with the Console and receive event 
 * messages.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService(endpointInterface="org.janelia.it.FlyWorkstation.ws.ConsoleObserver",
			serviceName="ConsoleObserver",	
			portName="ObsPort", name="Obs")
public class ConsoleObserverImpl implements ModelMgrObserver  {

	private IllegalStateException e = new IllegalStateException(
			"This endpoint is meant as a guideline interface for client implementations. It is not a functional service.");

	@Override
	public void ontologySelected(long rootId) {
		throw e;
	}

	@Override
	public void ontologyChanged(long rootId) {
		throw e;
	}

	@Override
	public void entityOutlineSelected(String uniqueId, boolean clearAll) {
		throw e;
	}

	@Override
    public void entityOutlineDeselected(String uniqueId) {
		throw e;
	}
	
	@Override
	public void entitySelected(long entityId, boolean clearAll) {
		throw e;
	}

	@Override
    public void entityDeselected(long entityId) {
		throw e;
	}

	@Override
    public void entityChanged(long entityId) {
		throw e;
	}
	
	@Override
	public void entityViewRequested(long entityId) {
		throw e;
	}
	
	@Override
	public void annotationsChanged(long entityId) {
		throw e;
	}
	
	@Override
	public void sessionSelected(long sessionId) {
		throw e;
	}
	
	@Override
	public void sessionDeselected() {
		throw e;
	}
	
}