package org.janelia.it.FlyWorkstation.ws;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService
public interface ConsoleObserver {

    public void ontologySelected(@WebParam(name = "rootId") long rootId);
    
    public void ontologyChanged(@WebParam(name = "rootId") long rootId);

    public void entityOutlineSelected(@WebParam(name = "uniqueId") String uniqueId, @WebParam(name = "clearAll") boolean clearAll);

    public void entityOutlineDeselected(@WebParam(name = "uniqueId") String uniqueId);
    
    public void entitySelected(@WebParam(name = "entityId") long entityId, @WebParam(name = "clearAll") boolean clearAll);

    public void entityDeselected(@WebParam(name = "entityId") long entityId);

    public void entityChanged(@WebParam(name = "entityId") long entityId);
    
    public void entityViewRequested(@WebParam(name = "entityId") long entityId);
    
    public void annotationsChanged(@WebParam(name = "entityId") long entityId);
    
    public void sessionSelected(@WebParam(name = "sessionId") long entityId);
    
    public void sessionDeselected();
    
}
