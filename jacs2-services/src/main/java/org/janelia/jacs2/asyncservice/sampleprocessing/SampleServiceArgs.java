package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

class SampleServiceArgs {

    static <A extends SampleServiceArgs> A parse(String[] argsList, A args) {
        new JCommander(args).parse(argsList);
        return args;
    }

    static <A extends SampleServiceArgs> String usage(String serviceName, A args) {
        StringBuilder usageOutput = new StringBuilder();
        JCommander jc = new JCommander(args);
        jc.setProgramName(serviceName);
        jc.usage(usageOutput);
        return usageOutput.toString();
    }

    @Parameter(names = "-sampleId", description = "Sample ID", required = true)
    Long sampleId;
    @Parameter(names = "-objective",
            description = "Optional sample objective. If specified it retrieves all sample image files, otherwise it only retrieves the ones for the given objective", required = false)
    String sampleObjective;
    @Parameter(names = "-sampleDataDir", description = "Sample data directory", required = false)
    String sampleDataDir;
}
