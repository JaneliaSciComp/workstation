package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractSingleFileServiceResultHandler;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.nio.file.Files;
import java.util.Map;

@Named("mpegConverter")
public class VideoFormatConverterProcessor extends AbstractExeBasedServiceProcessor<File> {

    static class ConverterArgs extends ServiceArgs {
        private static final String DEFAULT_OUTPUT_EXT = ".mp4";

        @Parameter(names = "-input", description = "Input file name", required = true)
        String input;
        @Parameter(names = "-output", description = "Output file name")
        String output;
        @Parameter(names = "-trunc", arity = 0, description = "Truncate flag", required = false)
        boolean truncate = false;

        String getOutputName() {
            if (StringUtils.isBlank(output)) {
                String extension = com.google.common.io.Files.getFileExtension(input);
                return input.replace("." + extension, DEFAULT_OUTPUT_EXT);
            }
            return output;
        }
    }

    private final String executable;
    private final String libraryPath;

    @Inject
    VideoFormatConverterProcessor(ServiceComputationFactory computationFactory,
                                  JacsServiceDataPersistence jacsServiceDataPersistence,
                                  @Any Instance<ExternalProcessRunner> serviceRunners,
                                  @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                  @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                                  @PropertyValue(name = "FFMPEG.Bin.Path") String executable,
                                  @PropertyValue(name = "VAA3D.Library.Path") String libraryPath,
                                  Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, serviceRunners, defaultWorkingDir, executablesBaseDir, logger);
        this.executable = executable;
        this.libraryPath = libraryPath;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new ConverterArgs());
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            ConverterArgs args = getArgs(jacsServiceData);
            if (StringUtils.isBlank(args.input)) {
                throw new ComputationException(jacsServiceData, "Input must be specified");
            }
            File outputFile = getOutputFile(args);
            Files.createDirectories(outputFile.getParentFile().toPath());
        } catch (ComputationException e) {
            throw e;
        } catch (Exception e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    public ServiceResultHandler<File> getResultHandler() {
        return new AbstractSingleFileServiceResultHandler() {

            @Override
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                ConverterArgs args = getArgs(jacsServiceData);
                File outputFile = getOutputFile(args);
                return outputFile.exists();
            }

            @Override
            public File collectResult(JacsServiceData jacsServiceData) {
                ConverterArgs args = getArgs(jacsServiceData);
                return getOutputFile(args);
            }
        };
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        ConverterArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        externalScriptWriter.addWithArgs(getExecutable())
                .addArg("-y")
                .addArg("-r").addArg("7")
                .addArg("-i").addArg(args.input)
                .addArg("-vcodec")
                .addArg("libx264")
                .addArg("-b:v")
                .addArg("2000000")
                .addArg("-preset")
                .addArg("slow")
                .addArg("-tune")
                .addArg("film")
                .addArg("-pix_fmt")
                .addArg("yuv420p");
        if (args.truncate) {
            externalScriptWriter
                    .addArg("-vf")
                    .addArg("scale=trunc(iw/2)*2:trunc(ih/2)*2");
        }
        externalScriptWriter.addArg(args.getOutputName());
        externalScriptWriter.close();
        return externalScriptCode;
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    private ConverterArgs getArgs(JacsServiceData jacsServiceData) {
        ConverterArgs args = new ConverterArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private File getOutputFile(ConverterArgs args) {
        return new File(args.getOutputName());
    }

    private String getExecutable() {
        return getFullExecutableName(executable);
    }

}
