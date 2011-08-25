package org.janelia.it.FlyWorkstation.ws;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@SOAPBinding(style = SOAPBinding.Style.RPC)
@WebService
public interface ConsoleObserver {

    public void ontologySelected(@WebParam(name = "rootId") long rootId);

    public void entitySelected(@WebParam(name = "entityId") long entityId);

    public void entityViewRequested(@WebParam(name = "entityId") long entityId);
    
    public void annotationsChanged(@WebParam(name = "entityId") long entityId);
    
}
