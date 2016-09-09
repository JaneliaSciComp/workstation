
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
import org.janelia.it.jacs.model.tasks.tools.Fasta2BsmlTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.Fasta2BsmlResultNode;
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
 * Date: Sep 20, 2010
 * Time: 3:31:05 PM
 * From jacs.properties
 * #Fasta2Bsml
 Fasta2Bsml.Fasta2BsmlCmd = /usr/local/devel/ANNOTATION/ard/ergatis-v2r10b1/bin/fasta2bsml
 Fasta2Bsml.Queue=-l medium
 Fasta2Bsml.MaxEntriesPerJob=200
 */
public class Fasta2BsmlService extends SubmitDrmaaJobService {

    public static final String FASTA2BSML_CMD = SystemConfigurationProperties.getString("Fasta2Bsml.Fasta2BsmlCmd");
    public static final String FASTA2BSML_QUEUE = SystemConfigurationProperties.getString("Fasta2Bsml.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("Fasta2Bsml.MaxEntriesPerJob");
    private static final String resultFilename = Fasta2BsmlResultNode.BASE_OUTPUT_FILENAME;

    private List<File> inputFiles;
    private List<File> outputFiles;
    Fasta2BsmlTask fasta2bsmlTask;
    Fasta2BsmlResultNode fasta2bsmlResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    protected String getGridServicePrefixName() {
        return "fasta2bsml";
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
        createShellScript(fasta2bsmlTask, writer);

        int configIndex = 1;
        File outputDir = new File(resultFileNode.getDirectoryPath());
        outputFiles = new ArrayList<File>();
        for (File inputFile : inputFiles) {
            File outputFile;
            String genus = ""; 
            String species = "";
            
            if(task.getParameter(Fasta2BsmlTask.PARAM_format).compareTo("single") == 0){
                outputFile = new File(resultFileNode.getDirectoryPath());
            }else{
                outputFile = new File(new File(outputDir.getAbsolutePath()) + File.separator + configIndex + ".bsml");
            }

            //Parse file name to be set at genus
            if(!task.getParameter(Fasta2BsmlTask.PARAM_organism).isEmpty()){
                String inputFileName = inputFile.getName();
                genus = inputFileName.substring(0,inputFileName.lastIndexOf("."));

                species = "na";
            }else{
                if(!task.getParameter(Fasta2BsmlTask.PARAM_genus).isEmpty()){
                     genus = task.getParameter(Fasta2BsmlTask.PARAM_genus);
                }

                if(!task.getParameter(Fasta2BsmlTask.PARAM_species).isEmpty()){
                     species = task.getParameter(Fasta2BsmlTask.PARAM_species);
                }
            }

            outputFiles.add(outputFile);
            configIndex = writeConfigFile(inputFile, outputFile, genus, species, configIndex);
            configIndex++;
        }

        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);
    }

    private int writeConfigFile(File inputFile, File outputFile, String genus, String species, int configIndex) throws IOException {
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }
        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {
            configWriter.println(inputFile.getAbsolutePath());
            configWriter.println(outputFile.getAbsolutePath());
            configWriter.println(genus);
            configWriter.println(species);
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
        logger.info("setQueue = " + FASTA2BSML_QUEUE);
        jt.setNativeSpecification(FASTA2BSML_QUEUE);
    }

