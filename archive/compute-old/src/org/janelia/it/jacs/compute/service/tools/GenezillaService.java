
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
import org.janelia.it.jacs.model.tasks.tools.GenezillaTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.GenezillaIsoFileNode;
import org.janelia.it.jacs.model.user_data.tools.GenezillaResultNode;
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
 * Date: Jun 28, 2010
 * Time: 3:00:05 PM
 * From jacs.properties
 * # Genezilla
 Genezilla.GenezillaCmd=/usr/local/devel/ANNOTATION/jorvis/opt/genezilla
 Genezilla.Queue=-l medium
 Genezilla.MaxEntriesPerJob=200
 */
public class GenezillaService extends SubmitDrmaaJobService {

    public static final String GENEZILLA_CMD = SystemConfigurationProperties.getString("Genezilla.GenezillaCmd");
    public static final String GENEZILLA_QUEUE = SystemConfigurationProperties.getString("Genezilla.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("Genezilla.MaxEntriesPerJob");
    private static final String resultFilename = GenezillaResultNode.BASE_OUTPUT_FILENAME;

    private List<File> inputFiles;
    private List<File> outputFiles;
    GenezillaTask genezillaTask;
    GenezillaIsoFileNode genezillaIsoFileNode;
    GenezillaResultNode genezillaResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    protected String getGridServicePrefixName() {
        return "genezilla";
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
        createShellScript(genezillaTask, writer);

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
        logger.info("setQueue = " + GENEZILLA_QUEUE);
        jt.setNativeSpecification(GENEZILLA_QUEUE);
    }

    private void createShellScript(GenezillaTask genezillaTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");

            String isoFile = genezillaIsoFileNode.getIsoFilePath();
            String optionString = genezillaTask.generateCommandOptions();

            String inputFileString = "\"$INPUTFILE\"";
            String outputFileString = "\"$OUTPUTFILE\"";

            String genezillaStr = GENEZILLA_CMD + " " + isoFile + " " + inputFileString +
                    " " + optionString + " > " + outputFileString;
            script.append(genezillaStr).append("\n");

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File genezillaResultFile = new File(genezillaResultNode.getFilePathByTag(GenezillaResultNode.TAG_GENEZILLA_OUTPUT));
            File genezillaDir = new File(genezillaResultNode.getDirectoryPath());

            logger.info("postProcess for genezillaTaskId=" + genezillaTask.getObjectId() + " and resultNodeDir=" + genezillaDir.getAbsolutePath());

            for (File f : outputFiles) {
                if (!f.exists()) {
                    throw new Exception("Could not locate expected output file=" + f.getAbsolutePath());
                }
            }
            FileUtil.concatFilesUsingSystemCall(outputFiles, genezillaResultFile);

        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        genezillaTask = getGenezillaTask(processData);
        task = genezillaTask;

        Long genezillaIsoFileNodeId = new Long(genezillaTask.getParameter(GenezillaTask.PARAM_iso_input_node_id));
        genezillaIsoFileNode = (GenezillaIsoFileNode) computeDAO.getNodeById(genezillaIsoFileNodeId);

        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this Genezilla call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for GenezillaTask=" + genezillaTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        genezillaResultNode = createResultFileNode();
        logger.info("Setting genezillaResultNode=" + genezillaResultNode.getObjectId() + " path=" + genezillaResultNode.getDirectoryPath());
        resultFileNode = genezillaResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);

        Long inputFastaNodeId = new Long(genezillaTask.getParameter(GenezillaTask.PARAM_fasta_input_node_id));
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
            logger.info("Genezilla taskId=" + genezillaTask.getObjectId() + " using pre-split file=" + inputFile.getAbsolutePath() + " and post-split inputFile path=" + file.getAbsolutePath());
        }
        logger.info(this.getClass().getName() + " genezillaTaskId=" + task.getObjectId() + " init() end");
    }

    private GenezillaTask getGenezillaTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null GenezillaTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (GenezillaTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getGenezillaTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private GenezillaResultNode createResultFileNode() throws Exception {
        GenezillaResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof GenezillaResultNode) {
                logger.info("Found already-extant genezillaResultNode path=" + ((GenezillaResultNode) node).getDirectoryPath());
                return (GenezillaResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating GenezillaResultNode with sessionName=" + sessionName);
        resultFileNode = new GenezillaResultNode(task.getOwner(), task,
                "GenezillaResultNode", "GenezillaResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }


}