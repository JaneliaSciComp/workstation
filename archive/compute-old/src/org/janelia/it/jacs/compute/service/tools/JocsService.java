
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
import org.janelia.it.jacs.model.tasks.tools.JocsTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.JocsResultNode;
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
 * Date: Aug 26, 2010
 * Time: 3:31:05 PM
 * From jacs.properties
 * #JOCs
 Jocs.ParseBsmlCmd = /usr/local/devel/ANNOTATION/ekelsey/VICS/trunk/compute/scripts/pangenome/CogBsmlLoader
 Jocs.Queue=-l medium
 Jocs.MaxEntriesPerJob=200
 Jocs.BestHit = /usr/local/devel/ANNOTATION/ard/ergatis-v2r10b1/bin/best_hit
 */
public class JocsService extends SubmitDrmaaJobService {

    public static final String JOCS_PARSEBSML_CMD = SystemConfigurationProperties.getString("Jocs.ParseBsmlCmd");
    public static final String JOCS_BESTHIT_CMD = SystemConfigurationProperties.getString("Jocs.BestHit");
    public static final String JOCS_COG2FASTA_CMD = SystemConfigurationProperties.getString("Jaccard.Cog2FastaCmd");

    public static final String JOCS_QUEUE = SystemConfigurationProperties.getString("Jocs.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("Jocs.MaxEntriesPerJob");
      
    JocsTask jocsTask;
    JocsResultNode jocsResultNode;
    private String sessionName;
    String fileId;
    SystemCall sc;

    protected String getGridServicePrefixName() {
        return "jocs";
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
        createShellScript(jocsTask, writer);

        File parseBsmlOutput = new File(jocsResultNode.getFilePathByTag(JocsResultNode.TAG_BTAB_OUTPUT));
        File bestHitOutput = new File(jocsResultNode.getFilePathByTag(JocsResultNode.TAG_COG_OUTPUT));

        writeConfigFile(parseBsmlOutput, bestHitOutput, 1);
        
        setJobIncrementStop(1);
    }

    private void writeConfigFile(File parseBsmlOutput, File bestHitOutput, int configIndex) throws IOException {
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }
        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {
            configWriter.println(parseBsmlOutput);
            configWriter.println(bestHitOutput);
        }
        finally {
            configWriter.close();
        }
    }

    private String buildConfigFileName(int configIndex) {
        return getConfigPrefix() + configIndex;
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("setQueue = " + JOCS_QUEUE);
        jt.setNativeSpecification(JOCS_QUEUE);
    }

    private void createShellScript(JocsTask jocsTask, FileWriter writer)
            throws IOException, ParameterException {
        try {

            String cogs2FastaOut = (new File(resultFileNode.getDirectoryPath())).toString();

            StringBuffer script = new StringBuffer();
            script.append("read PARSEBSMLOUT\n");
            script.append("read BESTHITOUT\n");

            String parseBsmlOptions = jocsTask.generateParseBsmlOptions();
            String bestHitOptions   =  jocsTask.generateBestHitOptions();
            String cog2FastaOptions = jocsTask.generateCogs2FastaOptions();

            String parseBsmlOutputString = "\"$PARSEBSMLOUT\"";
            String bestHitOutputString = "\"$BESTHITOUT\"";

            String parseBsmlStr = JOCS_PARSEBSML_CMD + " " + parseBsmlOptions + " --outfile=" + parseBsmlOutputString;
            String bestHitStr = JOCS_BESTHIT_CMD + " " + bestHitOptions + " -i " + parseBsmlOutputString + " > " + bestHitOutputString;  
            String cogs2FastaStr = JOCS_COG2FASTA_CMD + " " + cog2FastaOptions + " --cogFile=" + bestHitOutputString + " --outputDir=" + cogs2FastaOut;

            script.append(parseBsmlStr).append("\n");
            script.append(bestHitStr).append("\n");
            script.append(cogs2FastaStr).append("\n");
            
            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File jocsFastaList = new File(jocsResultNode.getFilePathByTag(JocsResultNode.TAG_LIST_OUTPUT));
            File jocsDir = new File(jocsResultNode.getDirectoryPath());

            logger.info("postProcess for jocsTaskId=" + jocsTask.getObjectId() + " and resultNodeDir=" + jocsDir.getAbsolutePath());

            StringBuffer fastaBuffer = new StringBuffer();
            FileWriter fastaWriter = new FileWriter(jocsFastaList);

            ArrayList<File> fsaFiles = new ArrayList<File>();
            findFilesByFilter(jocsDir, fsaFilter, fsaFiles, 0, 2);

            for (File f : fsaFiles) {

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

    // filter is based on searchTag
    FilenameFilter fsaFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(JocsResultNode.TAG_FASTA_OUTPUT);
        }
    };
    
    public void findFilesByFilter(File dir, FilenameFilter filter, List<File> fileList, int depth, int maxdepth) {
        depth++;
        if (!(depth > maxdepth)) {


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
                    findFilesByFilter(g, filter, fileList, depth, maxdepth);
                }
            }
        }
    }
    
    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        jocsTask = getJocsTask(processData);
        task = jocsTask;
        Task tmpParentTask = ProcessDataHelper.getTask(processData);
        // If this Priam call is part of a pipeline, drop the results in the annotation directory
        if (null != tmpParentTask) {
            FileNode tmpParentNode = (FileNode) computeDAO.getResultNodeByTaskId(tmpParentTask.getObjectId());
            if (null != tmpParentNode) {
                sessionName = tmpParentNode.getSubDirectory() + File.separator + tmpParentNode.getObjectId();
                if (tmpParentNode.getRelativeSessionPath() != null) {
                    sessionName = tmpParentNode.getRelativeSessionPath() + File.separator + sessionName;
                }
                logger.info("Found non-null parentNode for JocsTask=" + jocsTask.getObjectId() + " - setting sessionName=" + sessionName);
            }
        }
        jocsResultNode = createResultFileNode();
        logger.info("Setting jocsResultNode=" + jocsResultNode.getObjectId() + " path=" + jocsResultNode.getDirectoryPath());
        resultFileNode = jocsResultNode;
        // super.init() must be called after the resultFileNode is set or it will throw an Exception
        super.init(processData);
        logger.info(this.getClass().getName() + " sessionName=" + sessionName);

        logger.info(this.getClass().getName() + " jocsTaskId=" + task.getObjectId() + " init() end");
    }

    private JocsTask getJocsTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Task pdTask = ProcessDataHelper.getTask(processData);
            if (pdTask != null) {
                logger.info("Found generic Task, possibly a non-null JocsTask from ProcessData taskId=" + pdTask.getObjectId());
                taskId = pdTask.getObjectId();
            }
            if (taskId != null) {
                return (JocsTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getJocsTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private JocsResultNode createResultFileNode() throws Exception {
        JocsResultNode resultFileNode;

        // Check if we already have a result node for this task
        if (task == null) {
            logger.info("task is null - therefore createResultFileNode() returning null result node");
            return null;
        }
        logger.info("Checking to see if there is already a result node for task=" + task.getObjectId());
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof JocsResultNode) {
                logger.info("Found already-extant jocsResultNode path=" + ((JocsResultNode) node).getDirectoryPath());
                return (JocsResultNode) node;
            }
        }

        // Create new node
        logger.info("Creating JocsResultNode with sessionName=" + sessionName);
        resultFileNode = new JocsResultNode(task.getOwner(), task,
                "JocsResultNode", "JocsResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }
    
}