    private void createShellScript(Fasta2BsmlTask fasta2bsmlTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");
            script.append("read GENUS\n");
            script.append("read SPECIES\n");

            String optionString = fasta2bsmlTask.generateCommandOptions(resultFileNode.getDirectoryPath());

            String inputFileString = "\"$INPUTFILE\"";
            String outputFileString = "\"$OUTPUTFILE\"";
            String genusString = "\"$GENUS\"";
            String speciesString = "\"$SPECIES\"";

            String fasta2bsmlStr = FASTA2BSML_CMD + " " + optionString + " --fasta_input=" + inputFileString + " --output=" + outputFileString + " --genus=" + genusString + " --species=" + speciesString;
            script.append(fasta2bsmlStr).append("\n");

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File fasta2bsmlResultFile = new File(fasta2bsmlResultNode.getFilePathByTag(Fasta2BsmlResultNode.TAG_FASTA2BSML_BSML));
            File fasta2bsmlDir = new File(fasta2bsmlResultNode.getDirectoryPath());
            File fasta2bsmlList = new File(fasta2bsmlResultNode.getFilePathByTag(Fasta2BsmlResultNode.TAG_FASTA2BSML_LIST));

            logger.info("postProcess for fasta2bsmlTaskId=" + fasta2bsmlTask.getObjectId() + " and resultNodeDir=" + fasta2bsmlDir.getAbsolutePath());

            FileUtil.concatFilesUsingSystemCall(outputFiles, fasta2bsmlResultFile);

            StringBuffer bsmlBuffer = new StringBuffer();
            FileWriter bsmlWriter = new FileWriter(fasta2bsmlList);

            for (File f : outputFiles) {

                bsmlBuffer.append(f.getAbsolutePath());
                bsmlBuffer.append("\n");

            }

            bsmlWriter.write(bsmlBuffer.toString());
            bsmlWriter.close();
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        fasta2bsmlTask = getFasta2BsmlTask(processData);
        task = fasta2bsmlTask;
        File inputFile;
        
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this Priam call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for Fasta2BsmlTask=" + fasta2bsmlTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        fasta2bsmlResultNode = createResultFileNode();
        logger.info("Setting fasta2bsmlResultNode=" + fasta2bsmlResultNode.getObjectId() + " path=" + fasta2bsmlResultNode.getDirectoryPath());
        resultFileNode = fasta2bsmlResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);

        FastaFileNode inputFastaNode;
        Long inputFastaNodeId;

        if (!task.getParameter(Fasta2BsmlTask.PARAM_fasta_input_node_id).isEmpty()) {
            inputFastaNodeId = new Long(fasta2bsmlTask.getParameter(Fasta2BsmlTask.PARAM_fasta_input_node_id));
            inputFastaNode = (FastaFileNode) computeDAO.getNodeById(inputFastaNodeId);

            logger.info("Using inputNode=" + inputFastaNode.getObjectId() + " path=" + inputFastaNode.getDirectoryPath());

            inputFile = new File(inputFastaNode.getFastaFilePath());
            inputFiles = SimpleMultiFastaSplitterService.splitInputFileToList(processData, new File(inputFastaNode.getFastaFilePath()),
            DEFAULT_ENTRIES_PER_EXEC, logger);

            for (File file : inputFiles) {
                logger.info("Fasta2Bsml taskId=" + fasta2bsmlTask.getObjectId() + " using pre-split file=" + inputFile.getAbsolutePath() + " and post-split inputFile path=" + file.getAbsolutePath());
            }
        }
        else if(!task.getParameter(Fasta2BsmlTask.PARAM_fasta_list).isEmpty()){
           inputFile =  new File(task.getParameter(Fasta2BsmlTask.PARAM_fasta_list));

           parseFsaInputList(inputFile);

           for (File file : inputFiles) {
               logger.info("Fasta2Bsml taskId=" + fasta2bsmlTask.getObjectId() + " found input file=" + file.getAbsolutePath());
           }

        }else if(!task.getParameter(Fasta2BsmlTask.PARAM_fasta_input).isEmpty()){            
           inputFile = new File(task.getParameter(Fasta2BsmlTask.PARAM_fasta_input)); 

           inputFiles.add(inputFile);
            
           for (File file : inputFiles) {
               logger.info("Fasta2Bsml taskId=" + fasta2bsmlTask.getObjectId() + " found input file=" + file.getAbsolutePath());
           }

        }else{
            String errorMessage = "Unexpectedly received null inputFastaNode, inputFastaFileList, inputFastaFile";
            logger.error(errorMessage);
            throw new Exception(errorMessage);
        }
        
        logger.info(this.getClass().getName() + " fasta2bsmlTaskId=" + task.getObjectId() + " init() end");
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
    
    private Fasta2BsmlTask getFasta2BsmlTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null Fasta2BsmlTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (Fasta2BsmlTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getFasta2BsmlTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private Fasta2BsmlResultNode createResultFileNode() throws Exception {
        Fasta2BsmlResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof Fasta2BsmlResultNode) {
                logger.info("Found already-extant fasta2bsmlResultNode path=" + ((Fasta2BsmlResultNode) node).getDirectoryPath());
                return (Fasta2BsmlResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating Fasta2BsmlResultNode with sessionName=" + sessionName);
        resultFileNode = new Fasta2BsmlResultNode(task.getOwner(), task,
                "Fasta2BsmlResultNode", "Fasta2BsmlResultNode for task " + task.getObjectId(),
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