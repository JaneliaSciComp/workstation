package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

import javax.inject.Inject;
import javax.inject.Named;

@Named("vaa3dCmd")
public class Vaa3dCmdServiceDescriptor implements ServiceDescriptor {
    static class Vaa3dCmdArgs extends ServiceArgs {
        @Parameter(names = "-vaa3dCmd", description = "Vaa3d headless command", required = true)
        String vaa3dCmd;
        @Parameter(names = "-vaa3dCmdArgs", description = "Arguments for vaa3d")
        String vaa3dCmdArgs;
    }

    private final Vaa3dCmdProcessor vaa3dCmdProcessor;

    @Inject
    Vaa3dCmdServiceDescriptor(Vaa3dCmdProcessor vaa3dCmdProcessor) {
        this.vaa3dCmdProcessor = vaa3dCmdProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Vaa3dCmdArgs());
    }

    @Override
    public Vaa3dCmdProcessor createServiceProcessor() {
        return vaa3dCmdProcessor;
    }

}
