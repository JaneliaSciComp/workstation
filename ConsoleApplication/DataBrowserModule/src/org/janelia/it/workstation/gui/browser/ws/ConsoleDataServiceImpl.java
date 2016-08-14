package org.janelia.it.workstation.gui.browser.ws;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasImageStack;
import org.janelia.it.jacs.model.domain.ontology.Ontology;
import org.janelia.it.jacs.model.domain.ontology.OntologyTerm;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.api.StateMgr;
import org.janelia.it.workstation.model.utils.AnnotationSession;
import org.janelia.it.workstation.gui.browser.model.keybind.OntologyKeyBindings;
import org.janelia.it.workstation.shared.filestore.PathTranslator;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the Console server interface which translates domain objects 
 * into Entity objects for compatability with Neuron Annotator. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@WebService(endpointInterface = "org.janelia.it.FlyWorkstation.ws.ConsoleDataService",
        serviceName = "ConsoleDataService",
        portName = "CdsPort", name = "Cds")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class ConsoleDataServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ConsoleDataServiceImpl.class);

    private DomainModel model = DomainMgr.getDomainMgr().getModel();
    
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
            parameters.put("rootId", StateMgr.getStateMgr().getCurrentOntologyId());
            client.sendMessage("ontologySelected", parameters);
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
        Ontology ontology = model.getDomainObject(Ontology.class, rootId);
        if (ontology==null) return null;
        return translateToEntity(ontology.getOwnerKey(), ontology);
    }
    
    private Entity translateToEntity(String ownerKey, OntologyTerm ontologyTerm) {
        
        Entity termEntity = new Entity();
        
        termEntity.setId(ontologyTerm.getId());
        termEntity.setName(ontologyTerm.getName());
        termEntity.setOwnerKey(ownerKey);
        
        if (ontologyTerm instanceof Ontology) {
            termEntity.setEntityTypeName(EntityConstants.TYPE_ONTOLOGY_ROOT);
        }
        else {   
            termEntity.setEntityTypeName(EntityConstants.TYPE_ONTOLOGY_ELEMENT);
        }
        
        setValueByAttributeName(termEntity, EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE, ontologyTerm.getTypeName());
        
        if (ontologyTerm.hasChildren()) {
            int index = 0;
            for(OntologyTerm childTerm : ontologyTerm.getTerms()) {
                Entity childEntity = translateToEntity(ownerKey, childTerm);
                EntityData ed = termEntity.addChildEntity(childEntity, EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
                ed.setId(getNewId());
                ed.setOrderIndex(index++);
            }
        }
        
        return termEntity;
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

        NeuronSeparation separation = ExternalClientMgr.getInstance().getNeuronSeparation(entityId);
        if (separation==null) return null; // Only supports getting neuron separations
        
        String opticalRes = null;
        
        PipelineResult result = separation.getParentResult();
        if (result instanceof SampleProcessingResult) {
            SampleProcessingResult sr = (SampleProcessingResult)result;
            opticalRes = sr.getOpticalResolution();
        }
        else if (result instanceof SampleAlignmentResult) {
            HasImageStack sr = (HasImageStack)result;
            opticalRes = sr.getOpticalResolution();
        }
        
        Entity separationEntity = new Entity();
        separationEntity.setId(separation.getId());
        separationEntity.setName(separation.getName());
        separationEntity.setEntityTypeName(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
        setValueByAttributeName(separationEntity, EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, opticalRes);
        setValueByAttributeName(separationEntity, EntityConstants.ATTRIBUTE_FILE_PATH, separation.getFilepath());
        
        Entity fragmentsEntity = new Entity();
        fragmentsEntity.setId(getNewId());
        fragmentsEntity.setName("Neuron Fragments");
        fragmentsEntity.setEntityTypeName(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
        EntityData sed = separationEntity.addChildEntity(fragmentsEntity, EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION);
        sed.setOrderIndex(1);
        sed.setId(getNewId());
        
        int orderIndex = 0;
        for(DomainObject domainObject : model.getDomainObjects(separation.getFragmentsReference())) {
            NeuronFragment fragment = (NeuronFragment)domainObject;
            Entity fragmentEntity = new Entity();
            fragmentEntity.setId(fragment.getId());
            fragmentEntity.setName(fragment.getName());
            fragmentEntity.setEntityTypeName(EntityConstants.TYPE_NEURON_FRAGMENT);
            setValueByAttributeName(fragmentEntity, EntityConstants.ATTRIBUTE_NUMBER, fragment.getNumber().toString());
            EntityData fed = fragmentsEntity.addChildEntity(fragmentEntity, EntityConstants.ATTRIBUTE_ENTITY);
            fed.setOrderIndex(orderIndex++);
            fed.setId(getNewId());
        }
        
        return translatePaths(separationEntity);
    }
    
    private void setValueByAttributeName(Entity entity, String attributeName, String value) {
        // Can't use Entity.setValueByAttributeName, because it doesn't set an id
        EntityData ed=new EntityData();
        ed.setId(getNewId());
        ed.setParentEntity(entity);
        ed.setEntityAttrName(attributeName);
        ed.setValue(value);
        ed.setOwnerKey(entity.getOwnerKey());
        entity.getEntityData().add(ed);
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

        NeuronSeparation separation = ExternalClientMgr.getInstance().getNeuronSeparation(entityId);
        if (separation==null) return null; // Only supports getting ancestors of neuron separations
        
        if (EntityConstants.TYPE_SAMPLE.equals(type)) {
            
            Sample sample = separation.getParentRun().getParent().getParent();
            
            Entity sampleEntity = new Entity();
            sampleEntity.setId(sample.getId());
            sampleEntity.setName(sample.getName());
            sampleEntity.setEntityTypeName(EntityConstants.TYPE_SAMPLE);
            return sampleEntity;
        }
        
        return null;
    }

    public String getUserAnnotationColor(String username) throws Exception {
        log.info("Get annotation color for user {}", username);
        Color color = ModelMgr.getModelMgr().getUserAnnotationColor(username);
        String rgb = Integer.toHexString((color.getRGB() & 0xffffff) | 0x1000000).substring(1);
        return rgb;
    }

    private Entity translatePaths(Entity entity) {
        if (ConsoleProperties.getBoolean("console.WebServer.proxyFiles")) {
            return PathTranslator.translatePathsToProxy(entity);
        } else {
            return PathTranslator.translatePathsToCurrentPlatform(entity);
        }
    }
    
    private Long getNewId() {
        return TimebasedIdentifierGenerator.generateIdList(1).get(0);
    }
}
