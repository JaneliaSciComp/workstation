package org.janelia.workstation.core.model.descriptors;

import java.util.ArrayList;
import java.util.List;

import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.interfaces.HasFiles;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * An artifact descriptor describes a particular type of resource that is relative to a single 
 * domain object. For example, the LSMs relative to a Sample, or the post-processing results
 * for a Sample. 
 * 
 * It also describes a set of file types of interest for that resource. For example, the MIPs, or
 * the loss-less stack.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ArtifactDescriptor {
    
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
     * Gets all the file sources from the source object. For example, this might return 
     * a SampleAlignmentResult from a Sample.
     * @param sourceObject
     * @return
     * @throws Exception
     */
    public abstract List<HasFiles> getFileSources(DomainObject sourceObject) throws Exception;
 
    public static final ArtifactDescriptor LATEST = new LatestDescriptor(true, false);
    public static final ArtifactDescriptor LATEST_ALIGNED = new LatestDescriptor(false, true);
    public static final ArtifactDescriptor LATEST_UNALIGNED = new LatestDescriptor(false, false);
    
}
