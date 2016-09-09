
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
import org.janelia.it.jacs.compute.service.metageno.SimpleGridJobRunner;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.rnaSeq.TophatTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FastqDirectoryNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.rnaSeq.TophatResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * From jacs.properties
 * # Tophat
 Tophat.TophatCmd=/usr/local/bin/tophat
 Tophat.SamtoolsCmd=/usr/local/bin/samtools
 Tophat.BowtieBuildCmd=/usr/local/bin/bowtie-build
 Tophat.Queue=-l default
 */
public class TophatService extends SubmitDrmaaJobService {
    // IService constants
    public static String TOPHAT_TASK = "TOPHAT_TASK";

    private static final String tophatCmd = SystemConfigurationProperties.getString("Tophat.TophatCmd");
    private static final String samtoolsCmd = SystemConfigurationProperties.getString("Tophat.SamtoolsCmd");
    private static final String bowtieBuildCmd = SystemConfigurationProperties.getString("Tophat.BowtieBuildCmd");
    private static final String queueName = SystemConfigurationProperties.getString("Tophat.Queue");
    protected static String scratchDirPath = SystemConfigurationProperties.getString("SystemCall.ScratchDir");

    private static final String resultFilename = TophatResultNode.BASE_OUTPUT_FILENAME;

    private FastqDirectoryNode inputReadNode;
    private FastaFileNode inputGenomeNode;
    TophatTask tophatTask;
    TophatResultNode tophatResultNode;
    private String sessionName;
    File targetReferenceFile;
    List<File> targetReadFileList = new ArrayList<File>();
    File resultDir;
    Boolean isPaired;

    public static TophatTask createDefaultTask() {
        return new TophatTask();
    }

    protected void init(IProcessData processData) throws Exception {
        tophatTask = getTophatTask(processData);
        task = tophatTask;
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        tophatResultNode = createResultFileNode();
        resultFileNode = tophatResultNode;
        super.init(processData);
        initTophatInputNodes();
        preProcess();
        logger.info(this.getClass().getName() + " tophatTaskId=" + task.getObjectId() + " init() end");
    }

