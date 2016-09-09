
package org.janelia.it.jacs.compute.service.metageno.orf;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.metageno.MetaGenoPerlConfig;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoOrfCallerTask;
import org.janelia.it.jacs.model.user_data.metageno.MetaGenoOrfCallerResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 19, 2009
 * Time: 4:32:48 PM
 * From jacs.properties
 * # Clear-range filter properties
 MgPipeline.ClearRangeMinOrfSize=180
 MgPipeline.ClearRangeFilterOrfCmd=clr_range_filter_orf.pl
 MgPipeline.ClearRangeFilterPepCmd=clr_range_filter_pep.pl
 MgPipeline.OrfToBtabCmd=cameragene2btab.pl
 MgPipeline.OrfOverlapCmd=report_camera_orf_overlaps.pl
 MgPipeline.OrfStatsCmd=camera_orf_stats.pl
 MgPipeline.OrfStatsPercentNInterval=5
 MgPipeline.OrfStatsLengthInterval=100
 MgPipeline.MetageneMapCmd=camera_orf_metagene_mapping.pl
 MgPipeline.MetagenePepFastaCmd=get_seq_no_gzip.pl
 MgOrfPipeline.Cleanup=false
 */
public class MetaGenoClrRangeFilterService implements IService {

    public static String CLR_DIR_EXTENSION = ".clrDir";

    /* SCRIPT DEPENDENCIES

        ClearRangeFilterOrfCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/clr_range_filter_orf.pl
            use lib '/usr/local/annotation/CAMERA/lib';
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev pass_through);
            use Pod::Usage;
            use File::Basename qw(basename fileparse);
            use Ergatis::IdGenerator;
        ClearRangeFilterPepCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/clr_range_filter_pep.pl
            use lib '/usr/local/annotation/CAMERA/lib';
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev pass_through);
            use Pod::Usage;
            use File::Basename qw(basename fileparse);
            use Ergatis::IdGenerator;

        MODULE SUMMARY
            Ergatis

     */

    private static String orfFilterCmd = SystemConfigurationProperties.getString("MgPipeline.ClearRangeFilterOrfCmd");
    private static String pepFilterCmd = SystemConfigurationProperties.getString("MgPipeline.ClearRangeFilterPepCmd");
    private static String minOrfSize = SystemConfigurationProperties.getString("MgPipeline.ClearRangeMinOrfSize");
    private static String scratchDirPath = SystemConfigurationProperties.getString("SystemCall.ScratchDir");

