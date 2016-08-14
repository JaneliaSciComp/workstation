package org.janelia.it.workstation.gui.browser.ws;

import org.janelia.it.workstation.api.entity_model.access.ModelMgrObserver;

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
public class ConsoleObserverImpl implements ModelMgrObserver {

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
    public void entitySelected(String category, String entityId, boolean clearAll) {
        throw e;
    }

    @Override
    public void entityDeselected(String category, String entityId) {
        throw e;
    }

    @Override
    public void entityChanged(long entityId) {
        throw e;
    }

    @Override
    public void entityChildrenChanged(long entityId) {
        throw e;
    }

    @Override
    public void entityRemoved(long entityId) {
        throw e;
    }

    @Override
    public void entityDataRemoved(long entityDataId) {
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
