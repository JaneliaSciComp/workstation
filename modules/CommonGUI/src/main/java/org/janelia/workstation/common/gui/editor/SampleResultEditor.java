package org.janelia.workstation.common.gui.editor;

import java.util.concurrent.Callable;

import org.janelia.workstation.core.events.selection.SelectionModel;
import org.janelia.model.domain.sample.PipelineResult;

/**
 * An editor for a single sample result object.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SampleResultEditor extends Editor, ViewerContextProvider {
    
    void loadSampleResult(PipelineResult pipelineResult, boolean isUserDriven, Callable<Void> success);

    SelectionModel<?,?> getSelectionModel();
}
