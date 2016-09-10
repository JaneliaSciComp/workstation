
package org.janelia.it.jacs.compute.service.tools;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.SimpleMultiFastaSplitterService;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.tools.TargetpTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.TargetpResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: April 15, 2010
 * Time: 10:57:28 AM
 * From jacs.properties
 * # Targetp
 Targetp.TargetpCmd=/usr/local/bin/targetp
 Targetp.Queue=-l medium
 Targetp.MaxEntriesPerJob=200
 */
public class TargetpService extends SubmitDrmaaJobService {

    public static final String TARGETP_CMD = SystemConfigurationProperties.getString("Targetp.TargetpCmd");
    public static final String TARGETP_QUEUE = SystemConfigurationProperties.getString("Targetp.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("Targetp.MaxEntriesPerJob");
    private static final String resultFilename = TargetpResultNode.BASE_OUTPUT_FILENAME;

    private List<File> inputFiles;
    private List<File> outputFiles;
    TargetpTask targetpTask;
    TargetpResultNode targetpResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    protected String getGridServicePrefixName() {
        return "targetp";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(targetpTask, writer);

        int configIndex = 1;
        File outputDir = new File(resultFileNode.getDirectoryPath());
        outputFiles = new ArrayList<File>();
        for (File inputFile : inputFiles) {
            File outputFile = new File(new File(outputDir, resultFilename).getAbsolutePath() + "." + configIndex);
            outputFiles.add(outputFile);
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
        configWriter.println(inputFile.getAbsolutePath());
        configWriter.println(outputFile.getAbsolutePath());
        configWriter.close();
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
        logger.info("setQueue = " + TARGETP_QUEUE);
        jt.setNativeSpecification(TARGETP_QUEUE);
    }

    private void createShellScript(TargetpTask targetpTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");

            String optionString = targetpTask.generateCommandOptions();

            String inputFileString = "\"$INPUTFILE\"";
            String outputFileString = "\"$OUTPUTFILE\"";

            String targetpStr = TARGETP_CMD + " " + optionString + " " + inputFileString + " > " + outputFileString;
            script.append(targetpStr).append("\n");

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File targetpResultFile = new File(targetpResultNode.getFilePathByTag(TargetpResultNode.TAG_TARGETP_OUTPUT));
            File targetpDir = new File(targetpResultNode.getDirectoryPath());
            List<File> filteredFiles = new ArrayList<File>();
            logger.info("postProcess for targetpTaskId=" + targetpTask.getObjectId() + " and resultNodeDir=" + targetpDir.getAbsolutePath());
            int index = 0;
            for (File f : outputFiles) {
                if (!f.exists()) {
                    throw new Exception("Could not locate expected output file=" + f.getAbsolutePath());
                }
                if (index == 0) {
                    filteredFiles.add(f);
                }
                else {
                    File filteredFile = new File(f.getAbsolutePath() + ".noheader");
                    createFilteredFile(f, filteredFile, "#");
                    filteredFiles.add(filteredFile);
                }
                index++;
            }
            FileUtil.concatFilesUsingSystemCall(filteredFiles, targetpResultFile);
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        targetpTask = getTargetpTask(processData);
        task = targetpTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this Targetp call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for TargetpTask=" + targetpTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        targetpResultNode = createResultFileNode();
        logger.info("Setting targetpResultNode=" + targetpResultNode.getObjectId() + " path=" + targetpResultNode.getDirectoryPath());
        resultFileNode = targetpResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);
        Long inputFastaNodeId = new Long(targetpTask.getParameter(TargetpTask.PARAM_fasta_input_node_id));
        FastaFileNode inputFastaNode = (FastaFileNode) computeDAO.getNodeById(inputFastaNodeId);
        if (inputFastaNode != null) {
            logger.info("Using inputNode=" + inputFastaNode.getObjectId() + " path=" + inputFastaNode.getDirectoryPath());
        }
        else {
            String errorMessage = "Unexpectedly received null inputFastaNode using inputFastaNodeId=" + inputFastaNodeId;
            logger.error(errorMessage);
            throw new Exception(errorMessage);
        }
        File inputFile = new File(inputFastaNode.getFastaFilePath());
        inputFiles = SimpleMultiFastaSplitterService.splitInputFileToList(processData, new File(inputFastaNode.getFastaFilePath()),
                DEFAULT_ENTRIES_PER_EXEC, logger);
        for (File file : inputFiles) {
            logger.info("Targetp taskId=" + targetpTask.getObjectId() + " using pre-split file=" + inputFile.getAbsolutePath() + " and post-split inputFile path=" + file.getAbsolutePath());
        }
        logger.info(this.getClass().getName() + " targetpTaskId=" + task.getObjectId() + " init() end");
    }

    private TargetpTask getTargetpTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null TargetpTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (TargetpTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getTargetpTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private TargetpResultNode createResultFileNode() throws Exception {
        TargetpResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof TargetpResultNode) {
                logger.info("Found already-extant targetpResultNode path=" + ((TargetpResultNode) node).getDirectoryPath());
                return (TargetpResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating TargetpResultNode with sessionName=" + sessionName);
        resultFileNode = new TargetpResultNode(task.getOwner(), task,
                "TargetpResultNode", "TargetpResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    private void createFilteredFile(File source, File target, String filter) throws Exception {
        if (sc == null) {
            sc = new SystemCall(logger);
        }
        String cmd = "cat " + source.getAbsolutePath() + " | grep -v \"" + filter + "\" > " + target.getAbsolutePath();
        int ev = sc.emulateCommandLine(cmd, true);
        if (ev != 0) {
            throw new Exception("System cmd returned non-zero exit state=" + cmd);
        }
    }

}