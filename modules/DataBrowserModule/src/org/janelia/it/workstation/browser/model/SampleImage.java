package org.janelia.it.workstation.browser.model;

import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.sample.PipelineResult;
import org.janelia.model.domain.sample.Sample;

public class SampleImage {
    
    private PipelineResult result;
    private FileType imageType;
    
    public SampleImage(PipelineResult result, FileType imageType) {
        this.result = result;
        this.imageType = imageType;
    }

    public PipelineResult getResult() {
        return result;
    }
    
    public FileType getImageType() {
        return imageType;
    }
    
    public Sample getSample() {
        return result.getParentRun().getParent().getParent();
    }
}
