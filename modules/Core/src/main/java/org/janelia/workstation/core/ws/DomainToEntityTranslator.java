package org.janelia.workstation.core.ws;

import org.hibernate.HibernateException;
import org.janelia.model.access.domain.TimebasedIdentifierGenerator;
import org.janelia.model.domain.DomainUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.util.PathTranslator;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasImageStack;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.ontology.Ontology;
import org.janelia.model.domain.ontology.OntologyTerm;
import org.janelia.model.domain.sample.CuratedNeuron;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleAlignmentResult;
import org.janelia.model.domain.sample.SampleProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates domain objects into old-style entities for use with External Client web services.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainToEntityTranslator {

    private static final Logger log = LoggerFactory.getLogger(DomainToEntityTranslator.class);

    private long dummyId = 0;

    public Entity getOntologyEntity(Long ontologyId) throws Exception {
        Ontology ontology = DomainMgr.getDomainMgr().getModel().getDomainObject(Ontology.class, ontologyId);
        if (ontology==null) return null;
        return getOntologyEntity(ontology);
    }
    
    public Entity getOntologyEntity(Ontology ontology) {
        return getOntologyEntity(ontology.getOwnerKey(), ontology);
    }
    
    private Entity getOntologyEntity(String ownerKey, OntologyTerm ontologyTerm) {
        
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
                Entity childEntity = getOntologyEntity(ownerKey, childTerm);
                EntityData ed = termEntity.addChildEntity(childEntity, EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
                ed.setId(getNewId());
                ed.setOrderIndex(index++);
            }
        }
        
        return termEntity;
    }
    
    public Entity getSeparationEntity(NeuronSeparation separation) {

        PipelineResult result = separation.getParentResult();
        String llFilepath = DomainUtils.getFilepath(result, FileType.LosslessStack);
        String vllFilepath = DomainUtils.getFilepath(result, FileType.VisuallyLosslessStack);
        
        String opticalRes = getOpticalResolution(result);
        String chanSpec = getChannelSpec(result);
        
        Entity separationEntity = new Entity();
        separationEntity.setId(separation.getId());
        separationEntity.setOwnerKey(separation.getParentRun().getParent().getParent().getOwnerKey());
        separationEntity.setName(separation.getName());
        separationEntity.setEntityTypeName(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
        setValueByAttributeName(separationEntity, EntityConstants.ATTRIBUTE_FILE_PATH, separation.getFilepath());
        if (llFilepath!=null) {
            setValueByAttributeName(separationEntity, EntityConstants.ATTRIBUTE_LOSSLESS_IMAGE, llFilepath);
        }
        if (vllFilepath!=null) {
            setValueByAttributeName(separationEntity, EntityConstants.ATTRIBUTE_VISUALLY_LOSSLESS_IMAGE, vllFilepath);
        }
        setValueByAttributeName(separationEntity, EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, chanSpec);
        setValueByAttributeName(separationEntity, EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, opticalRes);
        
        Entity fragmentsEntity = new Entity();
        fragmentsEntity.setId(getNewId());
        fragmentsEntity.setName("Neuron Fragments");
        fragmentsEntity.setEntityTypeName(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
        EntityData sed = separationEntity.addChildEntity(fragmentsEntity, EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION);
        sed.setOrderIndex(1);
        sed.setId(getNewId());
        
        try {
            int orderIndex = 0;
            for(DomainObject domainObject : DomainMgr.getDomainMgr().getModel().getDomainObjects(separation.getFragmentsReference())) {
                NeuronFragment fragment = (NeuronFragment)domainObject;
                if (fragment instanceof CuratedNeuron) {
                    log.trace("Omitting curated fragment: {}", fragment);
                    continue;
                }
                if (fragment.getNumber()==null) {
                    log.warn("Omitting neuron fragment which has no number: {}", fragment);
                    continue;
                }
                Entity fragmentEntity = new Entity();
                fragmentEntity.setId(fragment.getId());
                fragmentEntity.setName(fragment.getName());
                fragmentEntity.setEntityTypeName(EntityConstants.TYPE_NEURON_FRAGMENT);
                setValueByAttributeName(fragmentEntity, EntityConstants.ATTRIBUTE_NUMBER, fragment.getNumber().toString());
                EntityData fed = fragmentsEntity.addChildEntity(fragmentEntity, EntityConstants.ATTRIBUTE_ENTITY);
                fed.setOrderIndex(orderIndex++);
                fed.setId(getNewId());
            }
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
        
        return translatePaths(separationEntity);
    }
    
    public Entity getSampleEntity(Sample sample) {
        Entity sampleEntity = new Entity();
        sampleEntity.setId(sample.getId());
        sampleEntity.setOwnerKey(sample.getOwnerKey());
        sampleEntity.setName(sample.getName());
        sampleEntity.setEntityTypeName(EntityConstants.TYPE_SAMPLE);
        return sampleEntity;
    }

    public Entity getImageEntity(PipelineResult result, FileType fileType) {
        String filepath = DomainUtils.getFilepath(result, fileType);
        Entity imageEntity = new Entity();
        imageEntity.setId(result.getId());
        imageEntity.setOwnerKey(result.getParentRun().getParent().getParent().getOwnerKey());
        imageEntity.setName(result.getName());
        imageEntity.setEntityTypeName(EntityConstants.TYPE_IMAGE_3D);
        setValueByAttributeName(imageEntity, EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, getChannelSpec(result));
        setValueByAttributeName(imageEntity, EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, getOpticalResolution(result));
        setValueByAttributeName(imageEntity, EntityConstants.ATTRIBUTE_FILE_PATH, filepath);
        return translatePaths(imageEntity);
    }

    public Entity getAnotationEntity(Annotation annotation) {
        Entity annotationEntity = new Entity();
        annotationEntity.setId(annotation.getId());
        annotationEntity.setOwnerKey(annotation.getOwnerKey());
        annotationEntity.setName(annotation.getName());
        annotationEntity.setEntityTypeName(EntityConstants.TYPE_ANNOTATION);
        setValueByAttributeName(annotationEntity, EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_ENTITY_ID, annotation.getKeyTerm().getOntologyTermId().toString());
        if (annotation.getValueTerm()!=null) {
            setValueByAttributeName(annotationEntity, EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_ENTITY_ID, annotation.getValueTerm().getOntologyTermId().toString());
        }
        setValueByAttributeName(annotationEntity, EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_KEY_TERM, annotation.getKey());
        setValueByAttributeName(annotationEntity, EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM, annotation.getValue());
        setValueByAttributeName(annotationEntity, EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID, annotation.getTarget().getTargetId().toString());
        return annotationEntity;
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

    private String getChannelSpec(PipelineResult result) {
        if (result instanceof SampleProcessingResult) {
            SampleProcessingResult sr = (SampleProcessingResult)result;
            return sr.getChannelSpec();
        }
        else if (result instanceof SampleAlignmentResult) {
            HasImageStack sr = (HasImageStack)result;
            return sr.getChannelSpec();
        }
        return null;
    }
    
    private String getOpticalResolution(PipelineResult result) {
        if (result instanceof SampleProcessingResult) {
            SampleProcessingResult sr = (SampleProcessingResult)result;
            return sr.getOpticalResolution();
        }
        else if (result instanceof SampleAlignmentResult) {
            HasImageStack sr = (HasImageStack)result;
            return sr.getOpticalResolution();
        }
        return null;
    }

    private Entity translatePaths(Entity entity) {
        if (ConsoleProperties.getBoolean("console.WebServer.proxyFiles")) {
            return PathTranslator.translatePathsToProxy(entity);
        } else {
            return PathTranslator.translatePathsToCurrentPlatform(entity);
        }
    }
    
    private Long getNewId() {
        try {
            return TimebasedIdentifierGenerator.generateIdList(1).get(0);
        }
        catch (HibernateException e) {
            log.error("Error generating a real GUID, falling back on dummy id", e);
            return dummyId++;
        }
        
    }

}
