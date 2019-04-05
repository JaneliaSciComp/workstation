package org.janelia.it.workstation.browser.model.descriptors;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.interfaces.HasFiles;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.ObjectiveSample;
import org.janelia.model.domain.sample.Sample;
import org.janelia.model.domain.sample.SampleTile;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LSMArtifactDescriptor extends ArtifactDescriptor {

    private String objective;
    private String area;

    // Empty constructor needed for JSON deserialization
    public LSMArtifactDescriptor() {
    }
    
    public LSMArtifactDescriptor(String objective, String area) {
        this.objective = objective;
        this.area = area;
    }

    public String getObjective() {
        return objective;
    }

    public String getArea() {
        return area;
    }

    @JsonIgnore
    public boolean isAligned() {
        return false;
    }

    @JsonIgnore
    public List<DomainObject> getDescribedObjects(DomainObject sourceObject) throws Exception {
        List<DomainObject> objects = new ArrayList<>();
        if (sourceObject instanceof LSMImage) {
            LSMImage lsm = (LSMImage)sourceObject;
            if (objective.equals(lsm.getObjective()) && area.equals(lsm.getAnatomicalArea())) {
                objects.add(sourceObject);
            }
        }
        else if (sourceObject instanceof Sample) {
            Sample sample = (Sample)sourceObject;
            List<Reference> refs = new ArrayList<>();
            ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
            if (objectiveSample!=null) {
                for(SampleTile tile : objectiveSample.getTiles()) {
                    if (StringUtils.areEqual(tile.getAnatomicalArea(), area)) {
                        refs.addAll(tile.getLsmReferences());
                    }
                }
            }
            if (!refs.isEmpty()) {
                for (LSMImage lsm : DomainMgr.getDomainMgr().getModel().getDomainObjectsAs(LSMImage.class, refs)) {
                    objects.add(lsm);
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
        return objective + " Original LSM Images ("+area+")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
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
        LSMArtifactDescriptor other = (LSMArtifactDescriptor) obj;
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