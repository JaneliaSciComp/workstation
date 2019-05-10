package org.janelia.workstation.core.model.descriptors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.SampleUtils;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.Sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LatestDescriptor extends ArtifactDescriptor {
    
    private boolean all;
    private boolean aligned;

    public LatestDescriptor() {
    }
    
    public LatestDescriptor(boolean all, boolean aligned) {
        this.all = all;
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

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public void setAligned(boolean aligned) {
        this.aligned = aligned;
    }
    
    @Override
    public boolean isAligned() {
        return aligned;
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
            objects.addAll(SampleUtils.getMatchingResults(sample, null, null, aligned, null, null, null));
        }
        return objects;
    }

    @Override
    public String toString() {
        if (all) {
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
        result = prime * result + (aligned ? 1231 : 1237);
        result = prime * result + (all ? 1231 : 1237);
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
        if (aligned != other.aligned)
            return false;
        if (all != other.all)
            return false;
        return true;
    }
}
