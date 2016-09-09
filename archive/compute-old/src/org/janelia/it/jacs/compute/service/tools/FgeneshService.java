
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
import org.janelia.it.jacs.model.tasks.tools.FgeneshTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.FgeneshResultNode;
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
 * Date: Jun 10, 2010
 * Time: 3:31:05 PM
 * From jacs.properties
 * # Fgenesh
 Fgenesh.FgeneshCmd=/usr/local/bin/fgenesh
 Fgenesh.Queue=-l medium
 Fgenesh.MaxEntriesPerJob=200
 */
public class FgeneshService extends SubmitDrmaaJobService {

    public static final String FGENESH_CMD = SystemConfigurationProperties.getString("Fgenesh.FgeneshCmd");
    public static final String FGENESH_QUEUE = SystemConfigurationProperties.getString("Fgenesh.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("Fgenesh.MaxEntriesPerJob");
    private static final String resultFilename = FgeneshResultNode.BASE_OUTPUT_FILENAME;

    private List<File> inputFiles;
    private List<File> outputFiles;
    FgeneshTask fgeneshTask;
    FgeneshResultNode fgeneshResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    protected String getGridServicePrefixName() {
        return "fgenesh";
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
        createShellScript(fgeneshTask, writer);

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
        try {
            // move the inputFile(s) to scratch to get around fgenesh's long filename phobia.
            File inputFileCopy = new File(SystemConfigurationProperties.getString("Scratch.Path") + File.separator +
                    fgeneshTask.getObjectId() + File.separator + inputFile.getName());
            File inputFileParent = new File(inputFileCopy.getParent());
            inputFileParent.mkdirs();
            FileUtil.copyFile(inputFile, inputFileCopy);

            configWriter.println(inputFileCopy.getAbsolutePath());
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
        logger.info("setQueue = " + FGENESH_QUEUE);
        jt.setNativeSpecification(FGENESH_QUEUE);
    }

    private void createShellScript(FgeneshTask fgeneshTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");

            String optionString = fgeneshTask.generateCommandOptions();

            String inputFileString = "\"$INPUTFILE\"";
            String outputFileString = "\"$OUTPUTFILE\"";

            String parFile = fgeneshTask.getParFile();

            String fgeneshStr = FGENESH_CMD + " " + parFile + " " + inputFileString + " " + optionString + " > " + outputFileString;
            script.append(fgeneshStr).append("\n");

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File fgeneshResultFile = new File(fgeneshResultNode.getFilePathByTag(FgeneshResultNode.TAG_FGENESH_OUTPUT));
            File fgeneshDir = new File(fgeneshResultNode.getDirectoryPath());
            List<File> filteredFiles = new ArrayList<File>();
            logger.info("postProcess for fgeneshTaskId=" + fgeneshTask.getObjectId() + " and resultNodeDir=" + fgeneshDir.getAbsolutePath());
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
            FileUtil.concatFilesUsingSystemCall(filteredFiles, fgeneshResultFile);
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        fgeneshTask = getFgeneshTask(processData);
        task = fgeneshTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this fgenesh call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for FgeneshTask=" + fgeneshTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        fgeneshResultNode = createResultFileNode();
        logger.info("Setting fgeneshResultNode=" + fgeneshResultNode.getObjectId() + " path=" + fgeneshResultNode.getDirectoryPath());
        resultFileNode = fgeneshResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);
        Long inputFastaNodeId = new Long(fgeneshTask.getParameter(FgeneshTask.PARAM_fasta_input_node_id));
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
            logger.info("Fgenesh taskId=" + fgeneshTask.getObjectId() + " using pre-split file=" + inputFile.getAbsolutePath() +
                    " and post-split inputFile path=" + file.getAbsolutePath());
        }
        logger.info(this.getClass().getName() + " fgeneshTaskId=" + task.getObjectId() + " init() end");
    }

    private FgeneshTask getFgeneshTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null FgeneshTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (FgeneshTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getFgeneshTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private FgeneshResultNode createResultFileNode() throws Exception {
        FgeneshResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof FgeneshResultNode) {
                logger.info("Found already-extant fgeneshResultNode path=" + ((FgeneshResultNode) node).getDirectoryPath());
                return (FgeneshResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating FgeneshResultNode with sessionName=" + sessionName);
        resultFileNode = new FgeneshResultNode(task.getOwner(), task,
                "FgeneshResultNode", "FgeneshResultNode for task " + task.getObjectId(),
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