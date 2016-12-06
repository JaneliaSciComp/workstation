package org.janelia.jacs2.model.domain.sample;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.janelia.jacs2.model.domain.AbstractDomainObject;
import org.janelia.jacs2.model.domain.DataFile;
import org.janelia.jacs2.model.domain.annotations.MongoMapping;

import java.util.List;

@MongoMapping(collectionName="image", label="Image")
public class SampleImage extends AbstractDomainObject {
    public static final String ENTITY_NAME = "Image";
    private String filepath;
    private String imageSize;
    private String opticalResolution;
    private String objective;
    private Integer numChannels;
    private List<DataFile> dataFiles;

    @JsonIgnore
    @Override
    public String getEntityName() {
        return ENTITY_NAME;
    }

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

    public List<DataFile> getDataFiles() {
        return dataFiles;
    }

    public void setDataFiles(List<DataFile> dataFiles) {
        this.dataFiles = dataFiles;
    }
}
