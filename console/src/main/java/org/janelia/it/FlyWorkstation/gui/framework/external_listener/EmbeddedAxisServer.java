package org.janelia.it.FlyWorkstation.gui.framework.external_listener;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.janelia.it.FlyWorkstation.api.entity_model.access.ModelMgrObserver;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.ExternalClient;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.tasks.Task;

public class EmbeddedAxisServer implements ModelMgrObserver{

    AxisService service;
    SimpleHTTPServer server;

    public EmbeddedAxisServer(int port) {
        try {
            ConfigurationContext context = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
            AxisService service = AxisService.createService(ClientInterface.class.getName(), context.getAxisConfiguration());//, RPCMessageReceiver.class, "", "http://samples");
            context.getAxisConfiguration().addService(service);
            server = new SimpleHTTPServer(context, port);
        }
        catch (AxisFault axisFault) {
            SessionMgr.getSessionMgr().handleException(axisFault);
        }
    }

    public void start() {
        try {
            server.start();
        }
        catch (AxisFault axisFault) {
            SessionMgr.getSessionMgr().handleException(axisFault);
        }
    }


    public void stop() {
        server.stop();
    }

    @Override
    public void ontologyAdded(Entity ontology) {
        sendOntologyMessage("ontology added", ontology);
    }

    @Override
    public void ontologyRemoved(Entity ontology) {
        
    }

    @Override
    public void ontologySelected(Entity ontology) {
        
    }

    @Override
    public void ontologyUnselected(Entity ontology) {
        
    }

    private void sendOntologyMessage(String s, Entity ontology) {
        for (ExternalClient externalClient : SessionMgr.getSessionMgr().getExternalClients()) {
        }
    }

    @Override
    public void annotationSessionCreated(Task annotationSession) {
        
    }

    @Override
    public void annotationSessionRemoved(Task annotationSession) {
        
    }

    @Override
    public void annotationSessionSelected(Task annotationSession) {
        
    }

    @Override
    public void annotationSessionUnselected(Task annotationSession) {
        
    }

    @Override
    public void annotationSessionCriteriaChanged(Task annotationSession) {
        
    }
}