package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;

class SampleServiceArgs extends ServiceArgs {
    @Parameter(names = "-sampleId", description = "Sample ID", required = true)
    Long sampleId;
    @Parameter(names = "-objective",
            description = "Optional sample objective. If specified it retrieves all sample image files, otherwise it only retrieves the ones for the given objective", required = false)
    String sampleObjective;
    @Parameter(names = "-sampleDataDir", description = "Sample data directory", required = false)
    String sampleDataDir;
}
