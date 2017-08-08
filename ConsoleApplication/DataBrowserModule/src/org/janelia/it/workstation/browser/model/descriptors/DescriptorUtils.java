package org.janelia.it.workstation.browser.model.descriptors;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SamplePostProcessingResult;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;

/**
 * Utility methods for dealing with descriptors. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DescriptorUtils {

    private static final Logger log = LoggerFactory.getLogger(DescriptorUtils.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Creates a set of artifact descriptors which describe the contents of the given collection. Note that for certain types
     * (namely, NeuronFragments) this method does lookups in the DomainModel, so it may be slow.
     * @param domainObjects
     * @return
     */
    public static Multiset<ArtifactDescriptor> getArtifactCounts(Collection<? extends DomainObject> domainObjects) {

        Multiset<ArtifactDescriptor> countedArtifacts = LinkedHashMultiset.create();
            
        for(DomainObject domainObject : domainObjects) {
            log.trace("Inspecting object: {}", domainObject);
            if (domainObject instanceof LSMImage) {
                LSMImage image = (LSMImage)domainObject;
                LSMArtifactDescriptor desc = new LSMArtifactDescriptor(image.getObjective(), image.getAnatomicalArea());
                countedArtifacts.add(desc);
                log.trace("  Adding self LSM descriptor for objective: {}", desc);
            }
            else if (domainObject instanceof NeuronFragment) {
                NeuronFragment neuron = (NeuronFragment)domainObject;
                try {
                    Sample sample = DomainMgr.getDomainMgr().getModel().getDomainObject(Sample.class, neuron.getSample().getTargetId());
                    if (sample!=null) {
                        List<NeuronSeparation> results = sample.getResultsById(NeuronSeparation.class, neuron.getSeparationId());
                        if (!results.isEmpty()) {
                            NeuronSeparation separation = results.get(0);
                            PipelineResult parentResult = separation.getParentResult();
                            if (parentResult instanceof HasAnatomicalArea) {
                                HasAnatomicalArea hasAA = (HasAnatomicalArea)parentResult;
                                boolean aligned = (parentResult instanceof SampleAlignmentResult);
                                ObjectiveSample objectiveSample = parentResult.getParentRun().getParent();
                                NeuronFragmentDescriptor desc = new NeuronFragmentDescriptor(objectiveSample.getObjective(), hasAA.getAnatomicalArea(), aligned);
                                countedArtifacts.add(desc);
                                log.trace("  Adding neuron fragment self descriptor: {}", desc);
                            }
                        }
                    }
                }
                catch (Exception e) {
                    FrameworkImplProvider.handleException(e);
                }
            }
            else if (domainObject instanceof HasFiles) {
                log.trace("  Adding self descriptor");
                countedArtifacts.add(new SelfArtifactDescriptor());
            }
            else if (domainObject instanceof Sample) {
                Sample sample = (Sample)domainObject;
                log.trace("  Inspecting sample: {}", sample.getName());
                
                for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
                    log.trace("    Inspecting objective: {}", objectiveSample.getObjective());
                    
                    for (SampleTile tile : objectiveSample.getTiles()) {
                        log.trace("      Inspecting tile: {}", tile.getName());
                        
                        for (Reference reference : tile.getLsmReferences()) {
                            log.trace("         Adding LSM descriptor for objective: {}", objectiveSample.getObjective());
                            countedArtifacts.add(new LSMArtifactDescriptor(objectiveSample.getObjective(), tile.getAnatomicalArea()));
                        }
                    }
                    SamplePipelineRun run = objectiveSample.getLatestSuccessfulRun();
                    if (run==null || run.getResults()==null) {
                        run = objectiveSample.getLatestRun();
                    }
                    if (run!=null) {
                        for(PipelineResult result : run.getResults()) {
                            log.trace("  Inspecting pipeline result: {}", result.getName());
                            if (result instanceof SamplePostProcessingResult) {
                                // Add a descriptor for every anatomical area in the sample

                                Set<String> areas = new TreeSet<>();
                                for (SampleTile sampleTile : objectiveSample.getTiles()) {
                                    areas.add(sampleTile.getAnatomicalArea());    
                                }
                                
                                for(String area : areas) {
                                    ResultArtifactDescriptor rad = new ResultArtifactDescriptor(result, area);
                                    log.trace("    Adding result artifact descriptor: {}", rad);
                                    countedArtifacts.add(rad);
                                }
                            }
                            else if (result instanceof HasAnatomicalArea){
                                ResultArtifactDescriptor rad = new ResultArtifactDescriptor(result);
                                log.trace("    Adding result artifact descriptor: {}", rad);
                                countedArtifacts.add(rad);
                            }
                            else {
                                log.trace("Cannot handle result '"+result.getName()+"' of type "+result.getClass().getSimpleName());
                            }
                        }
                    }
                }
            }
        }
        
        return countedArtifacts;
    }

    public static HasFiles getResult(DomainObject domainObject, ArtifactDescriptor descriptor) {
        try {
            List<HasFiles> sources = descriptor.getFileSources(domainObject);
            if (sources==null || sources.isEmpty()) {
                return null;
            }
            return sources.get(0);
        }
        catch (Exception e) {
            log.error("Error getting descriptor {} for object {}", descriptor, domainObject, e);
            return null;
        }
    }

    public static String serialize(ArtifactDescriptor descriptor) throws Exception {
        return mapper.writeValueAsString(descriptor);
    }

    public static ArtifactDescriptor deserialize(String artifactDescriptorString) throws Exception {
        return mapper.readValue(artifactDescriptorString, ArtifactDescriptor.class);
    }
    
}
