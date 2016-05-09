package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.sample.PipelineResult;

/**
 * Event that is thrown when a sample result is selected in the Sample Viewer. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PipelineResultSelectionEvent {

    private final Object source;
    private final PipelineResult result;
    private final boolean isUserDriven;
    
    public PipelineResultSelectionEvent(Object source, PipelineResult sampleResult, boolean isUserDriven) {
        this.source = source;
        this.result = sampleResult;
        this.isUserDriven = isUserDriven;
    }

    public Object getSource() {
        return source;
    }
    
    public PipelineResult getPipelineResult() {
        return result;
    }
    
    public boolean isUserDriven() {
        return isUserDriven;
    }

    @Override
    public String toString() {
        return "PipelineResultSelectionEvent[" + "source=" + source + ", result=" + result + ']';
    }
}
