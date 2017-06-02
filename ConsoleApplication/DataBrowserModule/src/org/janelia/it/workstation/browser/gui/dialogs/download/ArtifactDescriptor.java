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
import org.janelia.it.jacs.model.domain.support.ResultDescriptor;
import org.janelia.it.jacs.model.domain.support.SampleUtils;
import org.janelia.it.workstation.browser.api.DomainMgr;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * An artifact descriptor describes a particular type of resource that is relative to a single 
 * domain object. For example, the LSMs relative to a sample, or the object itself. 
 * 
 * It also describes a set of file types of interest for that resource. For example, the MIPs, or
 * the lossless stack.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
abstract class ArtifactDescriptor {

    private List<FileType> fileTypes = new ArrayList<>();
        
    public void setFileTypes(List<FileType> fileTypes) {
        this.fileTypes = fileTypes;
    }

    public List<FileType> getFileTypes() {
        return fileTypes;
    }

    public abstract List<DomainObject> getDescribedObjects(DomainObject sourceObject) throws Exception;
    
    public abstract List<Object> getFileSources(DomainObject sourceObject) throws Exception;
}

class SelfArtifactDescriptor extends ArtifactDescriptor {

    public SelfArtifactDescriptor() {
    }
    
    public List<DomainObject> getDescribedObjects(DomainObject sourceObject) throws Exception {
        List<DomainObject> objects = new ArrayList<>();
        objects.add(sourceObject);
        return objects;
    }
    
    public List<Object> getFileSources(DomainObject sourceObject) throws Exception {
        List<Object> objects = new ArrayList<>();
        objects.add(sourceObject);
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

    public LSMArtifactDescriptor() {
    }
    
    public LSMArtifactDescriptor(String objective) {
        this.objective = objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getObjective() {
        return objective;
    }

    public List<DomainObject> getDescribedObjects(DomainObject sourceObject) throws Exception {
        List<DomainObject> objects = new ArrayList<>();
        if (sourceObject instanceof Sample) {
            Sample sample = (Sample)sourceObject;
            List<Reference> refs = new ArrayList<>();
            ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
            if (objectiveSample!=null) {
                for(SampleTile tile : objectiveSample.getTiles()) {
                    refs.addAll(tile.getLsmReferences());
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
    
    public List<Object> getFileSources(DomainObject sourceObject) throws Exception {
        List<Object> objects = new ArrayList<>();
        objects.addAll(getDescribedObjects(sourceObject));
        return objects;
    }
    
    @Override
    public String toString() {
        return objective + " Original LSM Images";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
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
    
    private ResultDescriptor resultDescriptor;

    public ResultArtifactDescriptor() {
    }
    
    public ResultArtifactDescriptor(ResultDescriptor resultDescriptor) {
        this.resultDescriptor = resultDescriptor;
    }

    public void setResultDescriptor(ResultDescriptor resultDescriptor) {
        this.resultDescriptor = resultDescriptor;
    }

    public ResultDescriptor getResultDescriptor() {
        return resultDescriptor;
    }

    public List<DomainObject> getDescribedObjects(DomainObject sourceObject) throws Exception {
        List<DomainObject> objects = new ArrayList<>();
        objects.add(sourceObject);
        return objects;
    }
    
    public List<Object> getFileSources(DomainObject sourceObject) throws Exception {
        List<Object> objects = new ArrayList<>();
        if (sourceObject instanceof Sample) {
            Sample sample = (Sample)sourceObject;
            HasFiles result = SampleUtils.getResult(sample, resultDescriptor);
            if (result!=null) {
                objects.add(result);
            }
        }
        return objects;
    }
    
    @Override
    public String toString() {
        return resultDescriptor.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((resultDescriptor == null) ? 0 : resultDescriptor.hashCode());
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
        if (resultDescriptor == null) {
            if (other.resultDescriptor != null)
                return false;
        }
        else if (!resultDescriptor.equals(other.resultDescriptor))
            return false;
        return true;
    }
}
