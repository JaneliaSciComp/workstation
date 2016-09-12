
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
import org.janelia.it.jacs.model.tasks.tools.Clustalw2Task;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.Clustalw2ResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Jul 28, 2010
 * Time: 3:31:05 PM
 * From jacs.properties
 * # Clustalw2
 Clustalw2.Clustalw2Cmd=/usr/local/bin/clustalw2
 Clustalw2.Queue=-l medium
 Clustalw2.MaxEntriesPerJob=200
 Clustalw2.MSF2Bsml = /usr/local/devel/ANNOTATION/ard/ergatis-v2r10b1/bin/MSF2Bsml
 */
public class Clustalw2Service extends SubmitDrmaaJobService {

    public static final String CLUSTALW2_CMD = SystemConfigurationProperties.getString("Clustalw2.Clustalw2Cmd");
    public static final String CLUSTALW2_QUEUE = SystemConfigurationProperties.getString("Clustalw2.Queue");
    public static final String CLUSTALW2_MSF2BSML_CMD = SystemConfigurationProperties.getString("Clustalw2.MSF2Bsml");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("Clustalw2.MaxEntriesPerJob");

    private static final String resultFilename = Clustalw2ResultNode.BASE_OUTPUT_FILENAME;

    private List<File> inputFiles;
    private List<File> outputFiles;
    Clustalw2Task clustalw2Task;
    Clustalw2ResultNode clustalw2ResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    ArrayList<File> bsmlFiles = new ArrayList<File>();
    
