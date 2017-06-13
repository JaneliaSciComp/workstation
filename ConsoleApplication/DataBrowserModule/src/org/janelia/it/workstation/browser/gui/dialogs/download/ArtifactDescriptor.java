package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.support.SampleUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * An artifact descriptor describes a particular type of resource that is relative to a single 
 * domain object. For example, the LSMs relative to a sample, or the object itself. 
 * 
 * It also describes a set of file types of interest for that resource. For example, the MIPs, or
 * the loss-less stack.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
abstract class ArtifactDescriptor {

    private List<FileType> selectedFileTypes = new ArrayList<>();
       
    public void setSelectedFileTypes(List<FileType> selectedFileTypes) {
        this.selectedFileTypes = selectedFileTypes;
    }

    /**
     * The file types that the user has selected.
     * @return
     */
    public List<FileType> getSelectedFileTypes() {
        return selectedFileTypes;
    }

    /**
     * The microscope objective, for filtering. May be null if not relevant.
     * @return
     */
    public abstract String getObjective();

    /**
     * The anatomical area, for filtering. May be null if not relevant.
     * @return
     */
    public abstract String getArea();

    /**
     * Is this artifact aligned, for filtering. May be null if not relevant.
     * @return
     */
    public abstract boolean isAligned();
    
    /**
     * Maps the source object to a set of objects described by the arfifact descriptor.
     * For example, this might take a Sample and return a set of LSMs.
     * @param sourceObject
     * @return
     * @throws Exception
     */
    public abstract List<DomainObject> getDescribedObjects(DomainObject sourceObject) throws Exception;
    
    /**
     * Gets all the file sources from the source object.
     * @param sourceObject
     * @return
     * @throws Exception
     */
    public abstract List<HasFiles> getFileSources(DomainObject sourceObject) throws Exception;
    
}

class SelfArtifactDescriptor extends ArtifactDescriptor {

    // Empty constructor needed for JSON deserialization
    public SelfArtifactDescriptor() {
    }

    public String getObjective() {
        return null;
    }

    public String getArea() {
        return null;
    }

    @JsonIgnore
    public boolean isAligned() {
        return false;
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
        if (sourceObject instanceof HasFiles) {
            objects.add((HasFiles)sourceObject);
        }
        return objects;
    }
    
    @Override
    public String toString() {
        return "Selected Object";
    }

    @Override
    public int hashCode() {
        return SelfArtifactDescriptor.class.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof SelfArtifactDescriptor);
    }
}

class LSMArtifactDescriptor extends ArtifactDescriptor {

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
        if (sourceObject instanceof Sample) {
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

class ResultArtifactDescriptor extends ArtifactDescriptor {

    private String objective;
    private String area;
    private String resultName;
    private boolean aligned;

    // Empty constructor needed for JSON deserialization
    public ResultArtifactDescriptor() {
    }
    
    public ResultArtifactDescriptor(String objective, String area, String resultName, boolean aligned) {
        this.objective = objective;
        this.area = area;
        this.resultName = resultName;
        this.aligned = aligned;
    }

    public String getObjective() {
        return objective;
    }
    
    public String getArea() {
        return area;
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
            objects.addAll(SampleUtils.getMatchingResults(sample, objective, area, resultName, null));
        }
        return objects;
    }

    @Override
    public String toString() {
        String suffix = " ("+area+")";
        StringBuilder sb = new StringBuilder();
        sb.append(objective);
        sb.append(" ");
        sb.append(resultName);
        if (!resultName.endsWith(suffix)) {
            sb.append(suffix);
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
