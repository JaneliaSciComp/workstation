
package org.janelia.it.jacs.compute.service.tools;

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
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.tools.CddSearchTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.CddSearchResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: April 28, 2010
 * Time: 10:20:32 AM
 * From jacs.properties
 * # CddSearch
 CddSearch.MaxEval=1e-10
 CddSearch.OtherOpts=-m 8
 CddSearch.Db=/usr/local/db/cdd/cdd/current/cdd
 CddSearch.RpsBlastCmd=blast-2.2.15/bin/rpsblast
 CddSearch.DefaultFastaEntriesPerExec=100000
 CddSearch.Queue=-l default
 */
public class CddSearchService extends SubmitDrmaaJobService {

    public static String CDDSEARCH_TASK = "CDDSEARCH_TASK";
    public static final String ANNOTATION_INPUT_DATA_TYPE = "cdd";
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("CddSearch.DefaultFastaEntriesPerExec");

    private static final String resultFilename = CddSearchResultNode.BASE_OUTPUT_FILENAME;

    protected static String cddsearchMaxEval = SystemConfigurationProperties.getString("CddSearch.MaxEval");
    protected static String cddsearchOtherOpts = SystemConfigurationProperties.getString("CddSearch.OtherOpts");
    protected static String cddsearchDb = SystemConfigurationProperties.getString("CddSearch.Db");
    protected static String rpsBlastCmd = SystemConfigurationProperties.getString("Executables.ModuleBase")+
            SystemConfigurationProperties.getString("CddSearch.RpsBlastCmd");

    private static final String queueName = SystemConfigurationProperties.getString("CddSearch.Queue");

    private List<File> inputFiles;
    CddSearchTask cddsearchTask;
    CddSearchResultNode cddsearchResultNode;
    private String sessionName;
    String fileId;

    public static CddSearchTask createDefaultTask() {
        CddSearchTask task = new CddSearchTask();
        task.setParameter(CddSearchTask.PARAM_rpsblast_options, cddsearchOtherOpts);
        task.setParameter(CddSearchTask.PARAM_max_eval, cddsearchMaxEval);
        return task;
    }

    protected String getGridServicePrefixName() {
        return "cddsearch";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        CddSearchTask cddsearchTask = (CddSearchTask) task;

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(cddsearchTask, writer);

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

    private void createShellScript(CddSearchTask cddsearchTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");

            String maxEval = getEvalFromTask(cddsearchTask);
            String rpsBlastOptions = getRpsBlastOptionsFromTask(cddsearchTask);

            String inputFileString = "\"$INPUTFILE\"";
            String outputFileString = "\"$OUTPUTFILE\"";

            String rpsStr = rpsBlastCmd +
                    " -i " + inputFileString +
                    " -d " + cddsearchDb +
                    " " + rpsBlastOptions +
                    " -e " + maxEval +
                    " > " + outputFileString + ".cddsearch";
            script.append(rpsStr).append("\n");
            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File hitsFile = new File(cddsearchResultNode.getFilePathByTag(CddSearchResultNode.TAG_CDDSEARCH_OUTPUT));
            List<File> hitFileList = new ArrayList<File>();
            File cddsearchDir = new File(cddsearchResultNode.getDirectoryPath());
            logger.info("postProcess for cddsearchTaskId=" + cddsearchTask.getObjectId() + " and resultNodeDir=" + cddsearchDir.getAbsolutePath());
            File[] files = cddsearchDir.listFiles();
            for (File f : files) {
                if (f.getName().startsWith("cddsearch.") && (f.getName().endsWith(".cddsearch"))) {
                    hitFileList.add(f);
                }
            }
            FileUtil.concatFilesUsingSystemCall(hitFileList, hitsFile);

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
        cddsearchTask = getCddSearchTask(processData);
        task = cddsearchTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this CddSearch call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for cddsearchTask=" + cddsearchTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        cddsearchResultNode = createResultFileNode();
        logger.info("Setting cddsearchResultNode=" + cddsearchResultNode.getObjectId() + " path=" + cddsearchResultNode.getDirectoryPath());
        resultFileNode = cddsearchResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);
        // First check if we have been assigned a specific file to process. If so, then we will not
        // worry about its size, assuming this has already been configured.
        inputFiles = new ArrayList<File>();
        File inputFile = getCddSearchInputFile(processData);
        FastaFileNode inputFastaNode = null;
        if (inputFile == null) {
            // Since we have not been assigned a file, we will get the input node from the task
            logger.info("Could not locate inputFile from getCddSearchInputFile() therefore getting inputNode from task");
            Long inputFastaNodeId = new Long(cddsearchTask.getParameter(CddSearchTask.PARAM_input_fasta_node_id));
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
            logger.info("CddSearch taskId=" + cddsearchTask.getObjectId() + " using pre-split file=" + inputFile.getAbsolutePath() + " and post-split inputFile path=" + file.getAbsolutePath());
        }
        logger.info(this.getClass().getName() + " cddsearchTaskId=" + task.getObjectId() + " init() end");
    }

    private CddSearchTask getCddSearchTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Object possibleTask = processData.getItem(CDDSEARCH_TASK);
            if (possibleTask != null) {
                logger.info("CDDSEARCH_TASK from processData is not null and taskId=" + ((Task) possibleTask).getObjectId());
                taskId = ((Task) possibleTask).getObjectId();
            }
            else {
                logger.info("CDDSEARCH_TASK was returned null from ProcessData");
            }
            if (taskId == null) {
                // Attempt to get task from default IProcess location
                Task pdTask = ProcessDataHelper.getTask(processData);
                if (pdTask != null) {
                    logger.info("Found generic Task, possibly a non-null cddsearch task from ProcessData taskId=" + pdTask.getObjectId());
                    taskId = pdTask.getObjectId();
                }
            }
            if (taskId != null) {
                return (CddSearchTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getCddSearchTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private CddSearchResultNode createResultFileNode() throws Exception {
        CddSearchResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof CddSearchResultNode) {
                logger.info("Found already-extant cddsearchResultNode path=" + ((CddSearchResultNode) node).getDirectoryPath());
                return (CddSearchResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating CddSearchResultNode with sessionName=" + sessionName);
        resultFileNode = new CddSearchResultNode(task.getOwner(), task,
                "CddSearchResultNode", "CddSearchResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    private File getCddSearchInputFile(IProcessData processData) {
        try {
            File inputFile = (File) processData.getItem("MG_INPUT_ARRAY");
            if (inputFile != null) {
                logger.info("CddSearchService using MG_INPUT_ARRAY input file=" + inputFile.getAbsolutePath());
            }
            return inputFile;
        }
        catch (Exception e) {
            return null; // assume the value simply isn't in processData
        }
    }

    private String getEvalFromTask(CddSearchTask cddsearchTask) {
        String taskMaxEval = cddsearchTask.getParameter(CddSearchTask.PARAM_max_eval);
        if (taskMaxEval.contains("e") || taskMaxEval.contains("E")) {
            return taskMaxEval;
        }
        else {
            return ("1e" + taskMaxEval);
        }
    }

    private String getRpsBlastOptionsFromTask(CddSearchTask cddsearchTask) {
        String taskOptions = cddsearchTask.getParameter(CddSearchTask.PARAM_rpsblast_options);
        if (taskOptions.contains(cddsearchOtherOpts)) {
            return taskOptions;
        }
        else {
            return taskOptions + " " + cddsearchOtherOpts;
        }
    }

}