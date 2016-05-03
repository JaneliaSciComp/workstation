package org.janelia.it.workstation.gui.browser.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.interfaces.HasFileGroups;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for extracting information from the domain model for view purposes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainModelViewUtils {

    private static final Logger log = LoggerFactory.getLogger(DomainModelViewUtils.class);
    
    private final static DateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd hh:mma");
    
    public static String getLabel(PipelineResult result) {
        return result.getParentRun().getParent().getObjective() + " " + result.getName();
    }
    
    public static String getDateString(Date date) {
        return dateFormatter.format(date).toLowerCase();
    }

    public static HasFiles getResult(Sample sample, ResultDescriptor result) {
        
        log.debug("Getting result '{}' from {}",result,sample.getName());
        log.debug("  Result name prefix: {}",result.getResultNamePrefix());
        log.debug("  Group name: {}",result.getGroupName());

        HasFiles chosenResult = null;

        if (DomainConstants.PREFERENCE_VALUE_LATEST.equals(result.getResultKey())) {
            List<ObjectiveSample> objectiveSamples = sample.getObjectiveSamples();
            if (objectiveSamples==null || objectiveSamples.isEmpty()) return null;
            int i = objectiveSamples.size()-1;
            while (chosenResult==null && i>=0) {
                log.debug("Testing objective with index "+i);
                ObjectiveSample objSample = objectiveSamples.get(i--);
                if (objSample!=null) {
                    log.debug("Testing objective: "+objSample.getObjective());
                    List<SamplePipelineRun> runs = new ArrayList<>(objSample.getPipelineRuns());
                    Collections.reverse(runs);
                    for(SamplePipelineRun run : runs) {
                        log.debug("Testing run: "+run.getName());
                        chosenResult = run.getLatestResult();
                    }

                }
            }

            log.debug("Got result: "+chosenResult);

            if (chosenResult instanceof HasFileGroups) {
                HasFileGroups hasGroups = (HasFileGroups)chosenResult;
                // Pick the first group, since there is no way to tell which is latest
                for(String groupKey : hasGroups.getGroupKeys()) {
                    chosenResult = hasGroups.getGroup(groupKey);
                    break;
                }
            }
        }
        else {
            ObjectiveSample objSample = sample.getObjectiveSample(result.getObjective());
            if (objSample==null) return null;
            SamplePipelineRun run = objSample.getLatestSuccessfulRun();
            if (run==null || run.getResults()==null) return null;
            for(PipelineResult pipelineResult : run.getResults()) {
                if (pipelineResult instanceof HasFileGroups) {
                    HasFileGroups hasGroups = (HasFileGroups)pipelineResult;
                    for(String groupKey : hasGroups.getGroupKeys()) {
                        if (pipelineResult.getName().equals(result.getResultNamePrefix()) && groupKey.equals(result.getGroupName())) {
                            chosenResult = hasGroups.getGroup(groupKey);
                            break;
                        }
                    }
                }
                else {
                    if (pipelineResult.getName().equals(result.getResultName())) {
                        chosenResult = pipelineResult;
                        break;
                    }
                }
            }
        }
        
        return chosenResult;
    }

    public static ResultDescriptor getLatestResultDescriptor(Sample sample) {
        
        List<ObjectiveSample> objectiveSamples = sample.getObjectiveSamples();
        if (objectiveSamples==null || objectiveSamples.isEmpty()) return null;
    
        ObjectiveSample objSample = objectiveSamples.get(objectiveSamples.size() - 1);
        if (objSample==null) return null;
        SamplePipelineRun run = objSample.getLatestSuccessfulRun();
        if (run==null) return null;
        PipelineResult chosenResult = run.getLatestResult();

        if (chosenResult instanceof HasFileGroups) {
            HasFileGroups hasGroups = (HasFileGroups)chosenResult;
            // Pick the first group, since there is no way to tell which is latest
            for(String groupKey : hasGroups.getGroupKeys()) {
                return new ResultDescriptor(objSample.getObjective(), chosenResult.getName(), groupKey);
            }
        }

        String name = (chosenResult==null)?null:chosenResult.getName();
        return new ResultDescriptor(objSample.getObjective(), name, null);

    }
    
    public static NeuronSeparation getNeuronSeparation(Sample sample, NeuronFragment neuronFragment) {
        if (neuronFragment==null) return null;
        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            for(SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
                if (run!=null && run.getResults()!=null) {
                    for(PipelineResult result : run.getResults()) {
                        if (result!=null && result.getResults()!=null) {
                            for(PipelineResult secondaryResult : result.getResults()) {
                                if (secondaryResult!=null && secondaryResult instanceof NeuronSeparation) {
                                    NeuronSeparation separation = (NeuronSeparation)secondaryResult;
                                    if (separation.getFragmentsReference().getReferenceId().equals(neuronFragment.getSeparationId())) {
                                        return separation;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
