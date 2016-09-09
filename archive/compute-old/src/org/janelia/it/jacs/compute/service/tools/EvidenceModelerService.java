
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
import org.janelia.it.jacs.model.tasks.tools.EvidenceModelerTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.EvidenceModelerResultNode;
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
 * Date: Jul16, 2010
 * Time: 11:22:22 AM
 * From jacs.properties
 * # EvidenceModeler
 EvidenceModeler.EvidenceModelerCmd=/usr/local/packages/EVM/evidence_modeler.pl
 EvidenceModeler.PartitioningCmd=/usr/local/packages/EVM/EvmUtils/partition_EVM_inputs.pl
 EvidenceModeler.CmdWritingCmd=/usr/local/packages/EVM/EvmUtils/write_EVM_commands.pl
 EvidenceModeler.CmdRunningCmd=/usr/local/packages/EVM/EvmUtils/execute_EVM_commands.pl
 EvidenceModeler.CombiningCmd=/usr/local/packages/EVM/EvmUtils/recombine_EVM_partial_outputs.pl
 EvidenceModeler.GffConversionCmd=/usr/local/packages/EVM/EvmUtils/convert_EVM_outputs_to_GFF3.pl
 EvidenceModeler.Queue=-l medium
 EvidenceModeler.MaxEntriesPerJob=1
 */
public class EvidenceModelerService extends SubmitDrmaaJobService {

    public static final String PARTITIONING_CMD = SystemConfigurationProperties.getString("EvidenceModeler.PartitioningCmd");
    public static final String CMDWRITER_CMD = SystemConfigurationProperties.getString("EvidenceModeler.CmdWritingCmd");
    public static final String CMDRUNNING_CMD = SystemConfigurationProperties.getString("EvidenceModeler.CmdRunningCmd");
    public static final String COMBINING_CMD = SystemConfigurationProperties.getString("EvidenceModeler.CombiningCmd");
    public static final String GFFCONVERSION_CMD = SystemConfigurationProperties.getString("EvidenceModeler.GffConversionCmd");


    public static final String EVIDENCEMODELER_QUEUE = SystemConfigurationProperties.getString("EvidenceModeler.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("EvidenceModeler.MaxEntriesPerJob");

    private List<File> inputFiles;
    EvidenceModelerTask evidenceModelerTask;
    EvidenceModelerResultNode evidenceModelerResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    protected String getGridServicePrefixName() {
        return "evidenceModeler";
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
        createShellScript(evidenceModelerTask, writer);

        int configIndex = 1;
        for (File inputFile : inputFiles) {
            configIndex = writeConfigFile(inputFile, configIndex);
            configIndex++;
        }

        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);
    }

    private int writeConfigFile(File inputFile, int configIndex) throws IOException {
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }
        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {
            configWriter.println(inputFile.getAbsolutePath());
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
        logger.info("setQueue = " + EVIDENCEMODELER_QUEUE);
        jt.setNativeSpecification(EVIDENCEMODELER_QUEUE);
    }

