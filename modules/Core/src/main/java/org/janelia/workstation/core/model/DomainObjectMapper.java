package org.janelia.workstation.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.model.access.domain.SampleUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleAlignmentResult;
import org.janelia.model.domain.sample.SamplePipelineRun;
import org.janelia.model.domain.sample.SampleProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

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
            types.addAll(getMappableTypes(domainObject));
        }
        
        return types;
    }
    
    /**
     * Returns a list of the types which the given domain object can be mapped to using the map functions.
     * @param domainObject
     * @return
     */
    private Collection<MappingType> getMappableTypes(DomainObject domainObject) {

        List<MappingType> types = new ArrayList<>();
        
        if (domainObject instanceof Sample) {
            types.add(MappingType.LSM);
        }
        else if (domainObject instanceof LSMImage) {
            types.add(MappingType.Sample);
        }

        else if (domainObject instanceof NeuronFragment) {
            types.add(MappingType.Sample);
            types.add(MappingType.LSM);
            types.add(MappingType.AlignedNeuronFragment);
            types.add(MappingType.UnalignedNeuronFragment);
        }

        return types;
    }
    
    /**
     * Map the given objects to the given class, and return a list of joined objects.
     * @param domainObjects
     * @param targetClass
     * @return
     * @throws Exception
     */
    public <T extends DomainObject> List<T> map(MappingType targetType, Class<T> outputClass) throws Exception {

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
     * @param targetClass
     * @return
     * @throws Exception
     */
    private List<DomainObject> map(DomainObject domainObject, MappingType targetType) throws Exception {

        List<DomainObject> mapped = new ArrayList<>();
        
        if (domainObject instanceof Sample) {
            Sample sample = (Sample)domainObject;
            
            if (targetType==MappingType.LSM) {
                List<LSMImage> lsms = DomainMgr.getDomainMgr().getModel().getDomainObjectsAs(LSMImage.class, sample.getLsmReferences());
                mapped.addAll(lsms);
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
                Sample sample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(lsm.getSample());
                mapped.add(sample);
            }
            else if (targetType==MappingType.LSM) {
                mapped.add(lsm);
            }
            else {
                log.warn("Cannot map LSMImage to "+targetType);
            }
            
        }
        else if (domainObject instanceof NeuronFragment) {
            NeuronFragment fragment = (NeuronFragment)domainObject;
            Sample sample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(fragment.getSample());

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
                
                if (StringUtils.areEqual(alignedResult.getAnatomicalArea(), unaligned.getAnatomicalArea())) {
                    
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

                if (StringUtils.areEqual(unalignedResult.getAnatomicalArea(), aligned.getAnatomicalArea())) {
        
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
