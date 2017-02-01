package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.JacsServiceDispatcher;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class VideoFormatConverterProcessor extends AbstractExeBasedServiceProcessor<File> {

    private final String ffmpegExecutable;
    private final String libraryPath;

    @Inject
    VideoFormatConverterProcessor(JacsServiceDispatcher jacsServiceDispatcher,
                                  ServiceComputationFactory computationFactory,
                                  JacsServiceDataPersistence jacsServiceDataPersistence,
                                  @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                  @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                                  @Any Instance<ExternalProcessRunner> serviceRunners,
                                  @PropertyValue(name = "FFMPEG.Bin.Path") String ffmpegExecutable,
                                  @PropertyValue(name = "VAA3D.LibraryPath") String libraryPath,
                                  Logger logger) {
        super(jacsServiceDispatcher, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.ffmpegExecutable = ffmpegExecutable;
        this.libraryPath = libraryPath;
    }

    @Override
    public File getResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToFile(jacsServiceData.getStringifiedResult());
    }

    @Override
    public void setResult(File result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.fileToString(result));
    }

    @Override
    protected ServiceComputation<File> preProcessData(JacsServiceData jacsServiceData) {
        VideoFormatConverterServiceDescriptor.ConverterArgs args = getArgs(jacsServiceData);
        if (StringUtils.isBlank(args.input)) {
            throw new ComputationException(jacsServiceData, "Input must be specified");
        }
        File outputFile = new File(args.getOutputName());
        try {
            Files.createDirectories(outputFile.getParentFile().toPath());
        } catch (IOException e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return computationFactory.newCompletedComputation(outputFile);
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        File destFile = (File) preProcessingResult;
        return Files.exists(destFile.toPath());

    }

    @Override
    protected File retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return (File) preProcessingResult;
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        VideoFormatConverterServiceDescriptor.ConverterArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        externalScriptWriter.addWithArgs(getFFMPEGExecutable())
                .addArg("-y")
                .addArg("-r").add("7")
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

    private VideoFormatConverterServiceDescriptor.ConverterArgs getArgs(JacsServiceData jacsServiceData) {
        VideoFormatConverterServiceDescriptor.ConverterArgs args = new VideoFormatConverterServiceDescriptor.ConverterArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private String getFFMPEGExecutable() {
        return getFullExecutableName(ffmpegExecutable);
    }

}