    MetaGenoOrfCallerResultNode resultNode;
    File resultNodeDir;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            resultNode = (MetaGenoOrfCallerResultNode) processData.getItem("META_GENO_ORF_CALLER_RESULT_NODE");
            File orfInputNucFile = (File) processData.getItem("SIMPLE_ORF_NT_OUTPUT_FILE");
            File orfInputPepFile = (File) processData.getItem("SIMPLE_ORF_AA_OUTPUT_FILE");
            if (orfInputNucFile == null) {
                logger.error(this.getClass().getName() + " received null orfInputNucFile");
            }
            else {
                logger.info(this.getClass().getName() + " started with orfInputNucFile=" + orfInputNucFile.getAbsolutePath());
            }
            if (orfInputPepFile == null) {
                logger.error(this.getClass().getName() + " received null orfInputPepFile");
            }
            else {
                logger.info(this.getClass().getName() + " started with orfInputPepFile=" + orfInputPepFile.getAbsolutePath());
            }
            MetaGenoOrfCallerTask parentTask = (MetaGenoOrfCallerTask) ProcessDataHelper.getTask(processData);
            if (parentTask == null) {
                throw new Exception("Could not get parent task for " + this.getClass().getName());
            }
            String useClearRange = parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_useClearRange);
            logger.info("useClearRange=" + useClearRange);
            int clearRangeMinOrfSize = new Integer(minOrfSize.trim());
            String clearRangeMinOrfSizeString = parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_clearRangeMinOrfSize);
            if (clearRangeMinOrfSizeString != null && clearRangeMinOrfSizeString.trim().length() > 0) {
                clearRangeMinOrfSize = new Integer(clearRangeMinOrfSizeString.trim());
                logger.info("using task parameter clearRangeMinOrfSize=" + clearRangeMinOrfSize);
            }
            else {
                logger.info("using default clearRangeMinOrfSize=" + clearRangeMinOrfSize);
            }
            logger.info(this.getClass().getName() + " execute() start");
            logger.info("Using result node directory=" + resultNode.getDirectoryPath());
            resultNodeDir = new File(resultNode.getDirectoryPath());
            File orfDir = new File(resultNodeDir, orfInputNucFile.getParentFile().getName() + "_orf" + CLR_DIR_EXTENSION);
            FileUtil.ensureDirExists(orfDir.getAbsolutePath());
            File pepDir = new File(resultNodeDir, orfInputPepFile.getParentFile().getName() + "_pep" + CLR_DIR_EXTENSION);
            FileUtil.ensureDirExists(pepDir.getAbsolutePath());

            if (parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_useClearRange).equals(Boolean.FALSE.toString())) {
                logger.info("skipping clrRangeFilter");
                // Skip this service and forward results
                processData.putItem("ORF_PEP_CLR_FAA_FILE", orfInputPepFile);
                processData.putItem("ORF_CLR_FNA_FILE", orfInputNucFile);
                return;
            }
            else {
                logger.info("continuing with clrRangeFilter");
            }

            // Orf filter
            String orfCmd = MetaGenoPerlConfig.getCmdPrefix() + orfFilterCmd +
                    " --input_file " + orfInputNucFile.getAbsolutePath() +
                    " --output_dir " + orfDir.getAbsolutePath() +
                    " --gzip_output 0" +
                    " --min_orf_size " + clearRangeMinOrfSize;
            File scratchDir = new File(scratchDirPath);
            logger.info("Using scratchDir=" + scratchDir.getAbsolutePath());
            SystemCall sc = new SystemCall(null, scratchDir, logger);
            int ev = sc.execute(orfCmd, false);
            //int ev=sc.emulateCommandLine(orfCmd, true);
            if (ev != 0) {
                throw new Exception("SystemCall produced non-zero exit value=" + orfCmd);
            }

            // Pep filter
            String pepCmd = MetaGenoPerlConfig.getCmdPrefix() + pepFilterCmd +
                    " --input_file " + orfInputPepFile.getAbsolutePath() +
                    " --output_dir " + pepDir.getAbsolutePath() +
                    " --gzip_output 0" +
                    " --min_orf_size " + clearRangeMinOrfSize;
            ev = sc.execute(pepCmd, false);
            //ev = sc.emulateCommandLine(pepCmd, true);
            if (ev != 0) {
                throw new Exception("SystemCall produced non-zero exit value=" + orfCmd);
            }

            // Retrieve faa clr file
            File orfPepClrFaaFile = null;
            File[] pepFiles = pepDir.listFiles();
            for (File f : pepFiles) {
                if (f.getName().endsWith(".clr_range.faa")) {
                    if (orfPepClrFaaFile != null) {
                        throw new Exception("Did not expect more than one clr_range.faa file in dir=" + pepDir.getAbsolutePath());
                    }
                    else {
                        orfPepClrFaaFile = f;
                    }
                }
            }
            if (orfPepClrFaaFile == null) {
                throw new Exception("Could not locate .clr_range.faa file in dir=" + pepDir.getAbsolutePath());
            }
            processData.putItem("ORF_PEP_CLR_FAA_FILE", orfPepClrFaaFile);

            // Retrieve fna clr file
            File orfClrFnaFile = null;
            File[] orfFiles = orfDir.listFiles();
            for (File f : orfFiles) {
                if (f.getName().endsWith(".clr_range.fna")) {
                    if (orfClrFnaFile != null) {
                        throw new Exception("Did not expect more than one clr_range.fna file in dir=" + orfDir.getAbsolutePath());
                    }
                    else {
                        orfClrFnaFile = f;
                    }
                }
            }
            if (orfClrFnaFile == null) {
                throw new Exception("Could not locate .clr_range.fna file in dir=" + orfDir.getAbsolutePath());
            }
            processData.putItem("ORF_CLR_FNA_FILE", orfClrFnaFile);

            sc.cleanup();

            logger.info(this.getClass().getName() + " execute() finish");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

}
