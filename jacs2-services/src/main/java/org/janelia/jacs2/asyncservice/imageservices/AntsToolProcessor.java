package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Map;

public class AntsToolProcessor extends AbstractExeBasedServiceProcessor<Void> {

    private final String executable;
    private final String libraryPath;

    @Inject
    AntsToolProcessor(JacsServiceEngine jacsServiceEngine,
                      ServiceComputationFactory computationFactory,
                      JacsServiceDataPersistence jacsServiceDataPersistence,
                      @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                      @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                      @Any Instance<ExternalProcessRunner> serviceRunners,
                      @PropertyValue(name = "ANTS.Bin.Path") String executable,
                      @PropertyValue(name = "ANTS.LibraryPath") String libraryPath,
                      Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.executable = executable;
        this.libraryPath = libraryPath;
    }

    @Override
    public Void getResult(JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public void setResult(Void result, JacsServiceData jacsServiceData) {
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return true;
    }

    @Override
    protected Void retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        AntsToolServiceDescriptor.AntsToolArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        createScript(args, externalScriptWriter);
        externalScriptWriter.close();
        return externalScriptCode;
    }

    private void createScript(AntsToolServiceDescriptor.AntsToolArgs args, ScriptWriter scriptWriter) {
        scriptWriter.addWithArgs(getExecutable())
                .addArgFlag("-x", args.mask)
                .addArgFlag("-m", args.imageMetric)
                .addArgFlag("-o", args.output)
                .addArgFlag("-r", args.roi)
                .addArgFlag("-i", args.iterations)
                .addArgFlag("--Restrict-Deformation", args.deformation)
                .addArgFlag("--use-all-metrics-for-convergence", args.useAllMetricsForConvergence)
                .addArgFlag("-t", args.transformationModel)
                .addArgFlag("--regularization", args.regularization)
                .addArgFlag("-a", args.initialAffine)
                .addArgFlag("-F", args.initialAffineImage)
                .addArgFlag("--fixed-image-initial-affine-ref-image", args.initialAffineRefImage)
                .addArgFlag("-T", Integer.toString(args.geodesic))
                .addArgFlag("-G", args.fastSyN)
                .addArgFlag("--continue-affine", args.performInitialAffine)
                .addArgFlag("--number-of-affine-iterations", args.affineIterations)
                .addArgFlag("--use-NN", args.useNN)
                .addArgFlag("--use-Histogram-Matching", args.useHistogram)
                .addArgFlag("--affine-metric-type", args.affineMetric)
                .addArgFlag("--MI-option", args.mutualInformation)
                .addArgFlag("--rigid-affine", args.rigidAffine)
                .addArgFlag("--affine-gradient-descent-option", args.affineGradientDescent)
                .addArgFlag("--use-rotation-header", args.useRotation)
                .addArgFlag("--gaussian-smoothing-sigmas", args.gaussianSigmas)
                .addArgFlag("--subsampling-factors", args.subSampling)
                .endArgs("");
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    private AntsToolServiceDescriptor.AntsToolArgs getArgs(JacsServiceData jacsServiceData) {
        AntsToolServiceDescriptor.AntsToolArgs args = new AntsToolServiceDescriptor.AntsToolArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private String getExecutable() {
        return getFullExecutableName(executable);
    }

}
