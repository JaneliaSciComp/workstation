package org.janelia.workstation.core.ws;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.StateMgr;
import org.janelia.workstation.core.model.keybind.OntologyKeyBindings;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.sample.NeuronFragment;
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
        log.warn("Creating annotations via the ConsoleDataService is not longer supported");
//        log.info("Create annotation {}", annotation.getKeyString());
//        // Only support annotations for neuron fragments
//        NeuronFragment domainObject = DomainMgr.getDomainMgr().getModel().getDomainObject(NeuronFragment.class, annotation.getTargetEntityId());
//
//        if (domainObject!=null) {
//            final ApplyAnnotationAction action = ApplyAnnotationAction.get();
//            Long ontologyId = StateMgr.getStateMgr().getCurrentOntologyId();
//            Long ontologyTermId = annotation.getKeyEntityId();
//            OntologyTermReference ref = new OntologyTermReference(ontologyId, ontologyTermId);
//            OntologyTerm term = DomainMgr.getDomainMgr().getModel().getOntologyTermByReference(ref);
//            String value = annotation.getValueString();
//            Annotation savedAnnotation = action.addAnnotation(domainObject, term, value);
//            return translator.getAnotationEntity(savedAnnotation);
//        }
//
        return null;
    }

    public void removeAnnotation(long annotationId) throws Exception {
        log.info("Remove annotation {}", annotationId);
        return;
    }

    public Entity[] getAnnotationsForEntity(long entityId) throws Exception {
        log.info("Get annotations for entity {}", entityId);
        return getAnnotationsForEntities(Arrays.asList(entityId).toArray(new Long[1]));
    }

    public Entity[] getAnnotationsForEntities(Long[] entityIds) throws Exception {
        List<Entity> annotations = new ArrayList<>(); 
        for(Long entityId : entityIds) {
            // Only support annotations for neuron fragments
            for(Annotation annotation : DomainMgr.getDomainMgr().getModel().getAnnotations(Reference.createFor(NeuronFragment.class, entityId))) {
                annotations.add(translator.getAnotationEntity(annotation));
            }
        }
        return annotations.toArray(new Entity[annotations.size()]);
    }

    public Entity getOntology(long rootId) throws Exception {
        log.info("Get ontology {}", rootId);
        return translator.getOntologyEntity(rootId);
    }
    
    public AnnotationSession getAnnotationSession(long sessionId) throws Exception {
        log.info("Get annotation session {}", sessionId);
        // Annotation sessions are no longer supported, but this method must still exist for backwards compatibility
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
        EntityUtils.logEntity(entity);
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
            return null;
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
