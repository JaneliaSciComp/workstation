package org.janelia.jacs2.imageservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;
import org.janelia.jacs2.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.service.impl.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.service.impl.ComputationException;
import org.janelia.jacs2.service.impl.ExternalProcessRunner;
import org.janelia.jacs2.service.impl.JacsServiceDispatcher;
import org.janelia.jacs2.service.impl.ServiceComputation;
import org.janelia.jacs2.service.impl.ServiceComputationFactory;
import org.janelia.jacs2.service.impl.ServiceDataUtils;
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
    protected List<String> prepareCmdArgs(JacsServiceData jacsServiceData) {
        VideoFormatConverterServiceDescriptor.ConverterArgs args = getArgs(jacsServiceData);
        jacsServiceData.setServiceCmd(getFFMPEGExecutable());
        ImmutableList.Builder<String> cmdLineBuilder = new ImmutableList.Builder<>();
        cmdLineBuilder
                .add("-y")
                .add("-r").add("7")
                .add("-i").add(args.input)
                .add("-vcodec")
                .add("libx264")
                .add("-b:v")
                .add("2000000")
                .add("-preset")
                .add("slow")
                .add("-tune")
                .add("film")
                .add("-pix_fmt")
                .add("yuv420p");
        if (args.truncate) {
            cmdLineBuilder
                    .add("-vf")
                    .add("scale=trunc(iw/2)*2:trunc(ih/2)*2");
        }
        cmdLineBuilder.add(args.getOutputName());
        return cmdLineBuilder.build();
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
