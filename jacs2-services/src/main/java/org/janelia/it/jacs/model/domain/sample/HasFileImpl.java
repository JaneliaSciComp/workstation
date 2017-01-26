package org.janelia.it.jacs.model.domain.sample;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.janelia.it.jacs.model.domain.FileReference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HasFileImpl implements HasFiles {
    // deprecated files is to keep track of the files that at one point were associated with the domain object
    private List<FileReference> deprecatedFiles = new ArrayList<>();
    private Map<FileType, String> files = new HashMap<>();

    @Override
    public Map<FileType, String> getFiles() {
        return files;
    }

    @Override
    public String getFileName(FileType fileType) {
        return files.get(fileType);
    }

    @Override
    public void setFileName(FileType fileType, String fileName) {
        String existingFile = getFileName(fileType);
        if (StringUtils.isNotBlank(existingFile) && !StringUtils.equals(existingFile, fileName)) {
            deprecatedFiles.add(new FileReference(fileType, existingFile));
        }
        files.put(fileType, fileName);
    }

    @Override
    public void removeFileName(FileType fileType) {
        String existingFile = getFileName(fileType);
        if (StringUtils.isNotBlank(existingFile)) {
            deprecatedFiles.add(new FileReference(fileType, existingFile));
        }
        files.remove(fileType);
    }

    public List<FileReference> getDeprecatedFiles() {
        return deprecatedFiles;
    }

    public void setDeprecatedFiles(List<FileReference> deprecatedFiles) {
        this.deprecatedFiles = deprecatedFiles;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, false);
    }

}