    private void createShellScript(EvidenceModelerTask evidenceModelerTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            String inputFileString = "\"$INPUTFILE\"";

            // Generate partitioning command:
            String evmPartitionOptions = evidenceModelerTask.generatePartitionCommandOptions();
            String evmPartitionFile = resultFileNode.getDirectoryPath() + File.separator + "partitions.list";
            String evmPartitionStr = PARTITIONING_CMD + " --genome " + inputFileString + " " + evmPartitionOptions +
                    " --partition_listing " + evmPartitionFile;
            script.append(evmPartitionStr);
            script.append("\n");

            // Generate write_commands command:
            String evmWriteCmdOptions = evidenceModelerTask.generateWriteCmdOptions();
            String evmCommandsFile = resultFileNode.getDirectoryPath() + File.separator + "commands.list";
            String evmWriteCmdStr = CMDWRITER_CMD + " --genome " + inputFileString + " " + evmWriteCmdOptions +
                    " --output_file_name evm.out --partitions " + evmPartitionFile + " > " + evmCommandsFile;
            script.append(evmWriteCmdStr);
            script.append("\n");

            // Generate run_commands command:
            String evmRunCmdStr = CMDRUNNING_CMD + " " + evmCommandsFile;
            script.append(evmRunCmdStr);
            script.append("\n");

            // Generate combine_results command:
            String evmCombineResultsStr = COMBINING_CMD + " --partitions " + evmPartitionFile +
                    " --output_file_name evm.out";
            script.append(evmCombineResultsStr);
            script.append("\n");

            // Generate gff_conversion command:
            String evmGffConversionStr = GFFCONVERSION_CMD + " --partitions " + evmPartitionFile + " --genome " +
                    inputFileString + " --output_file_name evm.out";
            script.append(evmGffConversionStr);

            // Write it all out
            writer.write(script.toString());

        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {

            File evidenceModelerOutputFile = new File(evidenceModelerResultNode.getFilePathByTag(EvidenceModelerResultNode.TAG_EVIDENCEMODELER_OUTPUT));
            File evidenceModelerGffFile = new File(evidenceModelerResultNode.getFilePathByTag(EvidenceModelerResultNode.TAG_EVIDENCEMODELER_OUTPUTGFF));
            File evidenceModelerDir = new File(evidenceModelerResultNode.getDirectoryPath());

            logger.info("postProcess for evidenceModelerTaskId=" + evidenceModelerTask.getObjectId() + " and resultNodeDir=" + evidenceModelerDir.getAbsolutePath());

            // Retrieve and catenate output gff files from within the subdirectories
            ArrayList<File> gffFiles = new ArrayList<File>();
            findFilesByTag(new File(resultFileNode.getDirectoryPath()), EvidenceModelerResultNode.TAG_EVIDENCEMODELER_OUTPUTGFF, gffFiles, 0, 2);
            FileUtil.concatFilesUsingSystemCall(gffFiles, evidenceModelerGffFile);

            // Retrieve and catenate output evm.out files from the subdirectories
            ArrayList<File> outputFiles = new ArrayList<File>();
            findFilesByTag(new File(resultFileNode.getDirectoryPath()), EvidenceModelerResultNode.TAG_EVIDENCEMODELER_OUTPUT, outputFiles, 0, 2);
            FileUtil.concatFilesUsingSystemCall(outputFiles, evidenceModelerOutputFile);

        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    /*  Grab all files in a given directory ending with a given String and add matching files to a given list
        Don't go past the specified depth.
    */
    public void findFilesByTag(File dir, final String searchTag, List<File> fileList, int depth, int maxdepth) {
        depth++;
        if (!(depth > maxdepth)) {

            logger.info(depth);
            // filter is based on searchTag
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(searchTag);
                }
            };
            // Add the files to our List of files
            String[] matches = dir.list(filter);
            for (String f : matches) {
                fileList.add(new File(new File(dir.getAbsolutePath() + File.separator + f).getAbsolutePath()).getAbsoluteFile());
            }

            // Check all the name here.  Visit any directories.
            String[] children = dir.list();
            for (String f : children) {
                File g = new File(new File(dir.getAbsolutePath() + File.separator + f).getAbsolutePath());
                if (g.isDirectory()) {
                    findFilesByTag(g, searchTag, fileList, depth, maxdepth);
                }
            }
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        evidenceModelerTask = getEvidenceModelerTask(processData);
        task = evidenceModelerTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this EVM call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for EvidenceModelerTask=" + evidenceModelerTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        evidenceModelerResultNode = createResultFileNode();
        logger.info("Setting evidenceModelerResultNode=" + evidenceModelerResultNode.getObjectId() + " path=" + evidenceModelerResultNode.getDirectoryPath());
        resultFileNode = evidenceModelerResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);
        Long inputFastaNodeId = new Long(evidenceModelerTask.getParameter(EvidenceModelerTask.PARAM_fastaInputNodeId));
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
            logger.info("EvidenceModeler taskId=" + evidenceModelerTask.getObjectId() + " using pre-split file=" + inputFile.getAbsolutePath() + " and post-split inputFile path=" + file.getAbsolutePath());
        }
        logger.info(this.getClass().getName() + " evidenceModelerTaskId=" + task.getObjectId() + " init() end");
    }

    private EvidenceModelerTask getEvidenceModelerTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null EvidenceModelerTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (EvidenceModelerTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getEvidenceModelerTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private EvidenceModelerResultNode createResultFileNode() throws Exception {
        EvidenceModelerResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof EvidenceModelerResultNode) {
                logger.info("Found already-extant evidenceModelerResultNode path=" + ((EvidenceModelerResultNode) node).getDirectoryPath());
                return (EvidenceModelerResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating EvidenceModelerResultNode with sessionName=" + sessionName);
        resultFileNode = new EvidenceModelerResultNode(task.getOwner(), task,
                "EvidenceModelerResultNode", "EvidenceModelerResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

}
