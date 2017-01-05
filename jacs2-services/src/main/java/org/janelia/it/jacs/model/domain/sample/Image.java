package org.janelia.it.jacs.model.domain.sample;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasRelativeFiles;
import org.janelia.it.jacs.model.domain.support.MongoMapping;

import java.util.HashMap;
import java.util.Map;

@MongoMapping(collectionName="image", label="Image")
public class Image extends AbstractDomainObject implements HasRelativeFiles {
    private String filepath;
    private String imageSize;
    private String opticalResolution;
    private String objective;
    private Integer numChannels;
    // files are in fact alternate representations of this image instance
    private Map<FileType, String> files = new HashMap<>();

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getImageSize() {
        return imageSize;
    }

    public void setImageSize(String imageSize) {
        this.imageSize = imageSize;
    }

    public String getOpticalResolution() {
        return opticalResolution;
    }

    public void setOpticalResolution(String opticalResolution) {
        this.opticalResolution = opticalResolution;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public Integer getNumChannels() {
        return numChannels;
    }

    public void setNumChannels(Integer numChannels) {
        this.numChannels = numChannels;
    }

    public Map<FileType, String> getFiles() {
        return files;
    }

    public void setFiles(Map<FileType, String> files) {
        this.files = files;
    }

    public void addFile(FileType fileType, String fileName) {
        this.files.put(fileType, fileName);
    }
}
