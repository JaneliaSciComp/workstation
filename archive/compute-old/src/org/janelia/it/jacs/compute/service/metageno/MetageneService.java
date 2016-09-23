
package org.janelia.it.jacs.compute.service.metageno;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.SimpleMultiFastaSplitterService;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.metageno.MetageneTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.metageno.MetageneResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 23, 2009
 * Time: 12:42:24 PM
 * From jacs.properties
 * # Metagene service properties
 Metagene.DefaultFastaEntriesPerExec=10000
 # Metagene 1.0
 #Metagene.Cmd=/usr/local/devel/ANNOTATION/microbial/metagene
 #Metagene.BtabCmd=metagene2btab.pl
 # Metagene 2.0
 Metagene.Cmd=/usr/local/devel/ANNOTATION/microbial/metagene_2_from_jbadger
 Metagene.BtabCmd=metagene2_2btab.pl
 Metagene.Queue=-l default
 */
public class MetageneService extends SubmitDrmaaJobService {
    // IService constants
    public static String METAGENE_TASK = "METAGENE_TASK";

    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("Metagene.DefaultFastaEntriesPerExec");

    private static final String resultFilename = MetageneResultNode.BASE_OUTPUT_FILENAME;

    /*
        SCRIPT DEPENDENCIES

        MetageneBtabCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/metagene2_2btab.pl
            <none>

        MODULE SUMMARY
            <none>

     */

    private static final String metageneCmd = SystemConfigurationProperties.getString("Metagene.Cmd");
    private static final String btabCmd = SystemConfigurationProperties.getString("Metagene.BtabCmd");
    private static final String queueName = SystemConfigurationProperties.getString("Metagene.Queue");

    private List<File> inputFiles;
    private List<File> outputFiles;
    MetageneTask metageneTask;
    MetageneResultNode metageneResultNode;
    File metageneBtabFile;
    private String sessionName;

    public static MetageneTask createDefaultTask() {
        return new MetageneTask();
    }

    protected void init(IProcessData processData) throws Exception {
        metageneTask = getMetageneTask(processData);
        task = metageneTask;
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        metageneResultNode = createResultFileNode();
        resultFileNode = metageneResultNode;
        super.init(processData);
        // First check if we have been assigned a specific file to process. If so, then we will not
        // worry about its size, assuming this has already been configured.
        inputFiles = new ArrayList<File>();
        outputFiles = new ArrayList<File>();
        File inputFile = getMgInputFile(processData);
        if (inputFile == null) {
            // Since we have not been assigned a file, we will get the input node from the task
            Long inputFastaNodeId = new Long(metageneTask.getParameter(MetageneTask.PARAM_input_fasta_node_id));
            FastaFileNode inputFastaNode = (FastaFileNode) computeDAO.getNodeById(inputFastaNodeId);
            inputFile = new File(inputFastaNode.getFastaFilePath());
        }
        inputFiles = SimpleMultiFastaSplitterService.splitInputFileToList(processData, inputFile, DEFAULT_ENTRIES_PER_EXEC, logger);
        logger.info(this.getClass().getName() + " metageneTaskId=" + task.getObjectId() + " init() end");
    }

    protected String getGridServicePrefixName() {
        return "metageneCaller";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    private MetageneTask getMetageneTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Object possibleTask = processData.getItem(METAGENE_TASK);
            if (possibleTask != null) {
                taskId = ((Task) possibleTask).getObjectId();
            }
            if (taskId == null) {
                // Attempt to get task from default IProcess location
                Task pdTask = ProcessDataHelper.getTask(processData);
                if (pdTask != null) {
                    taskId = pdTask.getObjectId();
                }
            }
            if (taskId != null) {
                return (MetageneTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getMetageneTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private MetageneResultNode createResultFileNode() throws ServiceException, IOException, DaoException {
        MetageneResultNode resultFileNode;

        // Check if we already have a result node for this task
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof MetageneResultNode) {
                return (MetageneResultNode) node;
            }
        }

        // Create new node
        resultFileNode = new MetageneResultNode(task.getOwner(), task,
                "MetageneResultNode", "MetageneResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    private File getMgInputFile(IProcessData processData) {
        try {
            File inputFile = (File) processData.getItem("MG_INPUT_ARRAY");
            if (inputFile != null) {
                logger.info("MetageneService using input file=" + inputFile.getAbsolutePath());
            }
            return inputFile;
        }
        catch (Exception e) {
            return null; // assume the value simply isn't in processData
        }
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(metageneTask, writer);

        int configIndex = 1;
        File outputDir = new File(resultFileNode.getDirectoryPath());
        for (File inputFile : inputFiles) {
            File outputFile = new File(new File(outputDir, resultFilename).getAbsolutePath() + "." + configIndex);
            outputFiles.add(outputFile);
            configIndex = writeConfigFile(inputFile, outputFile, configIndex);
            configIndex++;
        }

        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);
    }

    private class ConfigurationFileFilter implements FilenameFilter {
        public ConfigurationFileFilter() {
        }

        public boolean accept(File dir, String name) {
            return name != null && name.startsWith(getConfigPrefix());
        }
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

    private String buildConfigFileName(int configIndex) {
        return getConfigPrefix() + configIndex;
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("setQueue = " + queueName);
        jt.setNativeSpecification(queueName);
    }

    private void createShellScript(MetageneTask metageneTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");

            script.append("unset PERL5LIB\n");
            script.append("export PERL5LIB=").append(MetaGenoPerlConfig.PERL5LIB).append("\n");
            script.append("export PERL_MOD_DIR=").append(MetaGenoPerlConfig.PERL_MOD_DIR).append("\n");

            String cmd = metageneCmd + " \"$INPUTFILE\" > \"$OUTPUTFILE\"" + ".out\n";

            String echoCmd = "echo \"$OUTPUTFILE\".out > \"$OUTPUTFILE\".list\n";

            String btabCmdStr = MetaGenoPerlConfig.PERL_EXEC + " " +
                    MetaGenoPerlConfig.PERL_BIN_DIR + "/" + btabCmd +
                    " --input_file \"$OUTPUTFILE\".list" +
                    " --output_file \"$OUTPUTFILE\".btab" +
                    "\n";

            script.append(cmd);
            script.append(echoCmd);
            script.append(btabCmdStr);

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        processData.putItem("METAGENE_RESULT_NODE_ID", resultFileNode.getObjectId());
        try {
            // First, create consolidated result files
            List<File> rawFiles = new ArrayList<File>();
            List<File> btabFiles = new ArrayList<File>();
            List<File> listFiles = new ArrayList<File>();
            for (File file : outputFiles) {
                rawFiles.add(new File(file.getAbsolutePath() + ".out"));
                btabFiles.add(new File(file.getAbsolutePath() + ".btab"));
                listFiles.add(new File(file.getAbsolutePath() + ".list"));
            }
            File rawResultFile = new File(resultFileNode.getFilePathByTag(MetageneResultNode.TAG_RAW_OUTPUT));
            File btabResultFile = new File(resultFileNode.getFilePathByTag(MetageneResultNode.TAG_BTAB_OUTPUT));
            FileUtil.concatFilesUsingSystemCall(rawFiles, rawResultFile);
            FileUtil.concatFilesUsingSystemCall(btabFiles, btabResultFile);
            // Next, clear incremental fasta files
            for (File f : rawFiles) {
                f.delete();
            }
            for (File f : btabFiles) {
                f.delete();
            }
            for (File f : listFiles) {
                f.delete();
            }
            metageneBtabFile = btabResultFile;
            processData.putItem("METAGENE_BTAB_FILE", metageneBtabFile);
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

}