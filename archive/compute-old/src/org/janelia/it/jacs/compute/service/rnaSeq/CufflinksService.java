
package org.janelia.it.jacs.compute.service.rnaSeq;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.rnaSeq.CufflinksTask;
import org.janelia.it.jacs.model.user_data.GtfFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.SamFileNode;
import org.janelia.it.jacs.model.user_data.rnaSeq.CufflinksResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 4, 2010
 * Time: 3:31:19 PM
 * From jacs.properties
 * # Cufflinks
 Cufflinks.CufflinksCmd=/usr/local/bin/cufflinks
 Cufflinks.Queue=-l default
 Cufflinks.ThreadsPerJob=4
 Cufflinks.SortSplitSize=1000000
 */
public class CufflinksService extends SubmitDrmaaJobService {
    // IService constants
    public static String CUFFLINKS_TASK = "CUFFLINKS_TASK";

    private static final String cufflinksCmd = SystemConfigurationProperties.getString("Cufflinks.CufflinksCmd");
    private static final String queueName = SystemConfigurationProperties.getString("Cufflinks.Queue");
    protected static int threadsPerJob = SystemConfigurationProperties.getInt("Cufflinks.ThreadsPerJob");
    protected static String scratchDirPath = SystemConfigurationProperties.getString("SystemCall.ScratchDir");
    protected static Long sortSplitSize = SystemConfigurationProperties.getLong("Cufflinks.SortSplitSize");

    private SamFileNode samInputNode;
    private GtfFileNode gtfOptionalInputNode;
    CufflinksTask cufflinksTask;
    CufflinksResultNode cufflinksResultNode;
    private String sessionName;
    File targetSamFile;
    File targetGtfOptionalFile;
    File resultDir;

    public static CufflinksTask createDefaultTask() {
        return new CufflinksTask();
    }

    protected void init(IProcessData processData) throws Exception {
        cufflinksTask = getCufflinksTask(processData);
        task = cufflinksTask;
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        cufflinksResultNode = createResultFileNode();
        resultFileNode = cufflinksResultNode;
        super.init(processData);
        initCufflinksInputNodes();
        preProcess();
        logger.info(this.getClass().getName() + " cufflinksTaskId=" + task.getObjectId() + " init() end");
    }

    protected String getGridServicePrefixName() {
        return "cufflinksService";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    private CufflinksTask getCufflinksTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Object possibleTask = processData.getItem(CUFFLINKS_TASK);
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
                return (CufflinksTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getCufflinksTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private CufflinksResultNode createResultFileNode() throws ServiceException, IOException, DaoException {
        CufflinksResultNode resultFileNode;

        // Check if we already have a result node for this task
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof CufflinksResultNode) {
                return (CufflinksResultNode) node;
            }
        }

        // Create new node
        resultFileNode = new CufflinksResultNode(task.getOwner(), task,
                "CufflinksResultNode", "CufflinksResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        resultDir = new File(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    private void initCufflinksInputNodes() throws Exception {
        Long samInputNodeId = new Long(cufflinksTask.getParameter(CufflinksTask.PARAM_sam_file_input_node_id));
        String gtfOptionalInputNodeIdString = cufflinksTask.getParameter(CufflinksTask.PARAM_gtf_node_id);
        if (gtfOptionalInputNodeIdString != null && !gtfOptionalInputNodeIdString.equals("")) {
            Long gtfOptionalInputNodeId = new Long(gtfOptionalInputNodeIdString);
            gtfOptionalInputNode = (GtfFileNode) computeDAO.getNodeById(gtfOptionalInputNodeId);
        }
        samInputNode = (SamFileNode) computeDAO.getNodeById(samInputNodeId);
    }

    private void preProcess() throws Exception {
        logger.info("CufflinksService preProcess() start");

        // Step 1: copy source files into working directory - these will be erased later
        File sourceSamFile = new File(samInputNode.getFilePathByTag(SamFileNode.TAG_SAM));
        targetSamFile = new File(resultFileNode.getDirectoryPath(), "input.sam");
        logger.info("Copying " + sourceSamFile.getAbsolutePath() + " to " + targetSamFile.getAbsolutePath());
        FileUtil.copyFileUsingSystemCall(sourceSamFile, targetSamFile);
        checkAndSortSamFile(targetSamFile);

        if (gtfOptionalInputNode != null) {
            File sourceGtfFile = new File(gtfOptionalInputNode.getFilePathByTag(GtfFileNode.TAG_GTF));
            targetGtfOptionalFile = new File(resultFileNode.getDirectoryPath(), "input.gtf");
            logger.info("Copying " + sourceGtfFile.getAbsolutePath() + " to " + targetGtfOptionalFile.getAbsolutePath());
            FileUtil.copyFileUsingSystemCall(sourceGtfFile, targetGtfOptionalFile);
        }
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(cufflinksTask, writer);
        writeConfigFile(targetSamFile, targetGtfOptionalFile, 1);
        setJobIncrementStop(1);
    }

    private int writeConfigFile(File samFile, File gtfFile, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }
        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);