    protected String getGridServicePrefixName() {
        return "tophatService";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    private TophatTask getTophatTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Object possibleTask = processData.getItem(TOPHAT_TASK);
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
                return (TophatTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getTophatTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private TophatResultNode createResultFileNode() throws ServiceException, IOException, DaoException {
        TophatResultNode resultFileNode;

        // Check if we already have a result node for this task
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof TophatResultNode) {
                return (TophatResultNode) node;
            }
        }

        // Create new node
        resultFileNode = new TophatResultNode(task.getOwner(), task,
                "TophatResultNode", "TophatResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        resultDir = new File(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    private void initTophatInputNodes() throws Exception {
        Long inputReadId = new Long(tophatTask.getParameter(TophatTask.PARAM_reads_fastQ_node_id));
        Long inputGenomeId = new Long(tophatTask.getParameter(TophatTask.PARAM_refgenome_fasta_node_id));
        inputReadNode = (FastqDirectoryNode) computeDAO.getNodeById(inputReadId);
        isPaired = inputReadNode.isPairedData();
        inputGenomeNode = (FastaFileNode) computeDAO.getNodeById(inputGenomeId);
    }

    private void preProcess() throws Exception {
        logger.info("TophatService preProcess() start");

        // Step 1: copy source files into working directory - these will be erased later
        File sourceFastaReferenceFile = new File(inputGenomeNode.getFastaFilePath());
        targetReferenceFile = new File(resultFileNode.getDirectoryPath(), "reference.fa");
        logger.info("Copying " + sourceFastaReferenceFile.getAbsolutePath() + " to " + targetReferenceFile.getAbsolutePath());
        FileUtil.copyFileUsingSystemCall(sourceFastaReferenceFile, targetReferenceFile);

        File[] sourceFastqFiles = new File(inputReadNode.getDirectoryPath()).listFiles();
        int readFileCount = 0;
        for (File f : sourceFastqFiles) {
            if (f.getName().endsWith(FastqDirectoryNode.TAG_FASTQ)) {
                File targetReadFile = new File(resultFileNode.getDirectoryPath(), f.getName());
                logger.info("Copying " + f.getAbsolutePath() + " to " + targetReadFile.getAbsolutePath());
                FileUtil.copyFileUsingSystemCall(f, targetReadFile);
                readFileCount++;
                targetReadFileList.add(targetReadFile);
            }
        }
        if (readFileCount == 0) {
            throw new Exception("Could not find any files ending in " + FastqDirectoryNode.TAG_FASTQ + " to copy from fastq directory node=" +
                    inputReadNode.getDirectoryPath());
        }

        // Step 2: generate reference list - this produces <fasta ref file>.fai
        String generateReferenceStr = samtoolsCmd +
                " faidx" +
                " " + targetReferenceFile.getAbsolutePath();
        SimpleGridJobRunner job = new SimpleGridJobRunner(resultDir, generateReferenceStr, queueName,
                tophatTask.getParameter("project"), tophatTask.getObjectId());
        if (!job.execute()) {
            throw new Exception("Grid job failed with cmd=" + generateReferenceStr);
        }

        // Step 3: build bowtie index - generates set of files <base ref name>.<lane#>.ebwt, and also <base ref name>.rev.<lane#>.ebwt
        String refDbName = getBaseFilename(targetReferenceFile.getName());
        String bowtieBuildStr = bowtieBuildCmd +
                " " + targetReferenceFile.getAbsolutePath() +
                " " + refDbName;
        job = new SimpleGridJobRunner(resultDir, bowtieBuildStr, queueName,
                tophatTask.getParameter("project"), tophatTask.getObjectId());
        if (!job.execute()) {
            throw new Exception("Grid job failed with cmd=" + bowtieBuildStr);
        }

    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(tophatTask, writer);

        int configIndex = 1;
        for (File inputFile : targetReadFileList) {
            // Note: the "_2." files are implicitly run in the paired case by their left-mated files "_1."
            if (!isPaired || (isPaired && (!inputFile.getName().contains("_2.")))) {
                configIndex = writeConfigFile(
                        inputFile,
                        getBaseFilename(targetReferenceFile.getName()),
                        targetReferenceFile.getName() + ".fai",
                        configIndex);
                configIndex++;
            }
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

    private int writeConfigFile(File inputFile, String referenceBasename, String refIndexName, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }
        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {
            configWriter.println(inputFile.getParentFile().getAbsolutePath());  /* should be working dir */
            if (isPaired) {
                configWriter.println(getPairedBaseFromFilename(inputFile.getName())); /* e.g., s_1 from s_1_1, s_1_2 */
            }
            else {
                configWriter.println(getBaseFilename(inputFile.getName())); /* e.g., s1_1 */
            }
            if (isPaired) {
                configWriter.println("--num-threads 1 -r " + inputReadNode.getMateMeanInnerDistance());
            }
            else {
                configWriter.println("--num-threads 1");
            }
            configWriter.println(referenceBasename); /* e.g., 'nucleotide' */
            configWriter.println(refIndexName); /* e.g., 'reference.fa.fai' */
            if (isPaired) {
                configWriter.println(inputFile.getName() + " " + getRightMateFilenameFromLeft(inputFile.getName()));
            }
            else {
                configWriter.println(inputFile.getName()); /* e.g., s1_1.fq */
            }
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

    private void createShellScript(TophatTask tophatTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();

            script.append("read WORKINGDIR\n");
            script.append("read READBASENAME\n");
            script.append("read OPTIONS\n");
            script.append("read REFBASENAME\n");
            script.append("read REFINDEX\n");
            script.append("read READFULLNAME\n");

            String scriptChgDirCmd = "cd \"$WORKINGDIR\"\n";

            String scriptTophatCmd = tophatCmd + " --output-dir $READBASENAME $OPTIONS $REFBASENAME $READFULLNAME\n";

            String scriptSamCmd = samtoolsCmd + " import $REFINDEX $READBASENAME/accepted_hits.sam $READBASENAME.accepted_hits.bam\n";

            String scriptSortCmd = samtoolsCmd + " sort $READBASENAME.accepted_hits.bam $READBASENAME\n";

            script.append(scriptChgDirCmd);
            script.append(scriptTophatCmd);
            script.append(scriptSamCmd);
            script.append(scriptSortCmd);

            writer.write(script.toString());

        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        processData.putItem("TOPHAT_RESULT_NODE_ID", resultFileNode.getObjectId());
        try {

            SystemCall sc = new SystemCall(null, new File(scratchDirPath), logger);

            // Step 1: merge all .bam files
            FileFilter mergeFilter = new MergeFilter();
            File mergeInput[] = new File(scratchDirPath).listFiles(mergeFilter);
            String mergeBamStr;
            if ( mergeInput.length < 1 ) {
                throw new Exception("No *.accepted_hits.bam files generated.");
            } else if ( mergeInput.length == 1 ) {
                mergeBamStr = "cp " + mergeInput[0].getName() + " " + resultFilename + ".bam";
            } else {
                mergeBamStr = samtoolsCmd + " merge " + resultFilename + ".bam *.accepted_hits.bam";
            }
            SimpleGridJobRunner job = new SimpleGridJobRunner(resultDir, mergeBamStr, queueName,
                    tophatTask.getParameter("project"), tophatTask.getObjectId());
            if (!job.execute()) {
                throw new Exception("Grid job failed with cmd=" + mergeBamStr);
            }

            // Step 2: convert bam file to sam file
            String convertSamStr = samtoolsCmd + " view -o " + resultFilename + ".sam " + resultFilename + ".bam";
            job = new SimpleGridJobRunner(resultDir, convertSamStr, queueName,
                    tophatTask.getParameter("project"), tophatTask.getObjectId());
            if (!job.execute()) {
                throw new Exception("Grid job failed with cmd=" + convertSamStr);
            }

            // Step 3: move all .wig files to top-level
            for (File inputFile : targetReadFileList) {
                File localDir = getLocalDirectoryFromFilename(inputFile.getName());
                if (!localDir.isDirectory()) {
                    throw new Exception("Could not find expected directory=" + localDir.getAbsolutePath());
                }
                File wigFile = new File(localDir, "coverage.wig");
                File wigTargetFile = new File(resultDir, resultFilename + ".wig." + localDir.getName());
                if (wigFile.exists()) {
                    String mvWigStr = "mv " + wigFile.getAbsolutePath() + " " + wigTargetFile.getAbsolutePath();
                    int ev = sc.execute(mvWigStr, false);
                    if (ev != 0) {
                        throw new Exception("SystemCall produced non-zero exit value=" + mvWigStr);
                    }
                }
                else {
                    logger.info("Could not locate wig file=" + wigFile.getAbsolutePath() + " so assuming no hits were generated");
                    wigTargetFile.createNewFile();
                }
            }

            // Step 4: move all .bed files to top-level
            for (File inputFile : targetReadFileList) {
                File localDir = getLocalDirectoryFromFilename(inputFile.getName());
                if (!localDir.isDirectory()) {
                    throw new Exception("Could not find expected directory=" + localDir.getAbsolutePath());
                }
                File bedFile = new File(localDir, "junctions.bed");
                File bedTargetFile = new File(resultDir, resultFilename + ".bed." + localDir.getName());
                if (bedFile.exists()) {
                    String mvBedStr = "mv " + bedFile.getAbsolutePath() + " " + bedTargetFile.getAbsolutePath();
                    int ev = sc.execute(mvBedStr, false);
                    if (ev != 0) {
                        throw new Exception("SystemCall produced non-zero exit value=" + mvBedStr);
                    }
                }
                else {
                    logger.info("Could not locate bed file=" + bedFile.getAbsolutePath() + " so assuming no hits were generated");
                    bedTargetFile.createNewFile();
                }
            }

        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    private class MergeFilter implements FileFilter {
        public boolean accept(File file) {
            if ( file.getName().endsWith(".accepted_hits.bam") ) {
                return true;
            } else {
                return false;
            }
        }
    }

    File getLocalDirectoryFromFilename(String filename) {
        File localDir;
        if (isPaired) {
            localDir = new File(resultDir, getPairedBaseFromFilename(filename));
        }
        else {
            localDir = new File(resultDir, getBaseFilename(filename));
        }
        return localDir;
    }

    String getBaseFilename(String filename) {
        return filename.substring(0, filename.lastIndexOf("."));
    }

    String getPairedBaseFromFilename(String filename) {
        return filename.substring(0, filename.lastIndexOf("_"));
    }

    String getRightMateFilenameFromLeft(String leftMateFilename) throws Exception {
        String baseFilename = getBaseFilename(leftMateFilename);
        String extension = leftMateFilename.substring(leftMateFilename.lastIndexOf("."));
        String[] components = baseFilename.split("_");
        /*
         * We expect the following components from paired filenames:
         *  0 s1
         *  1 <1 or 2, depending on left or right, respectively>
         */
        if (components.length != 2) {
            throw new Exception("Could not parse right name from leftMateFilename=" + leftMateFilename + "  component arr size=" + components.length);
        }
        if (!components[1].equals("1")) {
            throw new Exception("Expected left-directed file but this file does not meet left-direction naming convention=" + leftMateFilename);
        }
        return components[0] + "_2" + extension;
    }
}
