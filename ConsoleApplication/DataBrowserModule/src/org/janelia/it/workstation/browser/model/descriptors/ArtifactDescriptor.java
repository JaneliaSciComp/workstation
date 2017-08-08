package org.janelia.it.workstation.browser.model.descriptors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.SampleUtils;

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
 
    public static final ArtifactDescriptor LATEST = new ArtifactDescriptor() {

        @Override
        public String getObjective() {
            return null;
        }

        @Override
        public String getArea() {
            return null;
        }

        @Override
        public boolean isAligned() {
            return false;
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
                objects.addAll(SampleUtils.getMatchingResults(sample, null, null, null, null, null));
            }
            return objects;
        }

        @Override
        public String toString() {
            return "Latest";
        }
    };

    public static final ArtifactDescriptor LATEST_ALIGNED = new ArtifactDescriptor() {

        @Override
        public String getObjective() {
            return null;
        }

        @Override
        public String getArea() {
            return null;
        }

        @Override
        public boolean isAligned() {
            return true;
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
                objects.addAll(SampleUtils.getMatchingResults(sample, null, null, true, null, null));
            }
            return objects;
        }

        @Override
        public String toString() {
            return "Latest aligned";
        }
    };

    public static final ArtifactDescriptor LATEST_UNALIGNED = new ArtifactDescriptor() {

        @Override
        public String getObjective() {
            return null;
        }

        @Override
        public String getArea() {
            return null;
        }

        @Override
        public boolean isAligned() {
            return false;
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
                objects.addAll(SampleUtils.getMatchingResults(sample, null, null, false, null, null));
            }
            return objects;
        }
        
        @Override
        public String toString() {
            return "Latest unaligned";
        }
    };
}
