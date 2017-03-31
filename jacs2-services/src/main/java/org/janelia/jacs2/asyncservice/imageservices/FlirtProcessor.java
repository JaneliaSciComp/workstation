package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractFileListServiceResultHandler;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Named("flirt")
public class FlirtProcessor extends AbstractExeBasedServiceProcessor<List<File>> {

    static class FlirtArgs extends ServiceArgs {
        @Parameter(names = {"-in", "-input"}, description = "Input volume")
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
        String sincWindow;
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
        @Parameter(names = "-fslOutputType", description = "FSL output type", required = false)
        String fslOutputType = "NIFTI_GZ";
    }

    private final String executable;
    private final String libraryPath;

    @Inject
    FlirtProcessor(ServiceComputationFactory computationFactory,
                   JacsServiceDataPersistence jacsServiceDataPersistence,
                   @Any Instance<ExternalProcessRunner> serviceRunners,
                   @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                   @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                   @PropertyValue(name = "FLIRT.Bin.Path") String executable,
                   @PropertyValue(name = "FLIRT.Library.Path") String libraryPath,
                   Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, serviceRunners, defaultWorkingDir, executablesBaseDir, logger);
        this.executable = executable;
        this.libraryPath = libraryPath;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new FlirtArgs());
    }

    @Override
    public ServiceResultHandler<List<File>> getResultHandler() {
        return new AbstractFileListServiceResultHandler() {
//            final String resultsPattern = "glob:**/*.{png,avi,mp4}";

            @Override
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                FlirtArgs args = getArgs(jacsServiceData);
                Path outputAffine = getOutputAffine(args, jacsServiceData);
                if (outputAffine != null && !outputAffine.toFile().exists()) {
                    return false;
                }
                Path outputVolume = getOutputVolume(args, jacsServiceData);
                if (outputVolume != null && !outputVolume.toFile().exists()) {
                    return false;
                }
                return true;
            }

            @Override
            public List<File> collectResult(JacsServiceData jacsServiceData) {
                FlirtArgs args = getArgs(jacsServiceData);
                List<File> results = new LinkedList<>();
                Path outputAffine = getOutputAffine(args, jacsServiceData);
                if (outputAffine != null) {
                    results.add(outputAffine.toFile());
                }
                Path outputVolume = getOutputVolume(args, jacsServiceData);
                if (outputVolume != null) {
                    results.add(outputVolume.toFile());
                }
                return results;
            }
        };
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        FlirtArgs args = getArgs(jacsServiceData);
        Path outputAffine = getOutputAffine(args, jacsServiceData);
        if (outputAffine != null) {
            try {
                Files.createDirectories(outputAffine.getParent());
            } catch (IOException e) {
                throw new ComputationException(jacsServiceData, e);
            }
        }
        Path outputVolume = getOutputVolume(args, jacsServiceData);
        if (outputVolume != null) {
            try {
                Files.createDirectories(outputVolume.getParent());
            } catch (IOException e) {
                throw new ComputationException(jacsServiceData, e);
            }
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        FlirtArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        createScript(args, externalScriptWriter);
        externalScriptWriter.close();
        return externalScriptCode;
    }

    private void createScript(FlirtArgs args, ScriptWriter scriptWriter) {
        scriptWriter.addWithArgs(getExecutable())
                .addArgFlag("-in", args.inputVol)
                .addArgFlag("-ref", args.referenceVol)
                .addArgFlag("-out", args.outputVol)
                .addArgFlag("-init", args.inputAffine)
                .addArgFlag("-omat", args.outputAffine)
                .addArgFlag("-datatype", args.dataType)
                .addArgFlag("-cost", args.cost)
                .addArgFlag("-searchcost", args.searchCost)
                .addArgFlag("-usesqform", args.useSqForm)
                .addArgFlag("-displayinit", args.displayInitialMatrix)
                .addArgFlag("-anglerep", args.angleRep)
                .addArgFlag("-interp", args.interpolation)
                .addArgFlag("-sincwidth", args.width)
                .addArgFlag("-sincwindow", args.sincWindow)
                .addArgFlag("-bins", args.bins)
                .addArgFlag("-dof", args.dofTransforms)
                .addArgFlag("-noresample", args.noResample)
                .addArgFlag("-forcescaling", args.forceScaling)
                .addArgFlag("-minsampling", args.voxDim)
                .addArgFlag("-applyxfm", args.applyXFm)
                .addArgFlag("-applyisoxfm", args.applyisoxfm)
                .addArgFlag("-paddingsize", args.paddingSize)
                .addArgFlag("-searchrx", args.searchRX, " ")
                .addArgFlag("-searchry", args.searchRY, " ")
                .addArgFlag("-searchrz", args.searchRZ, " ")
                .addArgFlag("-nosearch", args.noSearch)
                .addArgFlag("-coarsesearch", args.coarseSearch)
                .addArgFlag("-finesearch", args.fineSearch)
                .addArgFlag("-schedule", args.schedule)
                .addArgFlag("-refweight", args.refweight)
                .addArgFlag("-inweight", args.imweight)
                .addArgFlag("-wmseg", args.wmseg)
                .addArgFlag("-wmcoords", args.vmcoords)
                .addArgFlag("-wmnorms", args.wmNorms)
                .addArgFlag("-fieldmap", args.fieldMap)
                .addArgFlag("-fieldmapmask", args.fieldMapMask)
                .addArgFlag("-pedir", args.phaseEncodingDir)
                .addArgFlag("-echospacing", args.echoSpacing)
                .addArgFlag("-bbrtype", args.bbrCostType)
                .addArgFlag("-bbrslope", args.bbrSlope)
                .addArgFlag("-setbackground", args.background)
                .addArgFlag("-noclamp", args.noClamp)
                .addArgFlag("-noresampblur", args.noSampleBlur)
                .addArgFlag("-2D", args.use2D)
                .addArgFlag("-verbose", args.verbose)
                .endArgs("");
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        FlirtArgs args = getArgs(jacsServiceData);
        return ImmutableMap.of(
                DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath),
                "FSLOUTPUTTYPE", args.fslOutputType)
                ;
    }

    private FlirtArgs getArgs(JacsServiceData jacsServiceData) {
        FlirtArgs args = new FlirtArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private Path getOutputAffine(FlirtArgs args, JacsServiceData jacsServiceData) {
        if (StringUtils.isNotBlank(args.outputAffine)) {
            return getServicePath(args.outputAffine, jacsServiceData);
        } else {
            return null;
        }
    }

    private Path getOutputVolume(FlirtArgs args, JacsServiceData jacsServiceData) {
        if (StringUtils.isNotBlank(args.outputVol)) {
            return getServicePath(args.outputVol, jacsServiceData);
        } else {
            return null;
        }
    }

    private String getExecutable() {
        return getFullExecutableName(executable);
    }

}
