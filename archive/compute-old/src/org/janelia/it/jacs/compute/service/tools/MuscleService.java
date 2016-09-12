
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
import org.janelia.it.jacs.model.tasks.tools.MuscleTask;
import org.janelia.it.jacs.model.tasks.tools.TmhmmTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.MuscleResultFileNode;
import org.janelia.it.jacs.model.user_data.tools.TmhmmResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Basic usage
 * <p/>
 * muscle -in <inputfile> -out <outputfile>
 * <p/>
 * Common options (for a complete list please see the User Guide):
 * <p/>
 * -in <inputfile>    Input file in FASTA format (default stdin)
 * -out <outputfile>  Output alignment in FASTA format (default stdout)
 * -diags             Find diagonals (faster for similar sequences)
 * -maxiters <n>      Maximum number of iterations (integer, default 16)
 * -maxhours <h>      Maximum time to iterate in hours (default no limit)
 * -maxmb <m>         Maximum memory to allocate in Mb (default 80% of RAM)
 * -html              Write output in HTML format (default FASTA)
 * -msf               Write output in GCG MSF format (default FASTA)
 * -clw               Write output in CLUSTALW format (default FASTA)
 * -clwstrict         As -clw, with 'CLUSTAL W (1.81)' header
 * -log[a] <logfile>  Log to file (append if -loga, overwrite if -log)
 * -quiet             Do not write progress messages to stderr
 * -stable            Output sequences in input order (default is -group)
 * -group             Group sequences by similarity (this is the default)
 * -version           Display version information and exit
 * <p/>
 * Without refinement (very fast, avg accuracy similar to T-Coffee): -maxiters 2
 * Fastest possible (amino acids): -maxiters 1 -diags -sv -distance1 kbit20_3
 * Fastest possible (nucleotides): -maxiters 1 -diags
 *
 * From jacs.properties
 * #Muscle
 Muscle.MuscleCmd=/usr/local/bin/muscle
 Muscle.Queue=-l medium
 Muscle.MaxEntriesPerJob=200
  */
public class MuscleService extends SubmitDrmaaJobService {

    public static final String MUSCLE_CMD = SystemConfigurationProperties.getString("Muscle.MuscleCmd");
    public static final String TMHMM_QUEUE = SystemConfigurationProperties.getString("Muscle.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("Muscle.MaxEntriesPerJob");
    private List<File> inputFiles;
    private List<File> outputFiles;
    MuscleTask muscleTask;
    MuscleResultFileNode muscleResultNode;
    private String sessionName;

    protected String getGridServicePrefixName() {
        return "muscle";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        String resultFilename = "muscleOutput";
        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(muscleTask, writer);

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
        logger.info("setQueue = " + TMHMM_QUEUE);
        jt.setNativeSpecification(TMHMM_QUEUE);
    }

    private void createShellScript(MuscleTask muscleTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");

            String optionString = muscleTask.generateCommandOptions();

            String inputFileString = "\"$INPUTFILE\"";
            String outputFileString = "\"$OUTPUTFILE\"";

            String tmhmmStr = MUSCLE_CMD + " " + optionString + " " + inputFileString + " > " + outputFileString;
            script.append(tmhmmStr).append("\n");

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File tmhmmResultFile = new File(muscleResultNode.getFilePathByTag(TmhmmResultNode.TAG_TMHMM_OUTPUT));
            File tmhmmDir = new File(muscleResultNode.getDirectoryPath());
            List<File> filteredFiles = new ArrayList<File>();
            logger.info("postProcess for muscleTaskId=" + muscleTask.getObjectId() + " and resultNodeDir=" + tmhmmDir.getAbsolutePath());
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
//                    createFilteredFile(f, filteredFile, "#");
                    filteredFiles.add(filteredFile);
                }
                index++;
            }
            FileUtil.concatFilesUsingSystemCall(filteredFiles, tmhmmResultFile);
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        muscleTask = getMuscleTask(processData);
        task = muscleTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this Tmhmm call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for TmhmmTask=" + muscleTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        muscleResultNode = createResultFileNode();
        logger.info("Setting muscleResultNode=" + muscleResultNode.getObjectId() + " path=" + muscleResultNode.getDirectoryPath());
        resultFileNode = muscleResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);
        Long inputFastaNodeId = new Long(muscleTask.getParameter(TmhmmTask.PARAM_fasta_input_node_id));
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
            logger.info("Tmhmm taskId=" + muscleTask.getObjectId() + " using pre-split file=" + inputFile.getAbsolutePath() + " and post-split inputFile path=" + file.getAbsolutePath());
        }
        logger.info(this.getClass().getName() + " muscleTaskId=" + task.getObjectId() + " init() end");
    }

    private MuscleTask getMuscleTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null TmhmmTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (MuscleTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getMuscleTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private MuscleResultFileNode createResultFileNode() throws Exception {
        MuscleResultFileNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof MuscleResultFileNode) {
                logger.info("Found MuscleResultFileNode path=" + ((MuscleResultFileNode) node).getDirectoryPath());
                return (MuscleResultFileNode) node;
            }
        }

        // Create new node
        logger.info("Creating MuscleResultFileNode with sessionName=" + sessionName);
        resultFileNode = new MuscleResultFileNode(task.getOwner(), task,
                "MuscleResultFileNode", "MuscleResultFileNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

}