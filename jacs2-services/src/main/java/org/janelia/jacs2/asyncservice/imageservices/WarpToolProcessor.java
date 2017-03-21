package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
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
import java.util.Map;

@Named("warpTool")
public class WarpToolProcessor extends AbstractExeBasedServiceProcessor<Void> {

    static class AntsToolArgs extends ServiceArgs {
        @Parameter(names = {"-d", "--dimensionality", "-dims"}, description = "Scene dimensionality")
        int dims = 3;
        @Parameter(names = {"-e", "--input-image-type", "-inputImageType"}, description = "The image dimensionality: 0/1/2/3 - scalar, vector, tensor, time series")
        Integer inputImageType;
        @Parameter(names = {"-i", "--input", "-input"}, description = "Input image name")
        String input;
        @Parameter(names = {"-o", "--output-naming", "-output"},
                description = "Output name: " +
                        "[" +
                        "warpedOutputFileName, " +
                        "compositeDisplacementField,<printOutCompositeWarpFile=0>, " +
                        "Linear[genericAffineTransformFile,<calculateInverse=0> " +
                        "]")
        String output;
        @Parameter(names = {"-r", "--reference-image", "-reference"}, description = "Reference image name")
        String reference;
        @Parameter(names = {"-n", "--interpolation", "-interpolation"},
                description = "Interpolation options: " +
                        "Linear," +
                        " NearestNeighbor," +
                        " MultiLabel[<sigma=imageSpacing>,<alpha=4.0>]," +
                        "Gaussian[<sigma=imageSpacing>,<alpha=1.0>]," +
                        "BSpline[<order=3>]," +
                        "CosineWindowedSinc," +
                        "WelchWindowedSinc," +
                        "HammingWindowedSinc," +
                        "LanczosWindowedSinc," +
                        "GenericLabel[<interpolator=Linear>]")
        String interpolation;
        @Parameter(names = {"-t", "--transform", "-transform"},
                description = "Transform file name <transformFileName> or [<transformFileName>, <useInverse>]")
        String transformFile;
        @Parameter(names = {"-f", "--default-value", "-defaultValue"}, description = "Default voxel value")
        int defaultValue;
        @Parameter(names = {"-z", "--static-cast-for-R", "-staticCase"}, description = "Static cast in ReadTransform")
        double staticCost;
    }

    private final String executable;
    private final String libraryPath;

    @Inject
    WarpToolProcessor(JacsServiceEngine jacsServiceEngine,
                      ServiceComputationFactory computationFactory,
                      JacsServiceDataPersistence jacsServiceDataPersistence,
                      @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                      @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                      @Any Instance<ExternalProcessRunner> serviceRunners,
                      @PropertyValue(name = "WARP.Bin.Path") String executable,
                      @PropertyValue(name = "WARP.Library.Path") String libraryPath,
                      Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.executable = executable;
        this.libraryPath = libraryPath;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new AntsToolArgs());
    }

    @Override
    public Void getResult(JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public void setResult(Void result, JacsServiceData jacsServiceData) {
    }

    @Override
    protected ServiceComputation<JacsServiceData> prepareProcessing(JacsServiceData jacsServiceData) {
        return createComputation(jacsServiceData);
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        return true;
    }

    @Override
    protected Void retrieveResult(JacsServiceData jacsServiceData) {
        return null;
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
        if (args.inputImageType != null) {
            scriptWriter.addArgFlag("-e", args.inputImageType.toString());
        }
        if (StringUtils.isNotBlank(args.input)) {
            scriptWriter.addArgFlag("-i", args.input);
        }
        if (StringUtils.isNotBlank(args.output)) {
            scriptWriter.addArgFlag("-o", args.output);
        }
        if (StringUtils.isNotBlank(args.reference)) {
            scriptWriter.addArgFlag("-r", args.reference);
        }
        if (StringUtils.isNotBlank(args.interpolation)) {
            scriptWriter.addArgFlag("-n", args.interpolation);
        }
        if (StringUtils.isNotBlank(args.transformFile)) {
            scriptWriter.addArgFlag("-t", args.transformFile);
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
