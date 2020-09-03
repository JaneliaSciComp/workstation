package org.janelia.workstation.core.model;

import com.google.common.collect.Lists;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.SampleUtils;
import org.janelia.model.domain.gui.cdmip.ColorDepthImage;
import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.model.domain.sample.*;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.util.StringUtilsExtra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Mapping from one domain object type to another. For example, converting a set of Samples to their 
 * LSM images. Or moving from an unaligned fragment to its aligned version. 
 *
 * TODO: move the domain-specific logic to a confocal module
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectMapper {

    private final static Logger log = LoggerFactory.getLogger(DomainModelViewUtils.class);
    
    private Collection<DomainObject> domainObjects;
    private String alignmentSpace;

    public DomainObjectMapper(Collection<DomainObject> domainObjects) {
        this.domainObjects = domainObjects;
    }

    public void setAlignmentSpace(String alignmentSpace) {
        this.alignmentSpace = alignmentSpace;
    }

    public Collection<MappingType> getMappableTypes() {

        Set<MappingType> types = new LinkedHashSet<>();
        for (DomainObject domainObject : domainObjects) {
            types.addAll(getMappableTypes(domainObject.getClass()));
        }
        
        return types;
    }

    /**
     * Returns a list of the types which the given domain object class can be mapped to using the map functions.
     * @param clazz
     * @return list of mapping targets
     */
    private Collection<MappingType> getMappableTypes(Class<? extends DomainObject> clazz) {

        List<MappingType> types = new ArrayList<>();

        if (Sample.class.isAssignableFrom(clazz)) {
            types.add(MappingType.LSM);
        }
        else if (LSMImage.class.isAssignableFrom(clazz)) {
            types.add(MappingType.Sample);
        }
        else if (ColorDepthImage.class.isAssignableFrom(clazz)) {
            types.add(MappingType.Sample);
        }
        else if (NeuronFragment.class.isAssignableFrom(clazz)) {
            types.add(MappingType.Sample);
            types.add(MappingType.LSM);
            types.add(MappingType.AlignedNeuronFragment);
            types.add(MappingType.UnalignedNeuronFragment);
        }

        return types;
    }

    /**
     * Map the given objects to related objects of the given target type.
     * @param targetType
     * @return
     * @throws Exception
     */
    public <T extends DomainObject> List<T> map(MappingType targetType) throws Exception {

        List<T> mapped = new ArrayList<>();
        for (DomainObject domainObject : domainObjects) {
            for(DomainObject result : map(domainObject, targetType)) {
                if (result != null) {
                    mapped.add((T)result);
                }
            }
        }
        
        return mapped;
    }
    
    /**
     * Map the given object to the given class, and return a list of joined objects.
     * @param domainObject
     * @param targetType
     * @return
     * @throws Exception
     */
    private List<DomainObject> map(DomainObject domainObject, MappingType targetType) throws Exception {

        List<DomainObject> mapped = new ArrayList<>();
        
        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            
            if (targetType==MappingType.LSM) {
                mapped.addAll(DomainMgr.getDomainMgr().getModel().getDomainObjectsAs(LSMImage.class, sample.getLsmReferences()));
            }
            else if (targetType==MappingType.Sample) {
                mapped.add(sample);
            }
            else {
                log.warn("Cannot map Samples to "+targetType);
            }
            
        }
        else if (domainObject instanceof LSMImage) {
            LSMImage lsm = (LSMImage)domainObject;
            
            if (targetType==MappingType.Sample) {
                mapped.add(DomainMgr.getDomainMgr().getModel().getDomainObject(lsm.getSample()));
            }
            else if (targetType==MappingType.LSM) {
                mapped.add(lsm);
            }
            else {
                log.warn("Cannot map LSMImage to "+targetType);
            }
            
        }
        else if (domainObject instanceof ColorDepthMatch) {
            ColorDepthMatch cdm = (ColorDepthMatch)domainObject;
            ColorDepthImage image = DomainMgr.getDomainMgr().getModel().getDomainObject(cdm.getImageRef());
            if (image!=null) {
                if (targetType == MappingType.Sample) {
                    mapped.add(DomainMgr.getDomainMgr().getModel().getDomainObject(image.getSampleRef()));
                } else {
                    log.warn("Cannot map ColorDepthImage to " + targetType);
                }
            }
            else {
                log.warn("Could not retrieve " + cdm.getImageRef());
            }
        }
        else if (domainObject instanceof ColorDepthImage) {
            ColorDepthImage image = (ColorDepthImage)domainObject;

            if (targetType==MappingType.Sample) {
                mapped.add(DomainMgr.getDomainMgr().getModel().getDomainObject(image.getSampleRef()));
            }
            else {
                log.warn("Cannot map ColorDepthImage to "+targetType);
            }
        }
        else if (domainObject instanceof NeuronFragment) {
            NeuronFragment fragment = (NeuronFragment)domainObject;
            Sample sample = DomainMgr.getDomainMgr().getModel().getDomainObject(fragment.getSample());

            if (targetType==MappingType.Sample) {
                mapped.add(sample);
            }
            else if (targetType==MappingType.LSM) {
                mapped.addAll(map(sample, targetType));
            }
            else if (targetType==MappingType.AlignedNeuronFragment) {
                NeuronFragment aligned = getAligned(sample, fragment);
                if (aligned!=null) {
                    mapped.add(aligned);
                }
            }
            else if (targetType==MappingType.UnalignedNeuronFragment) {
                NeuronFragment unaligned = getUnaligned(sample, fragment);
                if (unaligned!=null) {
                    mapped.add(unaligned);
                }
            }
            else {
                log.warn("Cannot map NeuronFragment to "+targetType);
            }
            
        }
        
        return mapped;
    }
    
    private NeuronFragment getAligned(Sample sample, NeuronFragment unalignedFragment) throws Exception {

        DomainModel model = DomainMgr.getDomainMgr().getModel();
        PipelineResult unalignedResult = SampleUtils.getResultContainingNeuronSeparation(sample, unalignedFragment);
        
        if (unalignedResult instanceof HasAnatomicalArea) {
            HasAnatomicalArea unaligned = (HasAnatomicalArea)unalignedResult;
            
            SamplePipelineRun parentRun = unalignedResult.getParentRun();
    
            for (SampleAlignmentResult alignedResult : Lists.reverse(parentRun.getAlignmentResults())) {
                
                if (StringUtilsExtra.areEqual(alignedResult.getAnatomicalArea(), unaligned.getAnatomicalArea())) {
                    
                    if (alignmentSpace==null || alignedResult.getAlignmentSpace().equals(alignmentSpace)) {
                        
                        NeuronSeparation separation = alignedResult.getLatestSeparationResult();
                        if (separation != null) {
                            
                            List<DomainObject> fragments = model.getDomainObjects(separation.getFragmentsReference());
                            for(DomainObject fragmentObject : fragments) {
                                NeuronFragment alignedFragment = (NeuronFragment)fragmentObject;
                                if (unalignedFragment.getNumber().equals(alignedFragment.getNumber())) {
                                    // Found it
                                    return alignedFragment;
                                }
                            }
                        }
                    }
                }   
            }
        }
        
        return null;
    }

    private NeuronFragment getUnaligned(Sample sample, NeuronFragment alignedFragment) throws Exception {

        DomainModel model = DomainMgr.getDomainMgr().getModel();
        PipelineResult alignedResult = SampleUtils.getResultContainingNeuronSeparation(sample, alignedFragment);
        
        if (alignedResult instanceof HasAnatomicalArea) {
            HasAnatomicalArea aligned = (HasAnatomicalArea)alignedResult;
            
            SamplePipelineRun parentRun = alignedResult.getParentRun();

            for (SampleProcessingResult unalignedResult : Lists.reverse(parentRun.getSampleProcessingResults())) {

                if (StringUtilsExtra.areEqual(unalignedResult.getAnatomicalArea(), aligned.getAnatomicalArea())) {
        
                    NeuronSeparation separation = unalignedResult.getLatestSeparationResult();
                    if (separation != null) {
                        
                        List<DomainObject> fragments = model.getDomainObjects(separation.getFragmentsReference());
                        for(DomainObject fragmentObject : fragments) {
                            NeuronFragment unalignedFragment = (NeuronFragment)fragmentObject;
                            if (alignedFragment.getNumber().equals(unalignedFragment.getNumber())) {
                                // Found it
                                return unalignedFragment;
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
}
