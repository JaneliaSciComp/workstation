package org.janelia.jacs2.sampleprocessing;

import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.impl.AbstractServiceComputation;
import org.janelia.jacs2.service.impl.JacsService;

import javax.inject.Named;
import java.util.concurrent.CompletionStage;

@Named("flylightSamplePipelineService")
public class FlylightSamplePipelineComputation extends AbstractServiceComputation<Void> {
    @Override
    public CompletionStage<JacsService<Void>> processData(JacsService<Void> jacsService) {
        // !!!!!!! TODO
        return null;
    }
}
