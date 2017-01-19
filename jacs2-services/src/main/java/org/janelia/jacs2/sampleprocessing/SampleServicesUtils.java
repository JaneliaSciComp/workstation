package org.janelia.jacs2.sampleprocessing;

import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.model.service.JacsServiceDataBuilder;
import org.janelia.jacs2.service.impl.ComputationException;

public class SampleServicesUtils {

    static JacsServiceData createChildSampleServiceData(String serviceName, SampleServiceArgs args, JacsServiceData currentServiceData) {
        if (args.sampleId == null) {
            throw new IllegalArgumentException("Sample Id is required");
        } else if (StringUtils.isBlank(args.sampleDataDir)) {
            throw new ComputationException(currentServiceData, "Output directory is required");
        }
        JacsServiceDataBuilder sampleLSMsServiceDataBuilder = new JacsServiceDataBuilder(currentServiceData)
                .setName(serviceName)
                .addArg("-sampleId", args.sampleId.toString());
        if (StringUtils.isNotBlank(args.sampleObjective)) {
            sampleLSMsServiceDataBuilder.addArg("-objective", args.sampleObjective);
        }
        sampleLSMsServiceDataBuilder.addArg("-sampleDataDir", args.sampleDataDir);
        return sampleLSMsServiceDataBuilder.build();
    }

}
