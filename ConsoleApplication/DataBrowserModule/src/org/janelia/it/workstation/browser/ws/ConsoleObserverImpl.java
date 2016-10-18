package org.janelia.it.workstation.browser.ws;

import javax.jws.WebService;

/**
 * The SOAP interface which may be implemented by clients wishing to register with the Console and receive event
 * messages.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@WebService(endpointInterface = "org.janelia.it.FlyWorkstation.ws.ConsoleObserver",
        serviceName = "ConsoleObserver",
        portName = "ObsPort")
public class ConsoleObserverImpl {

    private IllegalStateException e = new IllegalStateException(
            "This endpoint is meant as a guideline interface for client implementations. It is not a functional service.");

    public void ontologySelected(long rootId) {
        throw e;
    }

    public void ontologyChanged(long rootId) {
        throw e;
    }

    public void entitySelected(String category, String entityId, boolean clearAll) {
        throw e;
    }

    public void entityDeselected(String category, String entityId) {
        throw e;
    }

    public void entityChanged(long entityId) {
        throw e;
    }

    public void entityChildrenChanged(long entityId) {
        throw e;
    }

    public void entityRemoved(long entityId) {
        throw e;
    }

    public void entityDataRemoved(long entityDataId) {
        throw e;
    }

    public void entityViewRequested(long entityId) {
        throw e;
    }

    public void annotationsChanged(long entityId) {
        throw e;
    }

    public void sessionSelected(long sessionId) {
        throw e;
    }

    public void sessionDeselected() {
        throw e;
    }

}
