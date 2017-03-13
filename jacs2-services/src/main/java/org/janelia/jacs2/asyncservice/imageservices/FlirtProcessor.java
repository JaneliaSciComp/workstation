package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Named("flirt")
public class FlirtProcessor extends AbstractExeBasedServiceProcessor<Void> {

    static class FlirtArgs extends ServiceArgs {
        @Parameter(names = "-in", description = "Input volume")
        String inputVol;
        @Parameter(names = {"-o", "-out"}, description = "Output volume")
        String outputVol;
        @Parameter(names = "-init", description = "Input 4x4 affine matrix")
        String inputAffine;
        @Parameter(names = "-omat", description = "Output in 4x4 ascii format")
        String outputAffine;
        @Parameter(names = "-ref", description = "Reference volume")
        String referenceVol;
        @Parameter(names = "-datatype", description = "Output data type: {char,short,int,float,double}")
        String dataType;
        @Parameter(names = "-cost", description = "Cost: {mutualinfo,corratio,normcorr,normmi,leastsq,labeldiff,bbr}")
        String cost = "corratio";
        @Parameter(names = "-searchcost", description = "Search cost: {mutualinfo,corratio,normcorr,normmi,leastsq,labeldiff,bbr}")
        String searchCost = "corratio";
        @Parameter(names = "-usesqform", arity = 0, description = "(initialise using appropriate sform or qform)")
        boolean useSqForm;
        @Parameter(names = "-displayinit", arity = 0, description = "Display initial matrix")
        boolean displayInitialMatrix;
        @Parameter(names = "-anglerep", description = "{quaternion, euler}")
        String angleRep = "euler";
        @Parameter(names = "-interp", description = "Final interpolation: {trilinear,nearestneighbour,sinc,spline}")
        String interpolation = "trilinear";
        @Parameter(names = "-sincwidth", description = "Full-width in voxels")
        int width;
        @Parameter(names = "-sincwindow", description = "{rectangular,hanning,blackman}")
        String dincWindow;
        @Parameter(names = "-bins", description = "Number of histogram bins")
        int bins = 256;
        @Parameter(names = "-dof", description = "Number of transform dofs")
        int dofTransforms = 12;
        @Parameter(names = "-noresample", arity = 0, description = "do not change input sampling")
        boolean noResample;
        @Parameter(names = "-forcescaling", arity = 0, description = "force rescaling even for low-res images")
        boolean forceScaling;
        @Parameter(names = "-minsampling", description = "set minimum voxel dimension for sampling (in mm)")
        int voxDim;
        @Parameter(names = "-applyxfm", arity = 0, description = "(applies transform (no optimisation) - requires -init)")
        boolean applyXFm;
        @Parameter(names = "-applyisoxfm", description = "<scale> (as applyxfm but forces isotropic resampling)")
        boolean applyisoxfm;
        @Parameter(names = "-paddingsize", description = "<number of voxels> (for applyxfm: interpolates outside image by size)")
        int paddingSize;
        @Parameter(names = "-searchrx", arity = 2, description = "<min_angle> <max_angle>  (angles in degrees: default is -90 90)")
        List<Integer> searchRX = new ArrayList<>();
        @Parameter(names = "-searchry", arity = 2, description = "<min_angle> <max_angle>  (angles in degrees: default is -90 90)")
        List<Integer> searchRY = new ArrayList<>();
        @Parameter(names = "-searchrz", arity = 2, description = "<Min angle> and <max angle> in degrees; default is -90 90 ")
        List<Integer> searchRZ = new ArrayList<>();
        @Parameter(names = "-nosearch", description = "sets all angular search ranges to 0 0")
        boolean noSearch;
        @Parameter(names = "-coarsesearch", description = "Coarse search angle in degrees")
        int coarseSearch = 60;
        @Parameter(names = "-finesearch", description = "Fine search angle in degrees")
        int fineSearch = 18;
        @Parameter(names = "-schedule", description = "replaces default schedule")
        String schedule;
        @Parameter(names = "-refweight", description = "(use weights for reference volume)")
        int refweight;
        @Parameter(names = "-inweight", description = "(use weights for input volume)")
        int imweight;
        @Parameter(names = "-wmseg", description = "(white matter segmentation volume needed by BBR cost function)")
        int wmseg;
        @Parameter(names = "-wmcoords", description = "(white matter boundary coordinates for BBR cost function)")
        int vmcoords;
        @Parameter(names = "-wmnorms", description = "(white matter boundary normals for BBR cost function)")
        String wmNorms;
        @Parameter(names = "-fieldmap", description = "fieldmap image in rads/s - must be already registered to the reference image")
        String fieldMap;
        @Parameter(names = "-fieldmapmask", description = "mask for fieldmap image")
        String fieldMapMask;
        @Parameter(names = "-pedir", description = "phase encode direction of EPI - 1/2/3=x/y/z & -1/-2/-3=-x/-y/-z")
        String phaseEncodingDir;
        @Parameter(names = "-echospacing", description = "value of EPI echo spacing - units of seconds")
        int echoSpacing;
        @Parameter(names = "-bbrtype", description = "type of bbr cost function: signed [default], global_abs, local_abs")
        String bbrCostType = "signed";
        @Parameter(names = "-bbrslope", description = "value of bbr slope")
        int bbrSlope;
        @Parameter(names = "-setbackground", description = "use specified background value for points outside FOV")
        int background;
        @Parameter(names = "-noclamp", arity = 0, description = "do not use intensity clamping")
        boolean noClamp;
        @Parameter(names = "-noresampblur", arity = 0, description = "do not use blurring on downsampling")
        boolean noSampleBlur;
        @Parameter(names = "-2D", arity = 0, description = "use 2D rigid body mode - ignores dof")
        boolean use2D;
        @Parameter(names = "-verbose", description = "Verbosity level - 0 is least and default")
        int verbose = 0;
    }

    private final String executable;
    private final String libraryPath;

    @Inject
    FlirtProcessor(JacsServiceEngine jacsServiceEngine,
                   ServiceComputationFactory computationFactory,
                   JacsServiceDataPersistence jacsServiceDataPersistence,
                   @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                   @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                   @Any Instance<ExternalProcessRunner> serviceRunners,
                   @PropertyValue(name = "FLIRT.Bin.Path") String executable,
                   @PropertyValue(name = "FLIRT.Library.Path") String libraryPath,
                   Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.executable = executable;
        this.libraryPath = libraryPath;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new FlirtArgs());
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
        FlirtArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        createScript(jacsServiceData, args, externalScriptWriter);
        externalScriptWriter.close();
        return externalScriptCode;
    }

    private void createScript(JacsServiceData jacsServiceData, FlirtArgs args, ScriptWriter scriptWriter) {
        try {
            Path workingDir = getWorkingDirectory(jacsServiceData);
            X11Utils.setDisplayPort(workingDir.toString(), scriptWriter);
            scriptWriter.addWithArgs(getExecutable())
                    .addArgFlag("-in", args.inputVol)
                    .addArgFlag("-ref", args.referenceVol)
                    .addArgFlag("-out", args.outputVol)
                    .addArgFlag("-init", args.inputAffine)
                    .addArgFlag("-omat", args.outputAffine)
                    .endArgs("");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    private FlirtArgs getArgs(JacsServiceData jacsServiceData) {
        FlirtArgs args = new FlirtArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private String getExecutable() {
        return getFullExecutableName(executable);
    }

}
