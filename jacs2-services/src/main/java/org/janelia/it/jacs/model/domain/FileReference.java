package org.janelia.it.jacs.model.domain;

import org.janelia.it.jacs.model.domain.enums.FileType;

public class FileReference {
    private FileType fileType;
    private String fileName;

    public FileReference() {
    }

    public FileReference(FileType fileType, String fileName) {
        this.fileType = fileType;
        this.fileName = fileName;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
