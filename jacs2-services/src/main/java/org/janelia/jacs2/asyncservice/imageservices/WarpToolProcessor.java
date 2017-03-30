package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.VoidServiceResultHandler;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named("warpTool")
public class WarpToolProcessor extends AbstractExeBasedServiceProcessor<Void> {

    static class AntsToolArgs extends ServiceArgs {
        @Parameter(names = {"-d", "-dims"}, description = "Scene dimensionality")
        int dims = 3;
        @Parameter(names = {"-i", "-input"}, description = "Input moving image")
        String input;
        @Parameter(names = {"-o", "-output"}, description = "Output image")
        String output;
        @Parameter(names = {"-r", "-R", "-reference"}, description = "Reference image name")
        List<String> references;
        @Parameter(names = {"-n", "--use-NN"}, description = "Nearest neighbor interpolation")
        boolean nearestNeighborInterpolation;
        @Parameter(names = {"--use-BSpline", "-bspline"}, description = "B-Spline interpolation")
        boolean bSplineInterpolation;
        @Parameter(names = {"--tightest-bounding-box"}, description = "Tightest bounding box")
        boolean tightestBoundingBox;
        @Parameter(names = {"--reslice-by-header"}, description = "Reslice by header")
        boolean resliceByHeader;
        @Parameter(names = {"--use-ML", "-sigma"}, description = "Anti aliasing interpolation with Gaussian standard deviation sigma. " +
                "Examples: " +
                "--use-ML 0.4mm " +
                "--use-ML 0.8x0.8x0.8vox")
        String sigma;
        @Parameter(names = {"-inverse"}, description = "Inverse transformation")
        List<String> inverse;
    }

    private final String executable;
    private final String libraryPath;

    @Inject
    WarpToolProcessor(ServiceComputationFactory computationFactory,
                      JacsServiceDataPersistence jacsServiceDataPersistence,
                      @Any Instance<ExternalProcessRunner> serviceRunners,
                      @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                      @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                      @PropertyValue(name = "WARP.Bin.Path") String executable,
                      @PropertyValue(name = "WARP.Library.Path") String libraryPath,
                      Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, serviceRunners, defaultWorkingDir, executablesBaseDir, logger);
        this.executable = executable;
        this.libraryPath = libraryPath;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new AntsToolArgs());
    }

    @Override
    public ServiceResultHandler<Void> getResultHandler() {
        return new VoidServiceResultHandler();
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        AntsToolArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        createScript(args, externalScriptWriter);
        externalScriptWriter.close();
        return externalScriptCode;
    }

    private void createScript(AntsToolArgs args, ScriptWriter scriptWriter) {
        scriptWriter.addWithArgs(getExecutable())
                .addArg(String.valueOf(args.dims));
        if (StringUtils.isNotBlank(args.input)) {
            scriptWriter.addArg(args.input);
        }
        if (StringUtils.isNotBlank(args.output)) {
            scriptWriter.addArg(args.output);
        }
        if (CollectionUtils.isNotEmpty(args.references)) {
            scriptWriter.addArgFlag("-R", args.references.stream().collect(Collectors.joining(" ")));
        }
        if (CollectionUtils.isNotEmpty(args.inverse)) {
            scriptWriter.addArgFlag("-i", args.inverse.stream().collect(Collectors.joining(" ")));
        }
        if (StringUtils.isNotBlank(args.sigma)) {
            scriptWriter.addArgFlag("--use-ML", args.sigma);
        }
        if (args.nearestNeighborInterpolation) {
            scriptWriter.addArg("--use-NN");
        }
        if (args.bSplineInterpolation) {
            scriptWriter.addArg("--use-BSpline");
        }
        if (args.tightestBoundingBox) {
            scriptWriter.addArg("--tightest-bounding-box");
        }
        if (args.resliceByHeader) {
            scriptWriter.addArg("--reslice-by-header");
        }
        scriptWriter.endArgs("");
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    private AntsToolArgs getArgs(JacsServiceData jacsServiceData) {
        AntsToolArgs args = new AntsToolArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private String getExecutable() {
        return getFullExecutableName(executable);
    }

}
