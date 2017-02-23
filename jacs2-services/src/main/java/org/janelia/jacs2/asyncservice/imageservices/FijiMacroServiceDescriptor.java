package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.janelia.jacs2.asyncservice.common.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

@Named("fijiMacro")
public class FijiMacroServiceDescriptor implements ServiceDescriptor {
    static class FijiMacroArgs extends ServiceArgs {
        @Parameter(names = "-macro", description = "FIJI macro name", required = true)
        String macroName;
        @Parameter(names = "-macroArgs", description = "Arguments for the fiji macro")
        String macroArgs;
        @Parameter(names = "-temporaryOutput", description = "Temporary output directory")
        String temporaryOutput;
        @Parameter(names = "-finalOutput", description = "Final output directory")
        String finalOutput;
        @Parameter(names = "-resultsPatterns", description = "results patterns")
        List<String> resultsPatterns = new ArrayList<>();
    }

    private final FijiMacroProcessor fijiMacroProcessor;

    @Inject
    FijiMacroServiceDescriptor(FijiMacroProcessor fijiMacroProcessor) {
        this.fijiMacroProcessor = fijiMacroProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new FijiMacroArgs());
    }

    @Override
    public FijiMacroProcessor createServiceProcessor() {
        return fijiMacroProcessor;
    }

}
