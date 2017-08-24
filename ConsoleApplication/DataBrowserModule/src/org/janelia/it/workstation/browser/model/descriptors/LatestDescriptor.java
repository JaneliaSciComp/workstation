package org.janelia.it.workstation.browser.model.descriptors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.SampleUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LatestDescriptor extends ArtifactDescriptor {
    
    private Boolean aligned;

    public LatestDescriptor() {
    }
    
    public LatestDescriptor(Boolean aligned) {
        this.aligned = aligned;
    }
    
    public void setAligned(Boolean aligned) {
        this.aligned = aligned;
    }

    @JsonIgnore
    @Override
    public String getObjective() {
        return null;
    }

    @JsonIgnore
    @Override
    public String getArea() {
        return null;
    }

    @Override
    public boolean isAligned() {
        return aligned!=null && aligned;
    }

    @Override
    public List<DomainObject> getDescribedObjects(DomainObject sourceObject) throws Exception {
        return Arrays.asList(sourceObject);
    }

    @Override
    public List<HasFiles> getFileSources(DomainObject sourceObject) throws Exception {
        List<HasFiles> objects = new ArrayList<>();
        if (sourceObject instanceof Sample) {
            Sample sample = (Sample)sourceObject;
            objects.addAll(SampleUtils.getMatchingResults(sample, null, null, aligned, null, null));
        }
        return objects;
    }

    @Override
    public String toString() {
        if (aligned==null) {
            return "Latest";
        }
        else if (aligned) {
            return "Latest aligned";    
        }
        else {
            return "Latest unaligned";
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aligned == null) ? 0 : aligned.hashCode());
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
        LatestDescriptor other = (LatestDescriptor) obj;
        if (aligned == null) {
            if (other.aligned != null)
                return false;
        }
        else if (!aligned.equals(other.aligned))
            return false;
        return true;
    }
}
