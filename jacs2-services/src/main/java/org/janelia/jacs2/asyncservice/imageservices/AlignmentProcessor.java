package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.asyncservice.utils.X11Utils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Named("align")
public class AlignmentProcessor extends AbstractExeBasedServiceProcessor<List<File>> {

    static class AlignmentArgs extends ServiceArgs {
        @Parameter(names = {"-nthreads"}, description = "Number of ITK threads")
        Integer nthreads = 16;
        @Parameter(names = "-i1File", description = "The name of the first input file", required = true)
        String input1File;
        @Parameter(names = "-i1Channels", description = "The channels of the first input file", required = true)
        String input1Channels;
        @Parameter(names = "-i1Ref", description = "The reference for the first input file", required = true)
        String input1Ref;
        @Parameter(names = "-i1Res", description = "The resolution of the first input file", required = true)
        String input1Res;
        @Parameter(names = "-i1Dims", description = "The dimensions of the first input file", required = true)
        String input1Dims;
        @Parameter(names = {"-e", "-i1Neurons"}, description = "Input1 neurons file", required = false)
        String input1Neurons;
        @Parameter(names = "-i2File", description = "The name of the second input file", required = false)
        String input2File;
        @Parameter(names = "-i2Channels", description = "The channels of the second input file", required = false)
        String input2Channels;
        @Parameter(names = "-i2Ref", description = "The reference for the second input file", required = false)
        String input2Ref;
        @Parameter(names = "-i2Res", description = "The resolution of the second input file", required = false)
        String input2Res;
        @Parameter(names = "-i2Dims", description = "The dimensions of the second input file", required = false)
        String input2Dims;
        @Parameter(names = {"-f", "-i2Neurons"}, description = "Input2 neurons file", required = false)
        String input2Neurons;
        @Parameter(names = {"-c", "-config"}, description = "Configuration file", required = true)
        String configFile;
        @Parameter(names = {"-t", "-templateDir"}, description = "Template directory", required = true)
        String templateDir;
        @Parameter(names = {"-k", "-toolsDir"}, description = "Tools directory", required = true)
        String toolsDir;
        @Parameter(names = {"-s", "-step"}, description = "Step", required = false)
        String step;
        @Parameter(names = {"-m", "-mp", "-mountingProtocol"}, description = "Mounting protocol", required = false)
        String mountingProtocol;
        @Parameter(names = {"-g", "-gender"}, description = "Gender", required = false)
        String gender;
        @Parameter(names = {"-z", "-zflip"}, arity = 0, description = "Z flip flag", required = false)
        boolean zFlip = false;
        @Parameter(names = "-fslOutputType", description = "FSL output type", required = false)
        String fslOutputType = "NIFTI_GZ";
        @Parameter(names = "-resultsDir", description = "Results directory", required = false)
        String resultsDir;
    }

    private final String alignmentScript;
    private final String libraryPath;

