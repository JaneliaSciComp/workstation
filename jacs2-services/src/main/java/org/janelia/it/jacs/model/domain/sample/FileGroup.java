package org.janelia.it.jacs.model.domain.sample;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private HasFileImpl filesImpl = new HasFileImpl();

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
        return filesImpl.getFiles();
    }

    @Override
    public String getFileName(FileType fileType) {
        return filesImpl.getFileName(fileType);
    }

    @Override
    public void setFileName(FileType fileType, String fileName) {
        filesImpl.setFileName(fileType, fileName);
    }

    @Override
    public void removeFileName(FileType fileType) {
        filesImpl.removeFileName(fileType);
    }

}
