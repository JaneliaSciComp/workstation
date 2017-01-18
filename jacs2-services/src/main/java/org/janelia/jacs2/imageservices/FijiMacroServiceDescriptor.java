package org.janelia.jacs2.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceDescriptor;
import org.janelia.jacs2.service.impl.ServiceProcessor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("fijiMacro")
public class FijiMacroServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "fijiMacro";

    static class FijiMacroArgs {
        @Parameter(names = "-macro", description = "FIJI macro name", required = true)
        String macroName;
        @Parameter(names = "-macroArgs", description = "Arguments for the fiji macro")
        String macroArgs;
    }

    private final FijiMacroProcessor fijiMacroProcessor;

    @Inject
    public FijiMacroServiceDescriptor(FijiMacroProcessor fijiMacroProcessor) {
        this.fijiMacroProcessor = fijiMacroProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        FijiMacroArgs args = new FijiMacroArgs();
        StringBuilder usageOutput = new StringBuilder();
        JCommander jc = new JCommander(args);
        jc.setProgramName(SERVICE_NAME);
        jc.usage(usageOutput);
        smd.setUsage(usageOutput.toString());
        return smd;
    }

    @Override
    public FijiMacroProcessor createServiceProcessor() {
        return fijiMacroProcessor;
    }

}