    @Inject
    AlignmentProcessor(JacsServiceEngine jacsServiceEngine,
                       ServiceComputationFactory computationFactory,
                       JacsServiceDataPersistence jacsServiceDataPersistence,
                       @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                       @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                       @Any Instance<ExternalProcessRunner> serviceRunners,
                       @PropertyValue(name = "Alignment.Script.Path") String alignmentScript,
                       @PropertyValue(name = "Alignment.Library.Path") String libraryPath,
                       Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.alignmentScript = alignmentScript;
        this.libraryPath = libraryPath;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new AlignmentArgs());
    }

    @Override
    public List<File> getResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToFileList(jacsServiceData.getStringifiedResult());
    }

    @Override
    public void setResult(List<File> result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.fileListToString(result));
    }

    @Override
    protected ServiceComputation<JacsServiceData> prepareProcessing(JacsServiceData jacsServiceData) {
        AlignmentArgs args = getArgs(jacsServiceData);
        try {
            Files.createDirectories(getResultsDir(args));
        } catch (IOException e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return createComputation(jacsServiceData);
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        AlignmentArgs args = getArgs(jacsServiceData);
        // count v3dpbd
        try {
            String resultsPattern = "glob:**/*.{v3dpbd}";
            PathMatcher inputFileMatcher =
                    FileSystems.getDefault().getPathMatcher(resultsPattern);
            long nFiles = Files.find(getResultsDir(args), 1, (p, a) -> inputFileMatcher.matches(p)).count();
            return nFiles > 0;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected List<File> retrieveResult(JacsServiceData jacsServiceData) {
        AlignmentArgs args = getArgs(jacsServiceData);
        // collect all v3dpbd
        List<File> results = new ArrayList<>();
        try {
            String resultsPattern = "glob:**/*.{v3dpbd}";
            PathMatcher inputFileMatcher =
                    FileSystems.getDefault().getPathMatcher(resultsPattern);
            Files.find(getResultsDir(args), 1, (p, a) -> inputFileMatcher.matches(p)).forEach(p -> results.add(p.toFile()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return results;
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        AlignmentArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        createScript(jacsServiceData, args, externalScriptWriter);
        externalScriptWriter.close();
        return externalScriptCode;
    }

    private void createScript(JacsServiceData jacsServiceData, AlignmentArgs args, ScriptWriter scriptWriter) {
        try {
            Path workingDir = getWorkingDirectory(jacsServiceData);
            X11Utils.setDisplayPort(workingDir.toString(), scriptWriter);
            scriptWriter.addWithArgs(getAlignmentScript(args));
            scriptWriter
                    .addArgFlag("-c", args.configFile)
                    .addArgFlag("-t", args.templateDir)
                    .addArgFlag("-k", args.toolsDir)
                    .addArgFlag("-w", getResultsDir(args).toString())
                    .addArgFlag("-i", String.join(",",
                            args.input1File, args.input1Channels, args.input1Ref, args.input1Res, args.input1Dims));
            if (StringUtils.isNotBlank(args.input2File)) {
                scriptWriter.addArgFlag("-j", String.join(",",
                        args.input2File, args.input2Channels, args.input2Ref, args.input2Res, args.input2Dims));
            }
            scriptWriter
                    .addArgFlag("-m", args.mountingProtocol)
                    .addArgFlag("-s", args.step);
            if (StringUtils.isNotBlank(args.gender)) {
                scriptWriter.addArgFlag("-g", args.gender);
            }
            if (StringUtils.isNotBlank(args.input1Neurons)) {
                scriptWriter.addArgFlag("-e", args.input1Neurons);
            }
            if (StringUtils.isNotBlank(args.input2Neurons)) {
                scriptWriter.addArgFlag("-f", args.input2Neurons);
            }
            if (args.zFlip) {
                scriptWriter.addArgFlag("-z", "zflip");
            }
            scriptWriter.endArgs("");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        AlignmentArgs args = getArgs(jacsServiceData);
        ImmutableMap.Builder envBuilder = new ImmutableMap.Builder<String, String>()
                .put(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath))
                .put("ITK_GLOBAL_DEFAULT_NUMBER_OF_THREADS", args.nthreads.toString());
        if (StringUtils.isNotBlank(args.fslOutputType)) {
            envBuilder.put("FSLOUTPUTTYPE", args.fslOutputType);
        }
        return envBuilder.build();
    }

    private String getAlignmentScript(AlignmentArgs args) {
        if (alignmentScript.startsWith("/")) {
            return alignmentScript;
        } else {
            return getFullExecutableName(alignmentScript);
        }
    }

    private Path getResultsDir(AlignmentArgs args) {
        return Paths.get(args.resultsDir, com.google.common.io.Files.getNameWithoutExtension(args.input1File));
    }

    private AlignmentArgs getArgs(JacsServiceData jacsServiceData) {
        return AlignmentArgs.parse(jacsServiceData.getArgsArray(), new AlignmentArgs());
    }
}
