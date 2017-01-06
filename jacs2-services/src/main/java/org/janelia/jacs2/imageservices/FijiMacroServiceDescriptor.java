package org.janelia.jacs2.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named("fijiMacro")
public class FijiMacroServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "fijiMacro";

    static class FijiMacroArgs {
        @Parameter(names = "-macro", description = "FIJI macro name", required = true)
        String macroName;
        @Parameter(names = "-macroArgs", description = "Arguments for the fiji macro")
        String macroArgs;
        String workingDir;
    }

    @Inject
    private Instance<FijiMacroComputation> fijiMacroComputationSource;

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
    public ServiceComputation createComputationInstance() {
        return fijiMacroComputationSource.get();
    }

}
