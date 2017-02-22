package org.janelia.jacs2.asyncservice.imageservices;


import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

@Named("vaa3dPlugin")
public class Vaa3dPluginServiceDescriptor implements ServiceDescriptor {
    static class Vaa3dPluginArgs extends ServiceArgs {
        @Parameter(names = {"-x", "-plugin"}, description = "Vaa3d plugin name", required = true)
        String plugin;
        @Parameter(names = {"-f", "-pluginFunc"}, description = "Vaa3d plugin function", required = true)
        String pluginFunc;
        @Parameter(names = {"-i", "-input"}, description = "Plugin input", required = true)
        String pluginInput;
        @Parameter(names = {"-o", "-output"}, description = "Plugin output", required = true)
        String pluginOutput;
        @Parameter(names = {"-p", "-pluginParams"}, description = "Plugin parameters")
        List<String> pluginParams = new ArrayList<>();
    }

    private final Vaa3dPluginProcessor vaa3dPluginProcessor;

    @Inject
    Vaa3dPluginServiceDescriptor(Vaa3dPluginProcessor vaa3dPluginProcessor) {
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Vaa3dPluginArgs());
    }

    @Override
    public Vaa3dPluginProcessor createServiceProcessor() {
        return vaa3dPluginProcessor;
    }

}
