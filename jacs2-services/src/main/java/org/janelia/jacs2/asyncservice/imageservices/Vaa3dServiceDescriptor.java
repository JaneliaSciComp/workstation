package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

import javax.inject.Inject;
import javax.inject.Named;

@Named("vaa3d")
public class Vaa3dServiceDescriptor implements ServiceDescriptor {
    static class Vaa3dArgs {
        @Parameter(names = "-vaa3dArgs", description = "Arguments for vaa3d")
        String vaa3dArgs;
    }

    private final Vaa3dProcessor vaa3dProcessor;

    @Inject
    Vaa3dServiceDescriptor(Vaa3dProcessor vaa3dProcessor) {
        this.vaa3dProcessor = vaa3dProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        String serviceName = this.getClass().getAnnotation(Named.class).value();
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(serviceName);
        Vaa3dArgs args = new Vaa3dArgs();
        StringBuilder usageOutput = new StringBuilder();
        JCommander jc = new JCommander(args);
        jc.setProgramName(serviceName);
        jc.usage(usageOutput);
        smd.setUsage(usageOutput.toString());
        return smd;
    }

    @Override
    public Vaa3dProcessor createServiceProcessor() {
        return vaa3dProcessor;
    }

}
