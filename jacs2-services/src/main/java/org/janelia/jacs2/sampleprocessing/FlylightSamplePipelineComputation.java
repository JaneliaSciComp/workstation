package org.janelia.jacs2.sampleprocessing;

import org.janelia.jacs2.service.impl.AbstractServiceProcessor;
import org.janelia.jacs2.service.impl.JacsService;

import javax.inject.Named;
import java.util.concurrent.CompletionStage;

@Named("flylightSamplePipelineService")
public class FlylightSamplePipelineComputation extends AbstractServiceProcessor<Void> {
    @Override
    public CompletionStage<JacsService<Void>> processData(JacsService<Void> jacsService) {
        // !!!!!!! TODO
        return null;
    }
}
