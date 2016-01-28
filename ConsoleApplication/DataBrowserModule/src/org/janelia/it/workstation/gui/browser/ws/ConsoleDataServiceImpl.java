package org.janelia.it.workstation.gui.browser.ws;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.model.utils.AnnotationSession;
import org.janelia.it.workstation.model.utils.OntologyKeyBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of the Console server interface.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@WebService(endpointInterface = "org.janelia.it.FlyWorkstation.ws.ConsoleDataService",
        serviceName = "ConsoleDataService",
        portName = "CdsPort", name = "Cds")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class ConsoleDataServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ConsoleDataServiceImpl.class);

    public int reservePort(String clientName) {
        int port = ExternalClientMgr.getInstance().addExternalClient(clientName);
        log.info("Reserving port " + port + " for client " + clientName);
        return port;
    }

    public void registerClient(int port, String endpointUrl) throws Exception {
        ExternalClient client = ExternalClientMgr.getInstance().getExternalClientByPort(port);
        client.init(endpointUrl);
        log.info("Initialized client on port " + port + " with endpoint " + endpointUrl);

        Map<String, Object> parameters = new HashMap<>();

        if (ModelMgr.getModelMgr().getCurrentOntology() != null) {
            parameters.clear();
            parameters.put("rootId", ModelMgr.getModelMgr().getCurrentOntology().getId());
            client.sendMessage("ontologySelected", parameters);
        }

        if (ModelMgr.getModelMgr().getCurrentAnnotationSession() != null) {
            parameters.clear();
            parameters.put("sessionId", ModelMgr.getModelMgr().getCurrentAnnotationSession().getId());
            client.sendMessage("sessionSelected", parameters);
        }
    }

    public void selectEntity(long entityId, boolean clearAll) throws Exception {
        log.info("Select entity {}", entityId);
    }

    public void deselectEntity(long entityId) throws Exception {
        log.info("Deselect entity {}", entityId);
    }

    public Entity createAnnotation(OntologyAnnotation annotation) throws Exception {
        log.info("Create annotation {}", annotation.getKeyString());
        return null;
    }

    public void removeAnnotation(long annotationId) throws Exception {
        log.info("Remove annotation {}", annotationId);
        return;
    }

    public Entity[] getAnnotationsForEntity(long entityId) throws Exception {
        log.info("Get annotations for entity {}", entityId);
        return new Entity[0];
    }

    public Entity[] getAnnotationsForEntities(Long[] entityIds) throws Exception {
        log.info("Get annotations for {} entities", entityIds.length);
        return new Entity[0];
    }

    public Entity getOntology(long rootId) throws Exception {
        log.info("Get ontology {}", rootId);
        return null;
    }

    public AnnotationSession getAnnotationSession(long sessionId) throws Exception {
        log.info("Get annotation session {}", sessionId);
        return null;
    }

    public OntologyKeyBindings getKeybindings(long ontologyId) throws Exception {
        log.info("Get key bindings for ontology {}", ontologyId);
        return null;
    }

    public Entity getEntityById(long entityId) throws Exception {
        log.info("Get entity {}", entityId);
        return null;
    }

    public Entity getEntityAndChildren(long entityId) throws Exception {
        log.info("Get entity and children {}", entityId);
        return null;
    }

    public Entity getEntityTree(long entityId) throws Exception {
        log.info("Get entity tree {}", entityId);
        return null;
    }

    public Entity[] getParentEntityArray(long childEntityId) throws Exception {
        log.info("Get parent entity array for entity {}", childEntityId);
        return new Entity[0];
    }

    public EntityData[] getParentEntityDataArray(long childEntityId) throws Exception {
        log.info("Get parent entity data array for entity {}", childEntityId);
        return new EntityData[0];
    }

    public Entity getAncestorWithType(long entityId, String type) throws Exception {
        log.info("Get ancestor {} for entity {}", type, entityId);
        return null;
    }

    public String getUserAnnotationColor(String username) throws Exception {
        log.info("Get annotation color for user {}", username);
        Color color = ModelMgr.getModelMgr().getUserAnnotationColor(username);
        String rgb = Integer.toHexString((color.getRGB() & 0xffffff) | 0x1000000).substring(1);
        return rgb;
    }
}