    protected String getGridServicePrefixName() {
        return "clustalw2";
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
        createShellScript(clustalw2Task, writer);

        int configIndex = 1;
        File outputDir = new File(resultFileNode.getDirectoryPath());
        outputFiles = new ArrayList<File>();
        for (File inputFile : inputFiles) {
            File outputFile = new File(new File(outputDir, resultFilename).getAbsolutePath() + "." + configIndex);
            File bsmlOutputFile = new File(new File(outputDir.getAbsolutePath()) + File.separator + configIndex + ".bsml");

            outputFiles.add(outputFile);
            bsmlFiles.add(bsmlOutputFile);
            
            configIndex = writeConfigFile(inputFile, outputFile, bsmlOutputFile, configIndex);
            configIndex++;
        }

        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);
    }

    private int writeConfigFile(File inputFile, File outputFile, File bsmlOutputFile, int configIndex) throws IOException {
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }
        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {
            configWriter.println(inputFile.getAbsolutePath());
            configWriter.println(outputFile.getAbsolutePath());
            configWriter.println(bsmlOutputFile.getAbsolutePath());
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
        logger.info("setQueue = " + CLUSTALW2_QUEUE);
        jt.setNativeSpecification(CLUSTALW2_QUEUE);
    }

    private void createShellScript(Clustalw2Task clustalw2Task, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");
            script.append("read BSMLOUTPUTFILE\n");

            String optionString = clustalw2Task.generateCommandOptions();

            String inputFileString = "\"$INPUTFILE\"";
            String outputFileString = "\"$OUTPUTFILE\"";
            String bsmlOutputFileString = "\"$BSMLOUTPUTFILE\"";

            String clustalw2Str = CLUSTALW2_CMD + " " + optionString + " -infile=" + inputFileString + " -outfile=" + outputFileString;
            String msf2BsmlStr = CLUSTALW2_MSF2BSML_CMD + " --msffile=" + outputFileString + " --output=" + bsmlOutputFileString;

            script.append(clustalw2Str).append("\n");
            script.append(msf2BsmlStr).append("\n");
            
            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File clustalw2ResultFile = new File(clustalw2ResultNode.getFilePathByTag(Clustalw2ResultNode.TAG_CLUSTALW2_CLW));
            File clustalw2Dir = new File(clustalw2ResultNode.getDirectoryPath());
            File clustalw2BsmlList = new File(clustalw2ResultNode.getFilePathByTag(Clustalw2ResultNode.TAG_CLUSTALW2_LIST));

            logger.info("postProcess for clustalw2TaskId=" + clustalw2Task.getObjectId() + " and resultNodeDir=" + clustalw2Dir.getAbsolutePath());

            FileUtil.concatFilesUsingSystemCall(outputFiles, clustalw2ResultFile);

            StringBuffer fastaBuffer = new StringBuffer();
            FileWriter fastaWriter = new FileWriter(clustalw2BsmlList);

            for (File f : bsmlFiles) {

                fastaBuffer.append(f.getAbsolutePath());
                fastaBuffer.append("\n");

            }

            fastaWriter.write(fastaBuffer.toString());
            fastaWriter.close();
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        clustalw2Task = getClustalw2Task(processData);
        task = clustalw2Task;
        File inputFile;
        
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this Clustalw2 call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for Clustalw2Task=" + clustalw2Task.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        clustalw2ResultNode = createResultFileNode();
        logger.info("Setting clustalw2ResultNode=" + clustalw2ResultNode.getObjectId() + " path=" + clustalw2ResultNode.getDirectoryPath());
        resultFileNode = clustalw2ResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);

        FastaFileNode inputFastaNode;
        Long inputFastaNodeId;

        if (!task.getParameter(Clustalw2Task.PARAM_fasta_input_node_id).isEmpty()) {

            inputFastaNodeId = new Long(clustalw2Task.getParameter(Clustalw2Task.PARAM_fasta_input_node_id));
            inputFastaNode = (FastaFileNode) computeDAO.getNodeById(inputFastaNodeId);

            logger.info("Using inputNode=" + inputFastaNode.getObjectId() + " path=" + inputFastaNode.getDirectoryPath());

            inputFile = new File(inputFastaNode.getFastaFilePath());
            inputFiles = SimpleMultiFastaSplitterService.splitInputFileToList(processData, new File(inputFastaNode.getFastaFilePath()),
            DEFAULT_ENTRIES_PER_EXEC, logger);

            for (File file : inputFiles) {
                logger.info("Clustalw2 taskId=" + clustalw2Task.getObjectId() + " using pre-split file=" + inputFile.getAbsolutePath() + " and post-split inputFile path=" + file.getAbsolutePath());
            }
        }
        else if(!task.getParameter(Clustalw2Task.PARAM_fasta_input_file_list).isEmpty()){
                       
           inputFile =  new File(task.getParameter(Clustalw2Task.PARAM_fasta_input_file_list));

           parseFsaInputList(inputFile);

           for (File file : inputFiles) {
               logger.info("Clustalw2 taskId=" + clustalw2Task.getObjectId() + " found input file=" + file.getAbsolutePath());
           }

        }else{
            String errorMessage = "Unexpectedly received null inputFastaNode or inputFastaFileList";
            logger.error(errorMessage);
            throw new Exception(errorMessage);
        }


        logger.info(this.getClass().getName() + " clustalw2TaskId=" + task.getObjectId() + " init() end");
    }

     protected void parseFsaInputList(File fsaListFile) throws Exception {

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fsaListFile)));

        inputFiles = new ArrayList<File>();
         
        try {
            String line;

            while (br.ready()) {

                line = br.readLine();
                File fsaFile = new File(line);

                inputFiles.add(fsaFile);
            }

            br.close();

        }
        catch (Exception e) {
            throw new Exception(e.toString());
        }

    }
    
    private Clustalw2Task getClustalw2Task(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null Clustalw2Task from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (Clustalw2Task) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getClustalw2Task: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private Clustalw2ResultNode createResultFileNode() throws Exception {
        Clustalw2ResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof Clustalw2ResultNode) {
                logger.info("Found already-extant clustalw2ResultNode path=" + ((Clustalw2ResultNode) node).getDirectoryPath());
                return (Clustalw2ResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating Clustalw2ResultNode with sessionName=" + sessionName);
        resultFileNode = new Clustalw2ResultNode(task.getOwner(), task,
                "Clustalw2ResultNode", "Clustalw2ResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }
}