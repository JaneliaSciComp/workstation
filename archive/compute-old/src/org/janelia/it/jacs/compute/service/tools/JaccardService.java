
package org.janelia.it.jacs.compute.service.tools;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.tools.JaccardTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.JaccardResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: ekelsey
 * Date: Aug 13, 2010
 * Time: 3:31:05 PM
 * From jacs.properties
 * # Jaccard
 Jaccard.JaccardCmd = /usr/local/devel/ANNOTATION/ekelsey/VICS/trunk/compute/scripts/pangenome/clusterBsmlPairwiseAlignments
 Jaccard.Queue=-l medium
 Jaccard.MaxEntriesPerJob=200
 Jaccard.MakeFastaLookupCmd=/usr/local/devel/ANNOTATION/ekelsey/VICS/trunk/compute/scripts/pangenome/fasta2idlookup.pl
 Jaccard.ClusterCmd = /usr/local/devel/ANNOTATION/EGC_utilities/bin/cluster
 Jaccard.Cog2FastaCmd = /usr/local/devel/ANNOTATION/ekelsey/VICS/trunk/compute/scripts/pangenome/CogProteinFasta.pl
 Jaccard.MergeListsCmd = /usr/local/devel/ANNOTATION/ard/ergatis-v2r10b1/bin/merge_lists
 Jaccard.JaccardFind = /usr/bin/find
 */
public class JaccardService extends SubmitDrmaaJobService {

    public static final String JACCARD_CMD = SystemConfigurationProperties.getString("Jaccard.JaccardCmd");
    public static final String JACCARD_MAKE_FASTA_LOOKUP_CMD = SystemConfigurationProperties.getString("Jaccard.MakeFastaLookupCmd");    
    public static final String JACCARD_MERGE_LISTS_CMD = SystemConfigurationProperties.getString("Jaccard.MergeListsCmd");
    public static final String JACCARD_CLUSTER_CMD = SystemConfigurationProperties.getString("Jaccard.ClusterCmd");
    public static final String JACCARD_FIND_CMD = SystemConfigurationProperties.getString("Jaccard.JaccardFind");
    public static final String JACCARD_COG2FASTA_CMD  = SystemConfigurationProperties.getString("Jaccard.Cog2FastaCmd");

    public static final String JACCARD_QUEUE = SystemConfigurationProperties.getString("Jaccard.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("Jaccard.MaxEntriesPerJob");

    File tmpDir;
    
    JaccardTask jaccardTask;
    JaccardResultNode jaccardResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    ArrayList<String> genomes = new ArrayList<String>();
    ArrayList<File> genomeListFiles = new ArrayList<File>();
    ArrayList<File> genomeDirs = new ArrayList<File>();
    ArrayList<File> fastaListFiles = new ArrayList<File>();
    ArrayList<File> inputFastaFiles = new ArrayList<File>();
    
    protected String getGridServicePrefixName() {
        return "jaccard";
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
        createShellScript(jaccardTask, writer);

        int configIndex = 0;

         for (String genome : genomes) {

            configIndex = writeConfigFile(genome, genomeListFiles.get(configIndex), genomeDirs.get(configIndex), fastaListFiles.get(configIndex), inputFastaFiles.get(configIndex),configIndex);

            configIndex++;
        }
        
        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);
    }

