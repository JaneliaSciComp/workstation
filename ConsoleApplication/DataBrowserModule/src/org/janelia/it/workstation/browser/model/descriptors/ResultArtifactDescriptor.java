package org.janelia.it.workstation.browser.model.descriptors;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.support.SampleUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ResultArtifactDescriptor extends ArtifactDescriptor {

    private String objective;
    private String area;
    private String resultClass;
    private String resultName;
    private String alignSpace;
    private boolean aligned;

    // Empty constructor needed for JSON deserialization
    public ResultArtifactDescriptor() {
    }
    
    public ResultArtifactDescriptor(PipelineResult result) {
        this.objective = result.getParentRun().getParent().getObjective();
        if (result instanceof HasAnatomicalArea) {
            this.area = ((HasAnatomicalArea) result).getAnatomicalArea();
        }
        this.resultClass = result.getClass().getName();
        this.resultName = result.getName();
        this.aligned = result instanceof SampleAlignmentResult;
        if (aligned) {
            this.alignSpace = ((SampleAlignmentResult) result).getAlignmentSpace();
        }
    }

    public ResultArtifactDescriptor(PipelineResult result, String area) {
        this.objective = result.getParentRun().getParent().getObjective();
        this.area = area;
        this.resultClass = result.getClass().getName();
        this.resultName = result.getName();
        this.aligned = result instanceof SampleAlignmentResult;
        if (aligned) {
            this.alignSpace = ((SampleAlignmentResult) result).getAlignmentSpace();
        }
    }
    
    public String getObjective() {
        return objective;
    }
    
    public String getArea() {
        return area;
    }

    public String getResultClass() {
        return resultClass;
    }

    public String getResultName() {
        return resultName;
    }

    public String getAlignSpace() {
        return alignSpace;
    }

    public boolean isAligned() {
        return aligned;
    }

    @JsonIgnore
    public List<DomainObject> getDescribedObjects(DomainObject sourceObject) throws Exception {
        List<DomainObject> objects = new ArrayList<>();
        objects.add(sourceObject);
        return objects;
    }

    @JsonIgnore
    public List<HasFiles> getFileSources(DomainObject sourceObject) throws Exception {
        List<HasFiles> objects = new ArrayList<>();
        if (sourceObject instanceof Sample) {
            Sample sample = (Sample)sourceObject;
            objects.addAll(SampleUtils.getMatchingResults(sample, objective, area, aligned, resultName, null));
        }
        return objects;
    }

    @Override
    public String toString() {

        String realArea = area;
        if (StringUtils.isBlank(area)) {
            realArea = "Brain";
        }

        // Strip area from result name
        String areaSuffix = " ("+realArea+")";
        String realResultName = resultName.replace(areaSuffix, "");
        
        StringBuilder sb = new StringBuilder();
        sb.append(objective);
        sb.append(" ");
        sb.append(realArea);
        sb.append(" - ");
        if (alignSpace!=null) {
            sb.append(alignSpace);
        }
        else {
            sb.append(realResultName);
        }
        
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((area == null) ? 0 : area.hashCode());
        result = prime * result + ((objective == null) ? 0 : objective.hashCode());
        result = prime * result + ((resultName == null) ? 0 : resultName.hashCode());
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
        ResultArtifactDescriptor other = (ResultArtifactDescriptor) obj;
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
        if (resultName == null) {
            if (other.resultName != null)
                return false;
        }
        else if (!resultName.equals(other.resultName))
            return false;
        return true;
    }
    
}