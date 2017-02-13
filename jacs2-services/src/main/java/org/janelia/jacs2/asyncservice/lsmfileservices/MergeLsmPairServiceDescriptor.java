package org.janelia.jacs2.asyncservice.lsmfileservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

import javax.inject.Inject;
import javax.inject.Named;

@Named("mergeLsmPair")
public class MergeLsmPairServiceDescriptor implements ServiceDescriptor {
    static class MergeLsmPairArgs extends ServiceArgs {
        @Parameter(names = "-lsm1", description = "First LSM", required = true)
        String lsm1File;
        @Parameter(names = "-lsm2", description = "Second LSM", required = true)
        String lsm2File;
        @Parameter(names = "-multiscanVersion", description = "Multiscan blend version", required = false)
        String multiscanBlendVersion;
        @Parameter(names = "-outputLsm", description = "Destination LSM", required = true)
        String outputLsmFile;
    }

    private final MergeLsmPairProcessor mergeLsmPairProcessor;

    @Inject
    MergeLsmPairServiceDescriptor(MergeLsmPairProcessor mergeLsmPairProcessor) {
        this.mergeLsmPairProcessor = mergeLsmPairProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new MergeLsmPairArgs());
    }

    @Override
    public MergeLsmPairProcessor createServiceProcessor() {
        return mergeLsmPairProcessor;
    }
}
