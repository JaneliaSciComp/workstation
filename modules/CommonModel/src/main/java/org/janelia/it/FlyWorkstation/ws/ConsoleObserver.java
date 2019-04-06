package org.janelia.it.FlyWorkstation.ws;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

/**
 * Console observer interface for external clients.
 *
 * NOTE: This class must stay in this legacy package until such time that the Neuron Annotator web service client
 * bindings can be regenerated.
 */
@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService
public interface ConsoleObserver {

    public void ontologySelected(@WebParam(name = "rootId") long rootId);

    public void ontologyChanged(@WebParam(name = "rootId") long rootId);

    public void entitySelected(@WebParam(name = "category") String category, @WebParam(name = "entityId") String entityId, @WebParam(name = "clearAll") boolean clearAll);

    public void entityDeselected(@WebParam(name = "category") String category, @WebParam(name = "entityId") String entityId);

    public void entityChanged(@WebParam(name = "entityId") long entityId);

    public void entityViewRequested(@WebParam(name = "entityId") long entityId);

    public void annotationsChanged(@WebParam(name = "entityId") long entityId);

    public void sessionSelected(@WebParam(name = "sessionId") long entityId);

    public void sessionDeselected();

}
