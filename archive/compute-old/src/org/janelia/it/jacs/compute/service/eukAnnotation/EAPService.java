
package org.janelia.it.jacs.compute.service.eukAnnotation;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.eukAnnotation.EAPTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.eukAnnotation.EAPResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jul 26, 2010
 * Time: 12:41:05 PM
 * From jacs.properties
 * # EAP
 EAP.CreateComputeDBCmd=/usr/local/devel/ANNOTATION/EAP/pipeline/createComputeDB.pl
 EAP.AddQueryFastaCmd=/usr/local/devel/ANNOTATION/EAP/pipeline/addQueryFasta.pl
 EAP.ComputeCmd=/usr/local/devel/ANNOTATION/EAP/pipeline/compute.pl
 EAP.GetAnnotationCmd=/usr/local/devel/ANNOTATION/EAP/pipeline/get_annotation.pl
 EAP.SummarizeEvidenceCmd=/usr/local/devel/ANNOTATION/EAP/pipeline/summarize_evidence.pl
 EAP.Queue=-l medium
 EAP.MaxEntriesPerJob=200
 EAP.LocalTmpDir=/tmp
 */
public class EAPService extends SubmitDrmaaJobService {

    public static final String EAP_CREATE_DB_CMD = SystemConfigurationProperties.getString("EAP.CreateComputeDBCmd");
    public static final String EAP_ADD_FASTA_CMD = SystemConfigurationProperties.getString("EAP.AddQueryFastaCmd");
    public static final String EAP_COMPUTE_CMD = SystemConfigurationProperties.getString("EAP.ComputeCmd");
    public static final String EAP_GET_ANNOTATION_CMD = SystemConfigurationProperties.getString("EAP.GetAnnotationCmd");
    public static final String EAP_SUMMARIZE_CMD = SystemConfigurationProperties.getString("EAP.SummarizeEvidenceCmd");
    public static final String EAP_QUEUE = SystemConfigurationProperties.getString("EAP.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("EAP.MaxEntriesPerJob");
    public static final String LOCAL_TMP_DIR = SystemConfigurationProperties.getString("EAP.LocalTmpDir");

    private static final String resultFilename = EAPResultNode.BASE_OUTPUT_FILENAME;

    EAPTask eapTask;
    EAPResultNode eapResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    protected String getGridServicePrefixName() {
        return "eap";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        // In the current configuration this file is not used
        String tempOutputFileName = FileUtil.checkFilePath(LOCAL_TMP_DIR) + File.separator + resultFilename + ".$$";
        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work

        File computeDbFile = new File(resultFileNode.getDirectoryPath() + File.separator + "computeDB");
        File annotationDir = new File(resultFileNode.getDirectoryPath() + File.separator + "annotationDir");
        File evidenceFile  = new File(annotationDir.getAbsolutePath() + File.separator + new File(computeDbFile.getAbsolutePath()).getName() + ".evidence");

        createShellScript(eapTask, tempOutputFileName, writer);

        writeConfigFile(computeDbFile, annotationDir, evidenceFile, 1);

        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);
    }

    private int writeConfigFile(File computeDb, File annotationDir, File evidenceFile, int configIndex) throws IOException {
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }
        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {

            configWriter.println(computeDb.getAbsolutePath());
            configWriter.println(annotationDir.getAbsolutePath());
            configWriter.println(evidenceFile.getAbsolutePath());

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
        logger.info("setQueue = " + EAP_QUEUE);
        jt.setNativeSpecification(EAP_QUEUE);
    }

    private void createShellScript(EAPTask eapTask, String tmpFilename /* not used */, FileWriter writer)
            throws IOException, ParameterException {
        try {
            String inputFileString = (new File (task.getParameter(EAPTask.PARAM_input_fasta))).toString();

            StringBuffer script = new StringBuffer();
            script.append("read COMPUTEDB\n");
            script.append("read ANNOTATIONDIR\n");
            script.append("read EVIDENCEFILE\n");

            String computeDbString="\"$COMPUTEDB\"";
            String annotationDirString="\"$ANNOTATIONDIR\"";
            String evidenceFileString="\"$EVIDENCEFILE\"";

            //  Create the sqlite db:
            String createDbStr = EAP_CREATE_DB_CMD + " -D " + computeDbString;
            script.append(createDbStr).append("\n");

            //  Load the Peptide FASTA into the SQLite Database
            String addFastaOptions = eapTask.generateAddQueryFastaOptions();
            String addFastaStr = EAP_ADD_FASTA_CMD + " -D " + computeDbString + " -F " + inputFileString + " " + addFastaOptions;
            script.append(addFastaStr).append("\n");

            //  Launch the Computes
            String computeOptions = eapTask.generateComputeOptions();
            String computeCmdStr = EAP_COMPUTE_CMD + " -D " + computeDbString + " " + computeOptions;
            script.append(computeCmdStr).append("\n");

            //  Create the directory using mkdir
            String makeDirStr = "mkdir -m 777 " + annotationDirString;
            script.append(makeDirStr).append("\n");

            //  Pull out annotations
            String getAnnotOptions = eapTask.generateGetAnnotOptions();
            String getAnnotCmdStr  = EAP_GET_ANNOTATION_CMD + " -d " + computeDbString + " -o " + annotationDirString + " " + getAnnotOptions;
            script.append(getAnnotCmdStr).append("\n");

            //  Summarize results
            String summarizeResultsStr = EAP_SUMMARIZE_CMD + " " + inputFileString + " " + evidenceFileString;
            script.append(summarizeResultsStr).append("\n");

            writer.write(script.toString());
        } catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File eapDir = new File(eapResultNode.getDirectoryPath());
            logger.info("postProcess for eapTaskId="+eapTask.getObjectId()+" and resultNodeDir="+eapDir.getAbsolutePath());
        } catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        eapTask = getEAPTask(processData);
        task = eapTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this EAP call is part of a larger pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for EAPTask=" + eapTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        eapResultNode = createResultFileNode();
        logger.info("Setting eapResultNode=" + eapResultNode.getObjectId() + " path=" + eapResultNode.getDirectoryPath());
        resultFileNode = eapResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);
        logger.info(this.getClass().getName() + " eapTaskId=" + task.getObjectId() + " init() end");
    }

    private EAPTask getEAPTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null EAPTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (EAPTask) computeDAO.getTaskById(taskId);
            }
            return null;
        } catch (Exception e) {
            logger.error("Received exception in getEAPTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private EAPResultNode createResultFileNode() throws Exception {
        EAPResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof EAPResultNode) {
                logger.info("Found already-extant eapResultNode path="+((EAPResultNode)node).getDirectoryPath());
                return (EAPResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating EAPResultNode with sessionName=" + sessionName);
        resultFileNode = new EAPResultNode(task.getOwner(), task,
                "EAPResultNode", "EAPResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

}