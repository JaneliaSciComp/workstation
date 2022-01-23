package org.janelia.workstation.browser.selection;

import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.sample.PipelineResult;

/**
 * Event that is thrown when a sample result is selected in the Sample Viewer. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PipelineResultSelectionEvent {

    private final Object source;
    private final PipelineResult result;
    private final FileType fileType;
    private final boolean isUserDriven;
    
    public PipelineResultSelectionEvent(Object source, PipelineResult sampleResult, FileType fileType, boolean isUserDriven) {
        this.source = source;
        this.result = sampleResult;
        this.fileType = fileType;
        this.isUserDriven = isUserDriven;
    }

    public Object getSourceComponent() {
        return source;
    }
    
    public PipelineResult getPipelineResult() {
        return result;
    }

    public FileType getFileType() {
        return fileType;
    }

    public boolean isUserDriven() {
        return isUserDriven;
    }

    @Override
    public String toString() {
        return "PipelineResultSelectionEvent[" +
                "source=" + source +
                ", result=" + result +
                ", fileType=" + fileType +
                ", isUserDriven=" + isUserDriven +
                ']';
    }
}
