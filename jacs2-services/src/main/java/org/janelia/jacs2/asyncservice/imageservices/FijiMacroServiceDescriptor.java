package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("fijiMacro")
public class FijiMacroServiceDescriptor implements ServiceDescriptor {
    static class FijiMacroArgs {
        @Parameter(names = "-macro", description = "FIJI macro name", required = true)
        String macroName;
        @Parameter(names = "-macroArgs", description = "Arguments for the fiji macro")
        String macroArgs;
    }

    private final FijiMacroProcessor fijiMacroProcessor;

    @Inject
    FijiMacroServiceDescriptor(FijiMacroProcessor fijiMacroProcessor) {
        this.fijiMacroProcessor = fijiMacroProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        String serviceName = this.getClass().getAnnotation(Named.class).value();
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(serviceName);
        FijiMacroArgs args = new FijiMacroArgs();
        StringBuilder usageOutput = new StringBuilder();
        JCommander jc = new JCommander(args);
        jc.setProgramName(serviceName);
        jc.usage(usageOutput);
        smd.setUsage(usageOutput.toString());
        return smd;
    }

    @Override
    public FijiMacroProcessor createServiceProcessor() {
        return fijiMacroProcessor;
    }

}
