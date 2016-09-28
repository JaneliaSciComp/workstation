
package org.janelia.it.jacs.compute.service.metageno.anno;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.SimpleMultiFastaSplitterService;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.metageno.MetaGenoPerlConfig;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.metageno.PriamTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.metageno.PriamResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 19, 2009
 * Time: 2:33:32 PM
 * From jacs.properties
 * # Priam
 Priam.DefaultFastaEntriesPerExec=100000
 Priam.Queue=-l default
 Priam.MaxEval=1e-10
 Priam.OtherOpts=-m 8
 Priam.Db=/usr/local/projects/CAMERA/db/priam_jun09_gene/ANNOTATION/priam_jun09_gene
 Priam.DeflineMap=/usr/local/projects/CAMERA/db/priam_jun09_gene/ANNOTATION/defline_map.txt
 Priam.ExpandPriamToEcCmd=expandPriToEcHitLines.pl
 Priam.Pssm=/usr/local/db/calit_db/PRIAM/blastpgp
 Priam.RpsBlastCmd=blast-2.2.15/bin/rpsblast
 Priam.CreateEcListCmd=create_ec_list.pl
 */
public class PriamEcService extends SubmitDrmaaJobService {

    public static String PRIAM_TASK = "PRIAM_TASK";
    public static final String ANNOTATION_INPUT_DATA_TYPE = "ECTable";
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("Priam.DefaultFastaEntriesPerExec");

    private static final String resultFilename = PriamResultNode.BASE_OUTPUT_FILENAME;

    /* SCRIPT DEPENDENCIES

        priExpandCmd=Xdb/priam_jun09_gene/bin/expandPriToEcHitLines.pl
            <none>
        ecListCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/create_ec_list
            use Blast::NcbiBlastHitDataType;
            use File::Basename;
            use IO::File;
            use Getopt::Long qw(:config no_ignore_case bundling);
        parserCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/camera_parse_annotation_results_to_text_table.pl
            use strict;
            use warnings;
            use Carp;
            use lib "/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/current/annotation_tool";
            use CAMERA::Parser::BTAB;
            use CAMERA::Parser::HTAB;
            use CAMERA::Parser::ECTable;
            use CAMERA::Parser::TMHMMBSML;
            use CAMERA::Parser::LipoproteinMotifBSML;
            use CAMERA::Parser::Hypothetical;
            use CAMERA::PolypeptideSet;
            #use DBM::Deep;
            use Getopt::Long;
            #use File::Copy;

        MODULE SUMMARY
            Blast, CAMERA

     */

    protected static String priamMaxEval = SystemConfigurationProperties.getString("Priam.MaxEval");
    protected static String priamOtherOpts = SystemConfigurationProperties.getString("Priam.OtherOpts");
    protected static String priamDb = SystemConfigurationProperties.getString("Priam.Db");
    protected static String priamPssm = SystemConfigurationProperties.getString("Priam.Pssm");
    protected static String rpsBlastCmd = SystemConfigurationProperties.getString("Executables.ModuleBase")+
            SystemConfigurationProperties.getString("Priam.RpsBlastCmd");
    protected static String ecListCmd = SystemConfigurationProperties.getString("Priam.CreateEcListCmd");
    protected static String parserCmd = SystemConfigurationProperties.getString("MgAnnotation.Parser");
    protected static String deflineMapFile = SystemConfigurationProperties.getString("Priam.DeflineMap");
    protected static String priExpandCmd = SystemConfigurationProperties.getString("Priam.ExpandPriamToEcCmd");

    private static final String queueName = SystemConfigurationProperties.getString("Priam.Queue");

    private List<File> inputFiles;
    PriamTask priamTask;
    PriamResultNode priamResultNode;
    private String sessionName;
    String fileId;

    public static PriamTask createDefaultTask() {
        PriamTask task = new PriamTask();
        task.setParameter(PriamTask.PARAM_rpsblast_options, priamOtherOpts);
        task.setParameter(PriamTask.PARAM_max_eval, priamMaxEval);
        return task;
    }