    private int writeConfigFile(String genome, File genomeListFile, File genomeDir, File fastaListFile, File inputFile, int configIndex) throws IOException {
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex + 1));
        String clusterOutputFile = (new File(resultFileNode.getDirectoryPath() + File.separator + genome + ".jkcluster.out")).toString();
        String asmblListFile = (new File(resultFileNode.getDirectoryPath() + File.separator + genome + ".asmbl.lookup")).toString();

        while (configFile.exists()) {
            configFile = new File(resultFileNode.getDirectoryPath(), buildConfigFileName(++configIndex));
        }

        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {

            configWriter.println(asmblListFile);
            configWriter.println(clusterOutputFile);
            configWriter.println(genomeDir);
            configWriter.println(fastaListFile);
            configWriter.println(inputFile);
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
        logger.info("setQueue = " + JACCARD_QUEUE);
        jt.setNativeSpecification(JACCARD_QUEUE);
    }

    private void createShellScript(JaccardTask jaccardTask, FileWriter writer)
            throws IOException, ParameterException {
        try {

            StringBuffer script = new StringBuffer();
            script.append("read ASMBLLOOKUP\n");
            script.append("read CLUSTEROUTPUTFILE\n");
            script.append("read GENOMEDIR\n");
            script.append("read FASTALISTFILE\n");
            script.append("read INPUTFILE\n");


            String clusterOptionString = jaccardTask.generateCommandOptionsClusters();
            String jaccard2FastaOptionString = jaccardTask.generateCommandOptionsJaccard2Fasta();

            String asmblLookupFile = "\"$ASMBLLOOKUP\"";
            String clusterOutputFile = "\"$CLUSTEROUTPUTFILE\"";
            String genomeDir = "\"$GENOMEDIR\"";
            String fastaListFile = "\"$FASTALISTFILE\"";
            String inputFile = "\"$INPUTFILE\"";

            String jaccardAsmblIdStr = JACCARD_MAKE_FASTA_LOOKUP_CMD + " --fasta_file=" + inputFile + " --output=" + asmblLookupFile;
            String jaccard2FastaStr = JACCARD_COG2FASTA_CMD + " " + jaccard2FastaOptionString + " --cogFile=" + clusterOutputFile + " --fastaInputFile=" + inputFile + " --outputDir=" + genomeDir;        
            String jaccardClusterStr = JACCARD_CMD + " " +  clusterOptionString +  " --asmbl_lookup=" + asmblLookupFile + " --cluster_path=" + JACCARD_CLUSTER_CMD + " --outfile=" + clusterOutputFile + " --tmpdir=" + tmpDir.toString();
            String createFastaList = JACCARD_FIND_CMD + " " + genomeDir + " -name '*.fsa' >> " + fastaListFile;
            
            script.append(jaccardAsmblIdStr).append("\n");
            script.append(jaccardClusterStr).append("\n");
            script.append(jaccard2FastaStr).append("\n");
            script.append(createFastaList).append("\n");

            writer.write(script.toString());
        } catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File jaccardResultFile = new File(jaccardResultNode.getFilePathByTag(JaccardResultNode.TAG_LIST_OUTPUT));
            File jaccardDir = new File(jaccardResultNode.getDirectoryPath());
            SystemCall call = new SystemCall(logger);
            
            logger.info("postProcess for jaccardTaskId="+jaccardTask.getObjectId()+" and resultNodeDir="+jaccardDir.getAbsolutePath());

            String mergeCmd = JACCARD_MERGE_LISTS_CMD + " --input_dir=" + jaccardDir.toString() + " --output_list=" + jaccardResultFile + " --glob=" + JaccardResultNode.TAG_FSA_LIST;

            int returnVal = call.emulateCommandLine(mergeCmd, true);

            if (returnVal != 0) {
                 throw new RuntimeException("Execution of " + mergeCmd + " failed");
            }
        } catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }

    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        jaccardTask = getJaccardTask(processData);
        task = jaccardTask;

        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this Jaccard call is part of a pipeline, drop the results in the annotation directory

        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for JaccardTask=" + jaccardTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        jaccardResultNode = createResultFileNode();
        logger.info("Setting jaccardResultNode=" + jaccardResultNode.getObjectId() + " path=" + jaccardResultNode.getDirectoryPath());
        resultFileNode = jaccardResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);

        //Parse Input File List
        File inputListFile = new File(task.getParameter(JaccardTask.PARAM_input_file_list));

        parseInputListFile(inputListFile);

        tmpDir = new File(SystemConfigurationProperties.getString("SystemCall.ScratchDir") + File.separator + jaccardTask.getObjectId());

        if(!tmpDir.mkdir()){
           throw new IOException("Could not create tmp directory:" + tmpDir.toString());
        }

        if(!tmpDir.setWritable(true,false)){
            throw new IOException("Could not change tmp directory permissions:" + tmpDir.toString());
        }
        
        logger.info(this.getClass().getName() + " jaccardTaskId=" + task.getObjectId() + " init() end");
    }

   protected void parseInputListFile(File inputListFile) throws Exception {

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputListFile)));

        try {
            String line;
            String genome = "";
 
            while (br.ready()) {
                line = br.readLine();
                SystemCall call = new SystemCall(logger);

                File inputFile = new File(line);

                inputFastaFiles.add(inputFile);
                String inputFileName = inputFile.getName();
                genome = inputFileName.substring(0,inputFileName.lastIndexOf("."));

                if(!genomes.contains(genome)){
                    genomes.add(genome);

                    File genomeDir = new File(resultFileNode.getDirectoryPath() + File.separator + genome);

                    genomeDirs.add(genomeDir);

                    if(!genomeDir.mkdir()){
                        throw new IOException("Could not create genome directory:" + genomeDir.toString());
                    }

                    File fastaListFile = new File(resultFileNode.getDirectoryPath() + File.separator + genome + "." + JaccardResultNode.TAG_FSA_LIST);
                    fastaListFiles.add(fastaListFile);
                }

                File genomeListFile = new File(resultFileNode.getDirectoryPath() + File.separator + genome + ".asmbl.list");

                if(!genomeListFiles.contains(genomeListFile)){
                    genomeListFiles.add(genomeListFile);
                }

                String command = "echo " + inputFile.getAbsolutePath() + " >> " + genomeListFile.getAbsolutePath();
                int returnVal = call.emulateCommandLine(command, true);

                if (returnVal != 0) {
                    throw new RuntimeException("Execution of " + command + " failed");
                }
            }

            br.close();

        }
        catch (Exception e) {
            throw new Exception(e.toString());
        }

    }

    private JaccardTask getJaccardTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null JaccardTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (JaccardTask) computeDAO.getTaskById(taskId);
            }
            return null;
        } catch (Exception e) {
            logger.error("Received exception in getJaccardTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private JaccardResultNode createResultFileNode() throws Exception {
        JaccardResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof JaccardResultNode) {
                logger.info("Found already-extant jaccardResultNode path="+((JaccardResultNode)node).getDirectoryPath());
                return (JaccardResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating JaccardResultNode with sessionName=" + sessionName);
        resultFileNode = new JaccardResultNode(task.getOwner(), task,
                "JaccardResultNode", "JaccardResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

}