package org.janelia.it.workstation.browser.events.selection;

import org.janelia.model.domain.sample.PipelineError;

/**
 * Event that is thrown when a sample error is selected in the Sample Viewer. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PipelineErrorSelectionEvent {

    private final Object source;
    private final PipelineError error;
    private final boolean isUserDriven;

    public PipelineErrorSelectionEvent(Object source, PipelineError error, boolean isUserDriven) {
        this.source = source;
        this.error = error;
        this.isUserDriven = isUserDriven;
    }
    
    public Object getSource() {
        return source;
    }
    
    public PipelineError getPipelineError() {
        return error;
    }
    
    public boolean isUserDriven() {
        return isUserDriven;
    }
    
    @Override
    public String toString() {
        return "PipelineErrorSelectionEvent[" + "source=" + source + ", error=" + error + ']';
    }
}
