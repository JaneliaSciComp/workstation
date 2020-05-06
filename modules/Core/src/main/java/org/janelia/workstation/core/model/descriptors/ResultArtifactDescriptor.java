package org.janelia.workstation.core.model.descriptors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.SampleUtils;
import org.janelia.model.domain.interfaces.HasAnatomicalArea;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleAlignmentResult;
import org.janelia.workstation.core.util.StringUtilsExtra;

import java.util.ArrayList;
import java.util.List;

/**
 * Descriptor for a pipeline result. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ResultArtifactDescriptor extends ArtifactDescriptor {

    private String objective;
    private String area;
    private String resultClass;
    private String resultName;
    private boolean aligned;

    // Empty constructor needed for JSON deserialization
    public ResultArtifactDescriptor() {
    }
    
    public ResultArtifactDescriptor(String objective, String area, String resultClass, String resultName, boolean aligned) {
        this.objective = objective;
        this.area = area;
        this.resultClass = resultClass;
        this.resultName = resultName;
        this.aligned = aligned;
    }

    public ResultArtifactDescriptor(PipelineResult result) {
        this.objective = result.getParentRun().getParent().getObjective();
        if (result instanceof HasAnatomicalArea) {
            this.area = ((HasAnatomicalArea) result).getAnatomicalArea();
        }
        this.resultClass = result.getClass().getName();
        this.aligned = result instanceof SampleAlignmentResult;
        this.resultName = result.getName();
    }

    /**
     * For special cases where the result does not have its own area,
     * this allows you to override it. Useful for e.g. SamplePostProcessingResult
     * @param result
     * @param area
     */
    public ResultArtifactDescriptor(PipelineResult result, String area) {
        this(result);
        this.area = area;
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
            objects.addAll(SampleUtils.getMatchingResults(sample, objective, area, aligned, resultClass, resultName, null));
        }
        return objects;
    }

    @Override
    public String toString() {

        String realArea = area;

        // Strip area from result name
        String areaSuffix = " ("+realArea+")";
        String realResultName = resultName==null?null:resultName.replace(areaSuffix, "");
        
        if (realResultName==null && resultClass!=null) {
            realResultName = StringUtilsExtra.splitCamelCase(resultClass);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(objective);
        if (!StringUtils.isBlank(realArea)) {
            sb.append(" ");
            sb.append(realArea);
        }
        sb.append(" - ");
        sb.append(realResultName);
        
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((area == null) ? 0 : area.hashCode());
        result = prime * result + ((objective == null) ? 0 : objective.hashCode());
        result = prime * result + ((resultClass == null) ? 0 : resultClass.hashCode());
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
        if (resultClass == null) {
            if (other.resultClass != null)
                return false;
        }
        else if (!resultClass.equals(other.resultClass))
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