package org.janelia.jacs2.asyncservice.lsmfileservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("archivedLsmMetadata")
public class ArchivedLsmMetadataServiceDescriptor implements ServiceDescriptor {
    static class ArchivedLsmMetadataArgs {
        @Parameter(names = "-archivedLSM", description = "Archived LSM file name", required = true)
        String archiveLSMFile;
        @Parameter(names = "-outputLSMMetadata", description = "Destination directory", required = true)
        String outputLSMMetadata;
        @Parameter(names = "-keepIntermediateLSM", arity = 0, description = "If used the temporary LSM file created from the archive will not be deleted", required = false)
        boolean keepIntermediateLSM = false;
    }

    private final ArchivedLsmMetadataProcessor archivedLsmMetadataProcessor;

    @Inject
    ArchivedLsmMetadataServiceDescriptor(ArchivedLsmMetadataProcessor archivedLsmMetadataProcessor) {
        this.archivedLsmMetadataProcessor = archivedLsmMetadataProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        String serviceName = this.getClass().getAnnotation(Named.class).value();
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(serviceName);
        ArchivedLsmMetadataArgs args = new ArchivedLsmMetadataArgs();
        StringBuilder usageOutput = new StringBuilder();
        JCommander jc = new JCommander(args);
        jc.setProgramName(serviceName);
        jc.usage(usageOutput);
        smd.setUsage(usageOutput.toString());
        return smd;
    }

    @Override
    public ArchivedLsmMetadataProcessor createServiceProcessor() {
        return archivedLsmMetadataProcessor;
    }

}
