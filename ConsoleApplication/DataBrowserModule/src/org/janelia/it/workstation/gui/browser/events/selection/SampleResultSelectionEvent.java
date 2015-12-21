package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.workstation.gui.browser.model.SampleResult;

/**
 * Event that is thrown when a sample result is selected in the Sample Viewer. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleResultSelectionEvent {

    private final Object source;
    private final SampleResult sampleResult;
    private final boolean isUserDriven;
    
    public SampleResultSelectionEvent(Object source, SampleResult sampleResult, boolean isUserDriven) {
        this.source = source;
        this.sampleResult = sampleResult;
        this.isUserDriven = isUserDriven;
    }

    public Object getSource() {
        return source;
    }
    
    public SampleResult getSampleResult() {
        return sampleResult;
    }
    
    public boolean isUserDriven() {
        return isUserDriven;
    }

    @Override
    public String toString() {
        return "PipelineResultSelectionEvent[" + "source=" + source + ", sampleResult=" + sampleResult + ']';
    }
}
