
package org.janelia.it.jacs.compute.service.sift;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.sift.SiftProteinSubstitutionTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.sift.SiftProteinSubstitutionResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: zguan
 * Date: Jul 21, 2010
 * Time: 10:35:32 AM
 * From jacs.properties
 * # SiftProteinSubstitution
 SiftProteinSubstitution.SiftProteinSubstitutionCmd=csh /usr/local/projects/SIFT/build/build-production/sift/bin/SIFT_for_submitting_fasta_seq.csh
 SiftProteinSubstitution.TmpResultDir=/usr/local/projects/SIFT/build/build-production/sift/tmp
 SiftProteinSubstitution.ProteinDatabase=/usr/local/projects/SIFT/test/legacy_blast/data/uniprot_trembl
 SiftProteinSubstitution.Queue=-l medium
 SiftProteinSubstitution.MaxEntriesPerJob=200
 */
public class SiftProteinSubstitutionService extends SubmitDrmaaJobService {

    public static final String SIFTPROTEINSUBSITUTION_CMD = SystemConfigurationProperties.getString("SiftProteinSubstitution.SiftProteinSubstitutionCmd");
    public static final String SIFTPROTEINSUBSITUTION_QUEUE = SystemConfigurationProperties.getString("SiftProteinSubstitution.Queue");
    public static final String SIFTDATABASE = SystemConfigurationProperties.getString("SiftProteinSubstitution.ProteinDatabase");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("SiftProteinSubstitution.MaxEntriesPerJob");
    public static final String TMP_RESULT_DIR = SystemConfigurationProperties.getString("SiftProteinSubstitution.TmpResultDir");
    private static final String resultFilename = SiftProteinSubstitutionResultNode.BASE_OUTPUT_FILENAME;

    private List<File> inputFiles;
    private List<File> outputFiles;
    private File substitutionFile;

