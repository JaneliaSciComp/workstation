package org.janelia.it.jacs.model.domain.sample;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.FileReference;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A sample tile consists of a set of LSMs with the same objective,
 * and in the same anatomical area.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleTile implements HasFiles {

    private String name;
    private String anatomicalArea;
    private List<Reference> lsmReferences;
    private Map<FileType, String> files = new HashMap<>();
    private List<FileReference> deprecatedFiles = new ArrayList<>();
    @JsonIgnore
    private transient ObjectiveSample parent;
    private Boolean blockAreaImageCreation = null;
    private Boolean blockAnatomicalAreaCreation = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnatomicalArea() {
        return anatomicalArea;
    }

    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
    }

    public List<Reference> getLsmReferences() {
        return lsmReferences;
    }

    public void setLsmReferences(List<Reference> lsmReferences) {
        this.lsmReferences = lsmReferences;
    }

    public void addLsmReference(Reference ref) {
        if (lsmReferences == null) {
            lsmReferences = new ArrayList<>();
        }
        lsmReferences.add(ref);
    }

    public Reference getLsmReferenceAt(int index) {
        return lsmReferences != null && index < lsmReferences.size() ? lsmReferences.get(index) : null;
    }

    @Override
    public Map<FileType, String> getFiles() {
        return files;
    }

    public void setFiles(Map<FileType, String> files) {
        this.files = files;
    }

    @Override
    public void addFileType(FileType fileType, String fileName) {
        String existingFile = files.get(fileType);
        if (StringUtils.isNotBlank(existingFile) && !StringUtils.equals(existingFile, fileName)) {
            deprecatedFiles.add(new FileReference(fileType, fileName));
        }
        files.put(fileType, fileName);
    }

    @Override
    public String getFileName(FileType fileType) {
        return files.get(fileType);
    }

    @Override
    public void removeFileType(FileType fileType) {
        String existingFile = files.get(fileType);
        if (StringUtils.isNotBlank(existingFile)) {
            deprecatedFiles.add(new FileReference(fileType, existingFile));
        }
        files.remove(fileType);
    }

    public ObjectiveSample getParent() {
        return parent;
    }

    public void setParent(ObjectiveSample parent) {
        this.parent = parent;
    }

    public Boolean getBlockAreaImageCreation() {
        return blockAreaImageCreation;
    }

    public void setBlockAreaImageCreation(Boolean blockAreaImageCreation) {
        this.blockAreaImageCreation = blockAreaImageCreation;
    }

    public Boolean getBlockAnatomicalAreaCreation() {
        return blockAnatomicalAreaCreation;
    }

    public void setBlockAnatomicalAreaCreation(Boolean blockAnatomicalAreaCreation) {
        this.blockAnatomicalAreaCreation = blockAnatomicalAreaCreation;
    }

}
