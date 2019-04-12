package org.janelia.workstation.core.model.descriptors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.NeuronSeparation;
import org.janelia.model.domain.sample.ObjectiveSample;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleAlignmentResult;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class NeuronFragmentDescriptor extends ArtifactDescriptor {

    private String objective;
    private String area;
    private boolean aligned;

    // Empty constructor needed for JSON deserialization
    public NeuronFragmentDescriptor() {
    }
    
    public NeuronFragmentDescriptor(String objective, String area, boolean aligned) {
        this.objective = objective;
        this.area = area;
        this.aligned = aligned;
    }

    public String getObjective() {
        return objective;
    }

    public String getArea() {
        return area;
    }

    public boolean isAligned() {
        return aligned;
    }

    @JsonIgnore
    public List<DomainObject> getDescribedObjects(DomainObject sourceObject) throws Exception {
        List<DomainObject> objects = new ArrayList<>();
        if (sourceObject instanceof NeuronFragment) {
            NeuronFragment neuron = (NeuronFragment)sourceObject;

            Sample sample = DomainMgr.getDomainMgr().getModel().getDomainObject(Sample.class, neuron.getSample().getTargetId());
            if (sample!=null) {
                List<NeuronSeparation> results = sample.getResultsById(NeuronSeparation.class, neuron.getSeparationId());
                if (!results.isEmpty()) {
                    NeuronSeparation separation = results.get(0);
                                                            
                    PipelineResult parentResult = separation.getParentResult();
                    boolean alignedResult = (parentResult instanceof SampleAlignmentResult);
                    if (alignedResult==aligned) {

                        ObjectiveSample objectiveSample = parentResult.getParentRun().getParent();
                        if (!objective.equals(objectiveSample.getObjective())) {
                            return Collections.emptyList();
                        }
                        
                        if (parentResult instanceof HasAnatomicalArea) {
                            HasAnatomicalArea hasAA = (HasAnatomicalArea)parentResult;
                            if (!area.equals(hasAA.getAnatomicalArea())) {
                                return Collections.emptyList();
                            }
                            return Arrays.asList(sourceObject);
                        }
                    }
                    
                }
            }
        }
        return objects;
    }

    @JsonIgnore
    public List<HasFiles> getFileSources(DomainObject sourceObject) throws Exception {
        List<HasFiles> objects = new ArrayList<>();
        for(DomainObject describedObject : getDescribedObjects(sourceObject))
        if (describedObject instanceof HasFiles) {
            objects.add((HasFiles)describedObject);
        }
        return objects;
    }
    
    @Override
    public String toString() {
        return objective + " "+(aligned?"Aligned":"Unaligned")+" Neuron Fragments ("+area+")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (aligned ? 1231 : 1237);
        result = prime * result + ((area == null) ? 0 : area.hashCode());
        result = prime * result + ((objective == null) ? 0 : objective.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NeuronFragmentDescriptor other = (NeuronFragmentDescriptor) obj;
        if (aligned != other.aligned)
            return false;
        if (area == null) {
            if (other.area != null)
                return false;
        }
        else if (!area.equals(other.area))
            return false;
        if (objective == null) {
            if (other.objective != null)
                return false;
        }
        else if (!objective.equals(other.objective))
            return false;
        return true;
    }
}