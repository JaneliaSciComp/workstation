
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
import org.janelia.it.jacs.model.tasks.tools.RepeatMaskerTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.RepeatMaskerResultNode;
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
 * Date: Jun 14, 2010
 * Time: 12:50:05 PM
 * From jacs.properties
 * # RepeatMasker
 RepeatMasker.RepeatMaskerCmd=/usr/local/bin/RepeatMasker
 RepeatMasker.Queue=-l medium
 RepeatMasker.MaxEntriesPerJob=200
 */
public class RepeatMaskerService extends SubmitDrmaaJobService {

    public static final String REPEATMASKER_CMD = SystemConfigurationProperties.getString("RepeatMasker.RepeatMaskerCmd");
    public static final String REPEATMASKER_QUEUE = SystemConfigurationProperties.getString("RepeatMasker.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("RepeatMasker.MaxEntriesPerJob");

    private static final String resultFilename = RepeatMaskerResultNode.BASE_OUTPUT_FILENAME;

    private List<File> inputFiles;
    private List<File> outputFiles;
    RepeatMaskerTask repeatMaskerTask;
    RepeatMaskerResultNode repeatMaskerResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    protected String getGridServicePrefixName() {
        return "repeatMasker";
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
        createShellScript(repeatMaskerTask, writer);

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
        logger.info("setQueue = " + REPEATMASKER_QUEUE);
        jt.setNativeSpecification(REPEATMASKER_QUEUE);
    }

    private void createShellScript(RepeatMaskerTask repeatMaskerTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");

            String optionString = repeatMaskerTask.generateCommandOptions(resultFileNode.getDirectoryPath());

            String inputFileString = "\"$INPUTFILE\"";
            String outputFileString = "\"$OUTPUTFILE\"";

            String repeatMaskerStr = REPEATMASKER_CMD + " " + optionString + " " + inputFileString + " > " + outputFileString;
            script.append(repeatMaskerStr).append("\n");

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {

            File repeatMaskerResultFileStdout = new File(repeatMaskerResultNode.getFilePathByTag(RepeatMaskerResultNode.TAG_REPEATMASKER_STDOUT));
            File repeatMaskerResultFileStderr = new File(repeatMaskerResultNode.getFilePathByTag(RepeatMaskerResultNode.TAG_REPEATMASKER_STDERR));
            File repeatMaskerResultFileMasked = new File(repeatMaskerResultNode.getFilePathByTag(RepeatMaskerResultNode.TAG_REPEATMASKER_MASKED));
            File repeatMaskerResultFileAlign = new File(repeatMaskerResultNode.getFilePathByTag(RepeatMaskerResultNode.TAG_REPEATMASKER_ALIGN));
            File repeatMaskerResultFileOut = new File(repeatMaskerResultNode.getFilePathByTag(RepeatMaskerResultNode.TAG_REPEATMASKER_OUT));
            File repeatMaskerResultFileCat = new File(repeatMaskerResultNode.getFilePathByTag(RepeatMaskerResultNode.TAG_REPEATMASKER_CAT));
            File repeatMaskerResultFileTbl = new File(repeatMaskerResultNode.getFilePathByTag(RepeatMaskerResultNode.TAG_REPEATMASKER_TBL));

            File repeatMaskerDir = new File(repeatMaskerResultNode.getDirectoryPath());

            List<File> stdoutFiles = new ArrayList<File>();
            List<File> stderrFiles = new ArrayList<File>();
            List<File> maskedFiles = new ArrayList<File>();
            List<File> alignFiles = new ArrayList<File>();
            List<File> outFiles = new ArrayList<File>();
            List<File> catFiles = new ArrayList<File>();
            List<File> tblFiles = new ArrayList<File>();

            logger.info("postProcess for repeatMaskerTaskId=" + repeatMaskerTask.getObjectId() + " and resultNodeDir=" + repeatMaskerDir.getAbsolutePath());
            int index = 0;
            // Post process all the other output files
            for (File f : inputFiles) {
                if (!f.exists()) {
                    throw new Exception("Could not locate " + f.getAbsolutePath());
                }

                File stderrF = new File(repeatMaskerDir + File.separator + f.getName() + ".stderr");
                File maskedF = new File(repeatMaskerDir + File.separator + f.getName() + ".masked");
                File alignF = new File(repeatMaskerDir + File.separator + f.getName() + ".align");
                File outF = new File(repeatMaskerDir + File.separator + f.getName() + ".out");
                File catF = new File(repeatMaskerDir + File.separator + f.getName() + ".cat");
                File tblF = new File(repeatMaskerDir + File.separator + f.getName() + ".tbl");

                if (!stderrF.exists()) {
                    throw new Exception("Could not locate " + stderrF.getAbsolutePath());
                }
                if (!maskedF.exists()) {
                    throw new Exception("Could not locate " + maskedF.getAbsolutePath());
                }
                if (!outF.exists()) {
                    throw new Exception("Could not locate " + outF.getAbsolutePath());
                }
                if (!catF.exists()) {
                    throw new Exception("Could not locate " + catF.getAbsolutePath());
                }
                if (!tblF.exists()) {
                    throw new Exception("Could not locate " + tblF.getAbsolutePath());
                }

                stderrFiles.add(stderrF);
                maskedFiles.add(maskedF);
                if (alignF.exists()) {
                    alignFiles.add(alignF);
                }
                if (index == 0) {
                    outFiles.add(outF);
                }
                else {
                    File filteredOutFile = new File(outF.getAbsolutePath() + ".noheader");
                    createFilteredFile(outF, filteredOutFile, "perc\\s+perc|score\\s+div|^$");
                    outFiles.add(filteredOutFile);
                }
                catFiles.add(catF);
                tblFiles.add(tblF);

                index++;
            }

            // cat the results
            FileUtil.concatFilesUsingSystemCall(stderrFiles, repeatMaskerResultFileStderr);
            FileUtil.concatFilesUsingSystemCall(maskedFiles, repeatMaskerResultFileMasked);
            FileUtil.concatFilesUsingSystemCall(outFiles, repeatMaskerResultFileOut);
            FileUtil.concatFilesUsingSystemCall(catFiles, repeatMaskerResultFileCat);
            FileUtil.concatFilesUsingSystemCall(tblFiles, repeatMaskerResultFileTbl);
            // (also .align, if they exist)
            if (alignFiles.size() > 0) {
                FileUtil.concatFilesUsingSystemCall(alignFiles, repeatMaskerResultFileAlign);
            }

            // Post process stdout files                       
            for (File f : outputFiles) {
                if (!f.exists()) {
                    throw new Exception("Could not locate expected output file=" + f.getAbsolutePath());
                }
                stdoutFiles.add(f);
            }
            FileUtil.concatFilesUsingSystemCall(stdoutFiles, repeatMaskerResultFileStdout);


        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        repeatMaskerTask = getRepeatMaskerTask(processData);
        task = repeatMaskerTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this RepeatMasker call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for RepeatMaskerTask=" + repeatMaskerTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        repeatMaskerResultNode = createResultFileNode();
        logger.info("Setting repeatMaskerResultNode=" + repeatMaskerResultNode.getObjectId() + " path=" + repeatMaskerResultNode.getDirectoryPath());
        resultFileNode = repeatMaskerResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);
        Long inputFastaNodeId = new Long(repeatMaskerTask.getParameter(RepeatMaskerTask.PARAM_fasta_input_node_id));
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
            logger.info("RepeatMasker taskId=" + repeatMaskerTask.getObjectId() + " using pre-split file=" + inputFile.getAbsolutePath() + " and post-split inputFile path=" + file.getAbsolutePath());
        }
        logger.info(this.getClass().getName() + " repeatMaskerTaskId=" + task.getObjectId() + " init() end");
    }

    private RepeatMaskerTask getRepeatMaskerTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null RepeatMaskerTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (RepeatMaskerTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getRepeatMaskerTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private RepeatMaskerResultNode createResultFileNode() throws Exception {
        RepeatMaskerResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof RepeatMaskerResultNode) {
                logger.info("Found already-extant repeatMaskerResultNode path=" + ((RepeatMaskerResultNode) node).getDirectoryPath());
                return (RepeatMaskerResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating RepeatMaskerResultNode with sessionName=" + sessionName);
        resultFileNode = new RepeatMaskerResultNode(task.getOwner(), task,
                "RepeatMaskerResultNode", "RepeatMaskerResultNode for task " + task.getObjectId(),
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
        String cmd = "cat " + source.getAbsolutePath() + " | grep -vP \'" + filter + "\' > " + target.getAbsolutePath();
        int ev = sc.emulateCommandLine(cmd, true);
        if (ev != 0) {
            throw new Exception("System cmd returned non-zero exit state=" + cmd);
        }
    }

}