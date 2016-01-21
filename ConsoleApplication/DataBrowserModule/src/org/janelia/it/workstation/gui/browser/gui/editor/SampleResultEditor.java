package org.janelia.it.workstation.gui.browser.gui.editor;

import java.util.concurrent.Callable;

import org.janelia.it.workstation.gui.browser.model.SampleResult;

/**
 * An editor for a single sample result object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SampleResultEditor {
    
    public void loadSampleResult(SampleResult sampleResult, boolean isUserDriven, Callable<Void> success);
    
    public String getName();
    
    public Object getEventBusListener();
    
}
