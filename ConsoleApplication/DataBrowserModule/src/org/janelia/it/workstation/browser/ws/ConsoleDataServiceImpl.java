package org.janelia.it.workstation.browser.ws;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.StateMgr;
import org.janelia.it.workstation.browser.model.keybind.OntologyKeyBindings;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.browser.util.PathTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the Console server interface which translates domain objects 
 * into Entity objects for compatibility with Neuron Annotator. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@WebService(endpointInterface = "org.janelia.it.FlyWorkstation.ws.ConsoleDataService",
        serviceName = "ConsoleDataService",
        portName = "CdsPort", name = "Cds")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class ConsoleDataServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ConsoleDataServiceImpl.class);

    private DomainToEntityTranslator translator = new DomainToEntityTranslator();
    
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

        if (StateMgr.getStateMgr().getCurrentOntologyId() != null) {
            parameters.clear();
            // Make sure current user has access to the last selected ontology
            Ontology ontology = StateMgr.getStateMgr().getCurrentOntology();
            if (ontology!=null) {
                parameters.put("rootId", ontology.getId());
                client.sendMessage("ontologySelected", parameters);
            }
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
        return translator.createOntologyEntity(rootId);
    }
    
    public AnnotationSession getAnnotationSession(long sessionId) throws Exception {
        log.info("Get annotation session {}", sessionId);
        return null;
    }

    public OntologyKeyBindings getKeybindings(long ontologyId) throws Exception {
        log.info("Get key bindings for ontology {}", ontologyId);
        // TODO: get the actual key bindings from the StageMgr
        return new OntologyKeyBindings(AccessManager.getSubjectKey(), ontologyId);
    }

    public Entity getEntityById(long entityId) throws Exception {
        log.info("Get entity {}", entityId);
        return getEntityTree(entityId);
    }

    public Entity getEntityAndChildren(long entityId) throws Exception {
        log.info("Get entity and children {}", entityId);
        return getEntityTree(entityId);
    }

    public Entity getEntityTree(long entityId) throws Exception {
        log.info("Get entity tree {}", entityId);
        Entity entity = ExternalClientMgr.getInstance().getCachedEntity(entityId);
        return entity;
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

        Entity entity = ExternalClientMgr.getInstance().getCachedEntity(entityId);
        
        if (entity==null || !EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT.equals(entity.getEntityTypeName())) {
         // Only supports getting ancestors of neuron separations
        }
        
        if (EntityConstants.TYPE_SAMPLE.equals(type)) {
            return ExternalClientMgr.getInstance().getCachedSampleForSeparation(entityId);
        }
        
        return null;
    }

    public String getUserAnnotationColor(String username) throws Exception {
        log.info("Get annotation color for user {}", username);
        Color color = StateMgr.getStateMgr().getUserAnnotationColor(username);
        String rgb = Integer.toHexString((color.getRGB() & 0xffffff) | 0x1000000).substring(1);
        return rgb;
    }

}
