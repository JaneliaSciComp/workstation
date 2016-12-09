package org.janelia.jacs2.sampleprocessing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.impl.AbstractServiceComputation;

import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Named("lsmMetadataFilesService")
public class CreateLsmMetadataFilesComputation extends AbstractServiceComputation {

    @Override
    public CompletionStage<JacsServiceData> processData(JacsServiceData jacsServiceData) {
        // TODO
        return null;
    }
}