        try {
            configWriter.println(samFile.getParentFile().getAbsolutePath());  /* should be working dir */

            String commandOptionsString = cufflinksTask.generateCommandOptions((gtfFile == null ? null : gtfFile.getAbsolutePath()));

            if (threadsPerJob > 1) {
                commandOptionsString = commandOptionsString + " -p " + threadsPerJob + " ";
            }

            logger.info("Cufflinks using command options string=" + commandOptionsString);

            configWriter.println(commandOptionsString);

            configWriter.println(samFile.getAbsolutePath());
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
        if (threadsPerJob > 1) {
            jt.setNativeSpecification("-pe threaded " + threadsPerJob);
        }
    }

    private void createShellScript(CufflinksTask cufflinksTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();

            script.append("read WORKINGDIR\n");
            script.append("read OPTIONS\n");
            script.append("read SAMFILE\n");

            String scriptChgDirCmd = "cd \"$WORKINGDIR\"\n";

            String scriptCufflinksCmd = cufflinksCmd + " $OPTIONS $SAMFILE\n";

            script.append(scriptChgDirCmd);
            script.append(scriptCufflinksCmd);
            writer.write(script.toString());

        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        processData.putItem("CUFFLINKS_RESULT_NODE_ID", resultFileNode.getObjectId());
    }