    protected String getGridServicePrefixName() {
        return "priam";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        PriamTask priamTask = (PriamTask) task;

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(priamTask, writer);

        int configIndex = 1;
        File outputDir = new File(resultFileNode.getDirectoryPath());
        for (File inputFile : inputFiles) {
            File outputFile = new File(new File(outputDir, resultFilename).getAbsolutePath() + "." + configIndex);
            configIndex = writeConfigFile(inputFile, outputFile, configIndex);
            configIndex++;
        }

        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);
    }

    private int writeConfigFile(File inputFile, File outputFile, int configIndex) throws IOException {
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }
        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {
            configWriter.println(inputFile.getAbsolutePath());
            configWriter.println(outputFile.getAbsolutePath());
        }
        finally {
            configWriter.close();
        }
        return configIndex;
    }

    private class ConfigurationFileFilter implements FilenameFilter {
        public ConfigurationFileFilter() {
        }

        public boolean accept(File dir, String name) {
            return name != null && name.startsWith(getConfigPrefix());
        }
    }

    private String buildConfigFileName(int configIndex) {
        return getConfigPrefix() + configIndex;
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("setQueue = " + queueName);
        jt.setNativeSpecification(queueName);
    }

    private void createShellScript(PriamTask priamTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");

            String maxEval = getEvalFromTask(priamTask);
            String rpsBlastOptions = getRpsBlastOptionsFromTask(priamTask);

            String inputFileString = "\"$INPUTFILE\"";
            String outputFileString = "\"$OUTPUTFILE\"";

            // Step 1: rpsblast
            String rpsStr = rpsBlastCmd +
                    " -i " + inputFileString +
                    " -d " + priamDb +
                    " " + rpsBlastOptions +
                    " -e " + maxEval +
                    " > " + outputFileString + ".priam";
            script.append(rpsStr).append("\n");

            // Step 2: expand priam assignments to EC values
            String expandStr = MetaGenoPerlConfig.getCmdPrefix() + priExpandCmd +
                    " -m " + deflineMapFile +
                    " -i " + outputFileString + ".priam" +
                    " -o " + outputFileString;
            script.append(expandStr).append("\n");

            // Step 3: create ec list
            String ecStr = MetaGenoPerlConfig.getCmdPrefix() + ecListCmd +
                    " --rps " +
                    " --hits " + outputFileString +
                    " --output " + outputFileString + ".ectab";
            script.append(ecStr).append("\n");

            // Step 4: parse btab
            String parseDirStr = outputFileString + "_priamParseDir";
            String mkParseDirStr = "mkdir " + parseDirStr;
            script.append(mkParseDirStr).append("\n");
            String parsedFileStr = outputFileString + ".ectab.parseJobOutput";
            String parserStr = MetaGenoPerlConfig.getCmdPrefix() + parserCmd +
                    " --input_file " + outputFileString + ".ectab" +
                    " --input_type " + ANNOTATION_INPUT_DATA_TYPE + " " +
                    " --output_file " + parsedFileStr +
                    " --work_dir " + parseDirStr;
            script.append(parserStr).append("\n");

            //document each step
            logger.debug(getClass().getName()+"\n"+
                        "PriamEcService Summary"                 +"\n"+
                        "Step 1 Run RPS Blast:"                 +rpsStr+"\n"+
                        "Step 2 Expand Results to EC:"          +expandStr+"\n"+
                        "Step 3 Create EC list:"                +ecStr+"\n"+
                        "Step 4 Parse BTAB"                     +parserStr+"\n"
            );            

            // Step 5: clean parse directory
            String cleanStr = "rm -r " + parseDirStr;
            script.append(cleanStr).append("\n");

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File parsedFile = new File(priamResultNode.getFilePathByTag(PriamResultNode.TAG_PRIAM_EC_HIT_TAB_PARSED_FILE));
            File ectabFile = new File(priamResultNode.getFilePathByTag(PriamResultNode.TAG_PRIAM_EC_HIT_TAB_FILE));
            File hitsFile = new File(priamResultNode.getFilePathByTag(PriamResultNode.TAG_PRIAM_EC_HIT_FILE));
            List<File> parsedFileList = new ArrayList<File>();
            List<File> ectabFileList = new ArrayList<File>();
            List<File> hitFileList = new ArrayList<File>();
            File priamDir = new File(priamResultNode.getDirectoryPath());
            logger.info("postProcess for priamTaskId=" + priamTask.getObjectId() + " and resultNodeDir=" + priamDir.getAbsolutePath());
            File[] files = priamDir.listFiles();
            for (File f : files) {
                if (f.getName().endsWith(".ectab")) {
                    ectabFileList.add(f);
                }
                else if (f.getName().endsWith(".parseJobOutput")) {
                    logger.info("Adding parsed job output file=" + f.getAbsolutePath());
                    parsedFileList.add(f);
                }
                else if (f.getName().startsWith("priamEc.") && (!f.getName().endsWith(".priam"))) {
                    hitFileList.add(f);
                }
            }
            FileUtil.concatFilesUsingSystemCall(ectabFileList, ectabFile);
            FileUtil.concatFilesUsingSystemCall(hitFileList, hitsFile);
            FileUtil.concatFilesUsingSystemCall(parsedFileList, parsedFile);

            if (fileId != null) {
                logger.info("Calling MgAnnoBaseService.addParsedFile parsedFile=" + parsedFile.getAbsolutePath() + " fileId=" + fileId);
                MgAnnoBaseService.addParsedFile(parsedFile, fileId);
            }

        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        if (processData.getItem("MGA_FILE_ID") != null) {
            fileId = (String) processData.getItem("MGA_FILE_ID");
            logger.info("init() registering MGA_FILE_ID as fileId=" + fileId);
        }
        priamTask = getPriamTask(processData);
        task = priamTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this Priam call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for priamTask=" + priamTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        priamResultNode = createResultFileNode();
        logger.info("Setting priamResultNode=" + priamResultNode.getObjectId() + " path=" + priamResultNode.getDirectoryPath());
        resultFileNode = priamResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);
        // First check if we have been assigned a specific file to process. If so, then we will not
        // worry about its size, assuming this has already been configured.
        inputFiles = new ArrayList<File>();
        File inputFile = getPriamInputFile(processData);
        FastaFileNode inputFastaNode = null;
        if (inputFile == null) {
            // Since we have not been assigned a file, we will get the input node from the task
            logger.info("Could not locate inputFile from getPriamInputFile() therefore getting inputNode from task");
            Long inputFastaNodeId = new Long(priamTask.getParameter(PriamTask.PARAM_input_fasta_node_id));
            inputFastaNode = (FastaFileNode) computeDAO.getNodeById(inputFastaNodeId);
            if (inputFastaNode != null) {
                logger.info("Using inputNode=" + inputFastaNode.getObjectId() + " path=" + inputFastaNode.getDirectoryPath());
            }
            else {
                String errorMessage = "Unexpectedly received null inputFastaNode using inputFastaNodeId=" + inputFastaNodeId;
                logger.error(errorMessage);
                throw new Exception(errorMessage);
            }
            inputFile = new File(inputFastaNode.getFastaFilePath());
        }
        if (inputFastaNode == null) {
            inputFastaNode = new FastaFileNode(task.getOwner(), task, inputFile.getName(), "copy of " + inputFile.getAbsolutePath(), FastaFileNode.VISIBILITY_PRIVATE,
                    FastaFileNode.PEPTIDE, 1, sessionName);
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            inputFastaNode = (FastaFileNode) computeBean.saveOrUpdateNode(inputFastaNode);
            FileUtil.ensureDirExists(inputFastaNode.getDirectoryPath());
            FileUtil.copyFile(inputFile.getAbsolutePath(), inputFastaNode.getFastaFilePath());
        }
        inputFiles = SimpleMultiFastaSplitterService.splitInputFileToList(processData, new File(inputFastaNode.getFastaFilePath()),
                DEFAULT_ENTRIES_PER_EXEC, logger);
        for (File file : inputFiles) {
            logger.info("Priam taskId=" + priamTask.getObjectId() + " using pre-split file=" + inputFile.getAbsolutePath() + " and post-split inputFile path=" + file.getAbsolutePath());
        }
        logger.info(this.getClass().getName() + " priamTaskId=" + task.getObjectId() + " init() end");
    }

    private PriamTask getPriamTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Object possibleTask = processData.getItem(PRIAM_TASK);
            if (possibleTask != null) {
                logger.info("PRIAM_TASK from processData is not null and taskId=" + ((Task) possibleTask).getObjectId());
                taskId = ((Task) possibleTask).getObjectId();
            }
            else {
                logger.info("PRIAM_TASK was returned null from ProcessData");
            }
            if (taskId == null) {
                // Attempt to get task from default IProcess location
                Task pdTask = ProcessDataHelper.getTask(processData);
                if (pdTask != null) {
                    logger.info("Found generic Task, possibly a non-null priam task from ProcessData taskId=" + pdTask.getObjectId());
                    taskId = pdTask.getObjectId();
                }
            }
            if (taskId != null) {
                return (PriamTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getPriamTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private PriamResultNode createResultFileNode() throws Exception {
        PriamResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof PriamResultNode) {
                logger.info("Found already-extant priamResultNode path=" + ((PriamResultNode) node).getDirectoryPath());
                return (PriamResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating PriamResultNode with sessionName=" + sessionName);
        resultFileNode = new PriamResultNode(task.getOwner(), task,
                "PriamResultNode", "PriamResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    private File getPriamInputFile(IProcessData processData) {
        try {
            File inputFile = (File) processData.getItem("MG_INPUT_ARRAY");
            if (inputFile != null) {
                logger.info("PriamService using MG_INPUT_ARRAY input file=" + inputFile.getAbsolutePath());
            }
            return inputFile;
        }
        catch (Exception e) {
            return null; // assume the value simply isn't in processData
        }
    }

    private String getEvalFromTask(PriamTask priamTask) {
        String taskMaxEval = priamTask.getParameter(PriamTask.PARAM_max_eval);
        if (taskMaxEval.contains("e") || taskMaxEval.contains("E")) {
            return taskMaxEval;
        }
        else {
            return ("1e" + taskMaxEval);
        }
    }

    private String getRpsBlastOptionsFromTask(PriamTask priamTask) {
        String taskOptions = priamTask.getParameter(PriamTask.PARAM_rpsblast_options);
        if (taskOptions.contains(priamOtherOpts)) {
            return taskOptions;
        }
        else {
            return taskOptions + " " + priamOtherOpts;
        }
    }

}
