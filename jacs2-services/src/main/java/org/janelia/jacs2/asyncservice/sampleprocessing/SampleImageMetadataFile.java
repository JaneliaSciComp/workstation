package org.janelia.jacs2.asyncservice.sampleprocessing;

public class SampleImageMetadataFile {
    private SampleImageFile sampleImageFile;
    private String metadataFilePath;

    public SampleImageFile getSampleImageFile() {
        return sampleImageFile;
    }

    public void setSampleImageFile(SampleImageFile sampleImageFile) {
        this.sampleImageFile = sampleImageFile;
    }

    public String getMetadataFilePath() {
        return metadataFilePath;
    }

    public void setMetadataFilePath(String metadataFilePath) {
        this.metadataFilePath = metadataFilePath;
    }

}
