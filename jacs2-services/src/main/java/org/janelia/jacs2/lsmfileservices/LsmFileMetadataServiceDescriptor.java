package org.janelia.jacs2.lsmfileservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("lsmFileMetadata")
public class LsmFileMetadataServiceDescriptor implements ServiceDescriptor {
    static class LsmFileMetadataArgs {
        @Parameter(names = "-inputLSM", description = "LSM Input file name", required = true)
        String inputLSMFile;
        @Parameter(names = "-outputLSMMetadata", description = "Destination directory", required = true)
        String outputLSMMetadata;
    }

    private final LsmFileMetadataProcessor lsmMetadataProcessor;

    @Inject
    LsmFileMetadataServiceDescriptor(LsmFileMetadataProcessor lsmMetadataProcessor) {
        this.lsmMetadataProcessor = lsmMetadataProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        String serviceName = this.getClass().getAnnotation(Named.class).value();
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(serviceName);
        LsmFileMetadataArgs args = new LsmFileMetadataArgs();
        StringBuilder usageOutput = new StringBuilder();
        JCommander jc = new JCommander(args);
        jc.setProgramName(serviceName);
        jc.usage(usageOutput);
        smd.setUsage(usageOutput.toString());
        return smd;
    }

    @Override
    public LsmFileMetadataProcessor createServiceProcessor() {
        return lsmMetadataProcessor;
    }

}
