
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
import org.janelia.it.jacs.model.tasks.tools.AugustusTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.AugustusResultNode;
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
 * Date: Jul 1, 2010
 * Time: 2:00:05 PM
 * From jacs.properties
 * # Augustus
 Augustus.AugustusCmd=/usr/local/bin/augustus
 Augustus.ConfigPath=/usr/local/packages/augustus/config
 Augustus.Queue=-l medium
 Augustus.MaxEntriesPerJob=200
 */
public class AugustusService extends SubmitDrmaaJobService {

    public static final String AUGUSTUS_CMD = SystemConfigurationProperties.getString("Augustus.AugustusCmd");
    public static final String AUGUSTUS_QUEUE = SystemConfigurationProperties.getString("Augustus.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("Augustus.MaxEntriesPerJob");

    private List<File> inputFiles;
    private List<File> outputFiles;
    AugustusTask augustusTask;
    AugustusResultNode augustusResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    protected String getGridServicePrefixName() {
        return "augustus";
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
        createShellScript(augustusTask, writer);

        int configIndex = 1;
        File outputDir = new File(resultFileNode.getDirectoryPath());
        outputFiles = new ArrayList<File>();
        for (File inputFile : inputFiles) {
            // Interestingly, I have to hardcode "augustus" in the line below.  Try changing it to 'resultFilename'
            // for a peculiar behavior.  (In my case, it was being set to 'signalp')
            File outputFile = new File(new File(outputDir, "augustus").getAbsolutePath() + "." + configIndex);
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
        logger.info("setQueue = " + AUGUSTUS_QUEUE);
        jt.setNativeSpecification(AUGUSTUS_QUEUE);
    }

    private void createShellScript(AugustusTask augustusTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");

            String optionString = augustusTask.generateCommandOptions();

            String inputFileString = "\"$INPUTFILE\"";
            String outputFileString = "\"$OUTPUTFILE\"";

            String augustusStr = AUGUSTUS_CMD + " " + optionString + " " + inputFileString + " > " + outputFileString;
            script.append(augustusStr).append("\n");

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File augustusResultFile = new File(augustusResultNode.getFilePathByTag(AugustusResultNode.TAG_AUGUSTUS_GFF));
            File augustusDir = new File(augustusResultNode.getDirectoryPath());
            List<File> gffFiles = new ArrayList<File>();
            logger.info("postProcess for augustusTaskId=" + augustusTask.getObjectId() + " and resultNodeDir=" + augustusDir.getAbsolutePath());

            for (File f : outputFiles) {
                if (!f.exists()) {
                    throw new Exception("Could not locate expected output file=" + f.getAbsolutePath());
                }
                gffFiles.add(f);
            }
            FileUtil.concatFilesUsingSystemCall(gffFiles, augustusResultFile);
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        augustusTask = getAugustusTask(processData);
        task = augustusTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this Priam call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for AugustusTask=" + augustusTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        augustusResultNode = createResultFileNode();
        logger.info("Setting augustusResultNode=" + augustusResultNode.getObjectId() + " path=" + augustusResultNode.getDirectoryPath());
        resultFileNode = augustusResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);
        Long inputFastaNodeId = new Long(augustusTask.getParameter(AugustusTask.PARAM_fasta_input_node_id));
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
            logger.info("Augustus taskId=" + augustusTask.getObjectId() + " using pre-split file=" + inputFile.getAbsolutePath() + " and post-split inputFile path=" + file.getAbsolutePath());
        }
        logger.info(this.getClass().getName() + " augustusTaskId=" + task.getObjectId() + " init() end");
    }

    private AugustusTask getAugustusTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null AugustusTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (AugustusTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getAugustusTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private AugustusResultNode createResultFileNode() throws Exception {
        AugustusResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof AugustusResultNode) {
                logger.info("Found already-extant augustusResultNode path=" + ((AugustusResultNode) node).getDirectoryPath());
                return (AugustusResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating AugustusResultNode with sessionName=" + sessionName);
        resultFileNode = new AugustusResultNode(task.getOwner(), task,
                "AugustusResultNode", "AugustusResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }


}