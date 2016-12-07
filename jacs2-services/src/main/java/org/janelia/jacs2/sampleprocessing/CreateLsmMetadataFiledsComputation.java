package org.janelia.jacs2.sampleprocessing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.service.impl.AbstractLocalProcessComputation;

import javax.inject.Named;
import java.util.List;
import java.util.Map;

@Named("lsmMetadataFilesService")
public class CreateLsmMetadataFiledsComputation extends AbstractLocalProcessComputation {

    @Override
    protected List<String> prepareCommandLine(JacsServiceData jacsServiceData) {
        ImmutableList.Builder cmdLineBuilder = new ImmutableList.Builder<>();
        cmdLineBuilder.add(jacsServiceData.getServiceCmd());
        cmdLineBuilder.add(jacsServiceData.getId().toString());
        cmdLineBuilder.add(jacsServiceData.getName());
        if (CollectionUtils.isNotEmpty(jacsServiceData.getArgs())) {
            cmdLineBuilder.addAll(jacsServiceData.getArgs());
        }
        return cmdLineBuilder.build();
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData si) {
        return ImmutableMap.of();
    }
}
