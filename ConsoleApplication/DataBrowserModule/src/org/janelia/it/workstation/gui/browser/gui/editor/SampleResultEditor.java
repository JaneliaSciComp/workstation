package org.janelia.it.workstation.gui.browser.gui.editor;

import java.util.concurrent.Callable;

import org.janelia.it.jacs.model.domain.sample.PipelineResult;

/**
 * An editor for a single sample result object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SampleResultEditor extends Editor {
    
    public void loadSampleResult(PipelineResult pipelineResult, boolean isUserDriven, Callable<Void> success);
}
