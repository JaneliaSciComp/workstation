package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
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

@Named("antsTool")
public class AntsToolProcessor extends AbstractExeBasedServiceProcessor<Void> {

    static class AntsToolArgs extends ServiceArgs {
        @Parameter(names = {"-x", "--mask-image", "-mask"}, description = "Mask file name that defines the region of interest")
        String mask;
        @Parameter(names = {"-m", "--image-metric", "-metric"}, description = "Image metric")
        String imageMetric;
        @Parameter(names = {"-o", "--output-naming", "-output"}, description = "Output name")
        String output;
        @Parameter(names = {"-r", "--R", "-roi"},
                description = "The center and bounding box of the region of interest," +
                        " e.g. 10x12x15x50x50x25 sets a bounding box of size 50,50,25 with origin at 10,12,15 in voxel coordinates")
        String roi;
        @Parameter(names = {"-i", "--number-of-iterations", "-iterations"}, description = "Number of iterations per level, e.g. 100x100x20")
        String iterations;
        @Parameter(names = {"--Restrict-Deformation", "-deformation"}, description = "Restrict the gradient that drives the deformation")
        String deformation;
        @Parameter(names = {"--use-all-metrics-for-convergence", "-useAllMetrics"}, arity = 0,
                description = "Enable to use weighted sum of all metrics for convergence computation")
        boolean useAllMetricsForConvergence;
        @Parameter(names = {"-t", "--transformation-model", "-transformationModel"},
                description = "Transformation model formatted as <trasformation>[<gradient-step>, <number-of-steps>,<deltaTime>,<symmetry type>]. \n" +
                        "Transformation values: Diff(eomorphic), Elast(ic), Exp(onential), Greedy( Exponential), SyN(Symmetric Normalization)")
        String transformationModel;
        @Parameter(names = {"--regularization", "-regularization"},
                description = "Regularization formatted as <regularization>[<gradient-field-sigma>, <def-field-sigma>,<truncation>]. \n" +
                        "Regularization values: Gauss (gaussian), DMFFD (directly manipulated free form deformation)")
        String regularization;
        @Parameter(names = {"-a", "--initial-affine", "-initialAffine"}, description = "Name of the initial affine parameter.")
        String initialAffine;
        @Parameter(names = {"-F", "--fixed-image-initial-affine", "-initialAffineImage"}, description = "Name of the image for the initial affine parameter.")
        String initialAffineImage;
        @Parameter(names = {"--fixed-image-initial-affine-ref-image", "-initialAffineRefImage"},
                description = "Name of the reference image for the initial affine parameter.")
        String initialAffineRefImage;
        @Parameter(names = {"-T", "--geodesic", "-geo"}, description = "Geodesic flag: 0 - not time dependent, 1 - asymmetric, 2 - symmetric")
        int geodesic;
        @Parameter(names = {"-G", "--go-faster", "-fastSyN"}, arity = 0, description = "Fast SyN but less accurate wrt inverse-identity constraint")
        boolean fastSyN;
        @Parameter(names = {"--continue-affine", "-performInitialAffine"}, arity = 0, description = "Perform affine given the initial affine parameters")
        boolean performInitialAffine = true;
        @Parameter(names = {"--number-of-affine-iterations", "-affineIterations"}, description = "Number of affine iterations per level, e.g. 100x100x20")
        String affineIterations;
        @Parameter(names = {"--use-NN", "-useNN"}, arity = 0, description = "Use nearest neighbor interpolation")
        boolean useNN;
        @Parameter(names = {"--use-Histogram-Matching", "-useHistogram"}, arity = 0, description = "Use histogram matching of moving to fixed image")
        boolean useHistogram = true;
        @Parameter(names = {"--affine-metric-type", "-affineMetric"},
                description = "Affine metric type: " +
                        "MI - mutual information, " +
                        "MSQ - mean square error, SSD, " +
                        "CC - Normalized correlation, " +
                        "CCH - Histogram based correlation coefficient, " +
                        "GD - gradient difference")
        String affineMetric = "MI";
        @Parameter(names = {"--MI-option", "-mi"}, description = "Mutual information: <MI_bins>x<MI_samples>")
        String mutualInformation = "32x32000";
        @Parameter(names = {"--rigid-affine", "-rigidAffine"}, arity = 0, description = "Use rigid affine")
        boolean rigidAffine;
        @Parameter(names = {"--affine-gradient-descent-option", "-affineGradientDescent"},
                description = "Affine gradient descent: <relaxation>x<min step length>x<translation scale>")
        String affineGradientDescent = "0.1x0.5x1.e-4x1.e-4";
        @Parameter(names = {"--use-rotation-header", "-useRotation"}, arity = 0, description = "Use rotation")
        boolean useRotation;
        @Parameter(names = {"--gaussian-smoothing-sigmas", "-gaussianSigmas"}, description = "Gaussian smoothing sigma values for each level")
        String gaussianSigmas;
        @Parameter(names = {"--subsampling-factors", "-subSampling"}, description = "Subsampling level at each resolution level")
        String subSampling;
    }

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
                      @PropertyValue(name = "ANTS.Library.Path") String libraryPath,
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
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return true;
    }

    @Override
    protected Void retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
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

    private AntsToolArgs getArgs(JacsServiceData jacsServiceData) {
        AntsToolArgs args = new AntsToolArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private String getExecutable() {
        return getFullExecutableName(executable);
    }

}