    public void checkAndSortSamFile(File samFile) throws Exception {
        SystemCall sc = new SystemCall(null, new File(scratchDirPath), logger);
        File sortCheckFile = new File(samFile.getAbsolutePath() + ".checksort");
        String checkSortCmd = "LC_ALL=\"C\" sort -c -k 3,3 -k 4,4n "+samFile.getAbsolutePath()+" >& "+sortCheckFile.getAbsolutePath();
        int ev = sc.execute(checkSortCmd, false);
        if (ev!=0) {
            logger.info("SystemCall produced non-zero exit value="+checkSortCmd+" - assuming this is because the check itself failed, and is therefore OK");
        }
        if (!sortCheckFile.exists()) {
            throw new Exception("Could not locate sortCheckFile="+sortCheckFile.getAbsolutePath());
        }
        if (sortCheckFile.length()>0) {
            logger.info("sortCheckFile has non-zero length so beginning sort process");
            // Implies the file must be sorted
            File sortDir = new File(samFile.getParentFile(), "samsort");
            FileUtil.ensureDirExists(sortDir.getAbsolutePath());
            File tmpSamFile=new File(sortDir, samFile.getName());
            String mvCmd = "mv "+samFile.getAbsolutePath()+" "+tmpSamFile.getAbsolutePath();
            ev = sc.execute(mvCmd, false);
            if (ev!=0) {
                throw new Exception("SystemCall failed="+mvCmd);
            }
            String splitCmd = "{ cd "+sortDir.getAbsolutePath()+"; split -l "+sortSplitSize+" "+tmpSamFile.getAbsolutePath()+"; }";
            ev = sc.execute(splitCmd, false);
            if (ev!=0) {
                throw new Exception("SystemCall failed="+splitCmd);
            }
            File accumSamFile=new File(tmpSamFile.getAbsolutePath()+".accum");
            File finalSamFile=new File(tmpSamFile.getAbsolutePath()+".final");
            if (!finalSamFile.createNewFile()) {
                throw new Exception("Could not create file="+finalSamFile.getAbsolutePath());
            }
            File[] splitFiles = sortDir.listFiles(new FilenameFilter() {
                public boolean accept(File file, String name) {
                    if (name.startsWith("x")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            } );
            if (splitFiles.length==0) {
                throw new Exception("Unexpectedly found zero files in sortDir="+sortDir.getAbsolutePath());
            }
            for (File splitFile : splitFiles) {
                logger.info("Sorting split file="+splitFile.getAbsolutePath());
                String sortCmd = "LC_ALL=\"C\" sort -k 3,3 -k 4,4n "+splitFile.getAbsolutePath()+" > "+
                        splitFile.getAbsolutePath()+".sorted";
                ev = sc.execute(sortCmd, false);
                if (ev!=0) {
                    throw new Exception("SystemCall failed="+sortCmd);
                }
                logger.info("Merging split file="+splitFile.getAbsolutePath()+".sorted");
                String mergeCmd = "LC_ALL=\"C\" sort -m -k 3,3 -k 4,4n "+splitFile.getAbsolutePath()+".sorted "+
                        finalSamFile.getAbsolutePath()+" > "+accumSamFile.getAbsolutePath();
                ev = sc.execute(mergeCmd, false);
                if (ev!=0) {
                    throw new Exception("SystemCall failed="+mergeCmd);
                }
                String resetFileCmd = "mv "+accumSamFile.getAbsolutePath()+" "+finalSamFile.getAbsolutePath();
                ev = sc.execute(resetFileCmd, false);
                if (ev!=0) {
                    throw new Exception("SystemCall failed="+resetFileCmd);
                }
            }
            String replaceCmd = "mv "+finalSamFile.getAbsolutePath()+" "+samFile.getAbsolutePath();
            logger.info("Replacing original SamFile with cmd="+replaceCmd);
            ev = sc.execute(replaceCmd, false);
            if (ev!=0) {
                throw new Exception("SystemCall failed="+replaceCmd);
            }
            // Cleanup
            for (File splitFile : splitFiles) {
                File sortFile=new File(splitFile.getAbsolutePath()+".sorted");
                if (sortFile.delete()) {
                    logger.info("Deleted sortFile="+sortFile.getAbsolutePath());
                } else {
                    logger.error("Warning: could not delete sortFile="+sortFile.getAbsolutePath());
                }
                if (splitFile.delete()) {
                    logger.info("Deleted splitFile="+splitFile.getAbsolutePath());
                } else {
                    logger.error("Warning: could not delete splitFile="+splitFile.getAbsolutePath());
                }
            }
            if (tmpSamFile.delete()) {
                logger.info("Deleted tmpSamFile="+tmpSamFile.getAbsolutePath());
            } else {
                logger.error("Warning: could not delete tmpSamFile="+tmpSamFile.getAbsolutePath());
            }
            if (sortDir.delete()) {
                logger.info("Deleted sortDir="+sortDir.getAbsolutePath());
            } else {
                logger.error("Warning: could not delete sortDir="+sortDir.getAbsolutePath());
            }
            logger.info("Finished sorting samFile="+samFile.getAbsolutePath());
        } else {
            logger.info("SamFile "+samFile.getAbsolutePath()+" does not need to be sorted - skipping sort step");
        }
        sortCheckFile.delete();
    }

}