    SiftProteinSubstitutionTask siftProteinSubstitutionTask;
    SiftProteinSubstitutionResultNode siftProteinSubstitutionResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    protected String getGridServicePrefixName() {
        return "siftProteinSubstitution";
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
        createShellScript(siftProteinSubstitutionTask, writer);

        int configIndex = 1;
        File outputDir = new File(resultFileNode.getDirectoryPath());
        outputFiles = new ArrayList<File>();

        File databaseFile = new File(SIFTDATABASE);

        for (File inputFile : inputFiles) {
            File outputFile = new File(new File(outputDir, resultFilename).getAbsolutePath() + "." + configIndex);
            outputFiles.add(outputFile);
            configIndex = writeConfigFile(inputFile, outputFile, databaseFile, substitutionFile, configIndex);
            configIndex++;
        }

        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);
    }

    private int writeConfigFile(File inputFile, File outputFile, File databaseFile, File substitutionFile, int configIndex) throws IOException {
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }
        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {
            configWriter.println(inputFile.getAbsolutePath());
            configWriter.println(databaseFile.getAbsolutePath());
            configWriter.println(substitutionFile.getAbsolutePath());
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
        logger.info("setQueue = " + SIFTPROTEINSUBSITUTION_QUEUE);
        jt.setNativeSpecification(SIFTPROTEINSUBSITUTION_QUEUE);
    }

    private void createShellScript(SiftProteinSubstitutionTask siftProteinSubstitutionTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read DATABASEFILE\n");
            script.append("read SUBSTITUTIONFILE\n");
            script.append("read OUTPUTFILE\n");

            String inputFileString = "\"$INPUTFILE\"";
            String databaseFileString = "\"$DATABASEFILE\"";
            String substitutionFileString = "\"$SUBSTITUTIONFILE\"";
            String outputFileString = "\"$OUTPUTFILE\"";

            String siftProteinSubstitutionStr = SIFTPROTEINSUBSITUTION_CMD + " " + inputFileString + " " + databaseFileString + " " + substitutionFileString + " > " + outputFileString;
            script.append(siftProteinSubstitutionStr).append("\n");

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File siftProteinSubstitutionResultFile = new File(siftProteinSubstitutionResultNode.getFilePathByTag(SiftProteinSubstitutionResultNode.TAG_SIFTPROTEINSUBSTITUTION_OUTPUT));
            File siftProteinSubstitutionDir = new File(siftProteinSubstitutionResultNode.getDirectoryPath());
            List<File> filteredFiles = new ArrayList<File>();
            logger.info("postProcess for siftProteinSubstitutionTaskId=" + siftProteinSubstitutionTask.getObjectId() + " and resultNodeDir=" + siftProteinSubstitutionDir.getAbsolutePath());
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
            FileUtil.concatFilesUsingSystemCall(filteredFiles, siftProteinSubstitutionResultFile);


            if (inputFiles == null) {
                throw new Exception("inputFiles is null");
            }
            if (inputFiles.size() < 1) {
                throw new Exception("inputFiles size is 0");
            }
            File inputFile = inputFiles.get(0);
            if (inputFile == null) {
                throw new Exception("inputFile is null");
            }
            logger.info("inputFile path is=" + inputFile.getAbsolutePath());
            String inputFileName = inputFile.getName();
            logger.info("inputFile name is=" + inputFileName);
            String[] inputFileComponents = inputFileName.split("\\.");
            if (inputFileComponents.length == 0) {
                throw new Exception("inputFileComponents length is 0");
            }
            if (inputFileComponents.length < 2) {
                throw new Exception("Expected inputFileComponents length > 1");
            }
            File resultFile = new File(TMP_RESULT_DIR, inputFileComponents[0] + ".SIFTprediction");
            if (!resultFile.exists()) {
                throw new Exception("Expected result file " + resultFile.getAbsolutePath() + " does not exist");
            }
            File finalResultFile = new File(resultFileNode.getDirectoryPath(), resultFile.getName());

            logger.info("Final result final name=" + finalResultFile.getName() + "Final result file path =" + finalResultFile.getAbsolutePath());
            FileUtil.copyFile(resultFile, finalResultFile);
            resultFile.delete();

        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        siftProteinSubstitutionTask = getSiftProteinSubstitutionTask(processData);
        task = siftProteinSubstitutionTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this Priam call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for SiftProteinSubstitutionTask=" + siftProteinSubstitutionTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        siftProteinSubstitutionResultNode = createResultFileNode();
        logger.info("Setting siftProteinSubstitutionResultNode=" + siftProteinSubstitutionResultNode.getObjectId() + " path=" + siftProteinSubstitutionResultNode.getDirectoryPath());
        resultFileNode = siftProteinSubstitutionResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);

        // Get the input file
        Long inputFastaNodeId = new Long(siftProteinSubstitutionTask.getParameter(SiftProteinSubstitutionTask.PARAM_fasta_input_node_id));
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
        inputFiles = new ArrayList<File>();
        inputFiles.add(inputFile);

        // Get the substitution file
        String substitutionString = siftProteinSubstitutionTask.getParameter(SiftProteinSubstitutionTask.PARAM_substitution_string);
        substitutionFile = new File(resultFileNode.getDirectoryPath(), inputFile.getName() + ".substitution");
        FileWriter fw = new FileWriter(substitutionFile);
        fw.write(substitutionString.trim() + "\n");
        fw.close();
        //FastaFileNode inputFastaNode = (FastaFileNode) computeDAO.getNodeById(inputFastaNodeId);
        if (substitutionString != null) {
            logger.info("Using substitution string=" + substitutionString + " path=" + substitutionFile.getAbsolutePath());
        }
        else {
            String errorMessage = "Unexpectedly received null substitution string";
            logger.error(errorMessage);
            throw new Exception(errorMessage);
        }

        logger.info("SiftProteinSubstitution taskId=" + siftProteinSubstitutionTask.getObjectId());
        logger.info(this.getClass().getName() + " siftProteinSubstitutionTaskId=" + task.getObjectId() + " init() end");
    }

    private SiftProteinSubstitutionTask getSiftProteinSubstitutionTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null SiftProteinSubstitutionTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (SiftProteinSubstitutionTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getSiftProteinSubstitutionTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private SiftProteinSubstitutionResultNode createResultFileNode() throws Exception {
        SiftProteinSubstitutionResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof SiftProteinSubstitutionResultNode) {
                logger.info("Found already-extant siftProteinSubstitutionResultNode path=" + ((SiftProteinSubstitutionResultNode) node).getDirectoryPath());
                return (SiftProteinSubstitutionResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating SiftProteinSubstitutionResultNode with sessionName=" + sessionName);
        resultFileNode = new SiftProteinSubstitutionResultNode(task.getOwner(), task,
                "SiftProteinSubstitutionResultNode", "SiftProteinSubstitutionResultNode for task " + task.getObjectId(),
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
