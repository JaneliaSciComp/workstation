package org.janelia.it.jacs.model.domain.sample;

import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.domain.FileReference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasRelativeFiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A group of files with a common parent path.
 */
public class FileGroup implements HasRelativeFiles {

    private String key;
    private String filepath;
    private List<FileReference> deprecatedFiles = new ArrayList<>();
    private Map<FileType, String> files = new HashMap<>();

    public FileGroup() {
    }

    public FileGroup(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public Map<FileType, String> getFiles() {
        return files;
    }

    public void setFiles(Map<FileType, String> files) {
        if (files==null) throw new IllegalArgumentException("Property cannot be null");
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

}
