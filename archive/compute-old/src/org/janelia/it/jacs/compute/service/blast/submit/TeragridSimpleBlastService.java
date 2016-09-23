
package org.janelia.it.jacs.compute.service.blast;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeBaseDAO;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.ProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.file.SimpleMultiFastaSplitterService;
import src.org.janelia.it.jacs.compute.service.common.grid.submit.teragrid.TeraGridHelper;
import org.janelia.it.jacs.compute.service.metageno.MetaGenoPerlConfig;
import org.janelia.it.jacs.compute.service.metageno.SimpleGridJobRunner;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.tasks.blast.TeragridSimpleBlastTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.shared.blast.BlastResultCollectionConverter;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 17, 2009
 * Time: 3:40:05 PM
 */
public class TeragridSimpleBlastService implements IService {

    public static final String SERVICE_DIR_NAME = "SimpleBlastService";

    public static final String TG_SSH_PATH = SystemConfigurationProperties.getString("TeraGrid.SshPath"); // tsafford@tg-login.ranger.tacc.teragrid.org
    public static final String SCRATCH_DIR = SystemConfigurationProperties.getString("SystemCall.ScratchDir");
    public static final String TG_RESOURCE = SystemConfigurationProperties.getString("TeraGrid.Resource"); // -pe 12way 384
    public static final String TG_QUEUE = SystemConfigurationProperties.getString("TeraGrid.Queue"); // normal
    public static final String TG_RUNTIME = SystemConfigurationProperties.getString("TeraGrid.Runtime"); // 06:00:00
    public static final String TG_EMAIL = SystemConfigurationProperties.getString("TeraGrid.Email"); // smurphy@jcvi.org
    public static final Long TG_STAT_INTERVAL = SystemConfigurationProperties.getLong("TeraGrid.StatInterval"); // 15000
    public static final Boolean TG_REMOVE_RESULT = SystemConfigurationProperties.getBoolean("TeraGrid.RemoveResult"); // false
    public static final String TG_REMAP_SCRIPT = SystemConfigurationProperties.getString("TeraGrid.BlastXmlRemapScript"); // manage_teragrid_blast_db.pl
    public static final String MPI_BLAST_DEFAULT_QUERIES_PER_EXEC = SystemConfigurationProperties.getString("TeraGrid.MpiBlastQueriesPerExec"); // 50000
    public static final Integer MPI_RUNS_PER_QSUB = SystemConfigurationProperties.getInt("TeraGrid.MpiRunsPerQsub"); // 20

    public static final String MPI_QSUB_CMD = SystemConfigurationProperties.getString("TeraGrid.QsubCmd"); // mpi_qsub.sh
    public static final String MPI_XML_CMD = SystemConfigurationProperties.getString("TeraGrid.MpiToNcbiXmlCmd"); // mpi_to_ncbi_blast_xml.pl
    public static final String MPI_BLAST_UNIFY_COLLECTION_HITS_TO_KEEP = SystemConfigurationProperties.getString("TeraGrid.MpiBlastUnifyCollectionHitsToKeep"); // 25
    public static final String TG_MAX_QUEUED_JOBS = SystemConfigurationProperties.getString("TeraGrid.MaxQueuedJobs"); // 49
    public static final String TG_WORK_DIR = SystemConfigurationProperties.getString("TeraGrid.WorkDir"); // /work/01243/tsafford

    public static final String JAVA_PATH = SystemConfigurationProperties.getString("Java.Path");
    public static final String JAVA_MAX_MEMORY = SystemConfigurationProperties.getString("BlastServer.GridMergeSortMaximumMemoryMB");
    public static final String GRID_JAR_PATH = SystemConfigurationProperties.getFilePath("Grid.Lib.Path", "Grid.Jar.Name");
    public static final String GRID_PERSISTXML_PROCESSOR = SystemConfigurationProperties.getString("BlastServer.GridPeristsBlastResultProcessor");
    public static final String MANDATORY_BLAST_FORMAT_TYPES = SystemConfigurationProperties.getString("BlastServer.MandatoryFormatTypes");
    private static final String MERGE_SORT_PROCESSOR = SystemConfigurationProperties.getString("BlastServer.PostblastMergeSortProcessor");
    protected static String JCVI_QUEUE = SystemConfigurationProperties.getString("MgAnnotation.TeraGrid.JcviUtilityQueue");

    private Logger logger;
    String sessionName;
    ComputeBeanRemote computeBean;
    TeragridSimpleBlastTask tgBlastTask;
    Task parentTask;
    BlastResultFileNode resultNode;
    IProcessData processData;
    File resultDir;
    File inputQueryFile;
    List<File> splitInputFileList;
    String param_TeragridGrantNumber;
    String param_MpiBlastProgram;
    String param_TeragridBlastDbName;
    String param_TeragridBlastDbSize;
    String param_SqliteMapPath;
    String param_MpiBlastParams;
    SystemCall sc;
    File scratchDir;

//    List<String> tgJobIdentifierList=new ArrayList<String>();
//    List<File> mpiBlastCmdFileList=new ArrayList<File>();
//    List<File> mpiBlastCmdCaptureFileList=new ArrayList<File>();
//    List<File> blastCollectionFileList=new ArrayList<File>();

    Map<Integer, String> tgJobIdentifierMap = new HashMap<Integer, String>();
    Map<Integer, File> mpiBlastCmdFileMap = new HashMap<Integer, File>();
    Map<Integer, File> mpiBlastCmdCaptureFileMap = new HashMap<Integer, File>();
    Map<Integer, File> blastCollectionFileMap = new HashMap<Integer, File>();

    int maxTgQueuedJobs = new Integer(TG_MAX_QUEUED_JOBS);
    String tgWorkDirPath;
    Map<Integer, Integer> qsubStartMap = new HashMap<Integer, Integer>();
    Map<Integer, Integer> qsubEndMap = new HashMap<Integer, Integer>();
    Set<File> isDoneFileSet = new HashSet<File>();
    int globalRunAndWaitCount = 0;
    int maxQsubIndex = 0;

    public void execute(IProcessData processData) throws ServiceException {
        this.processData = processData;
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            scratchDir = new File(SCRATCH_DIR);
            sc = new SystemCall(null, scratchDir, logger);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            computeBean = EJBFactory.getRemoteComputeBean();
            tgBlastTask = (TeragridSimpleBlastTask) ProcessDataHelper.getTask(processData);
            if (!setupAndCheckParentTask()) {
                throw new Exception("Parent task has error status");
            }
            logger.info("Setting up resultNode");
            setupResultNode();
            logger.info("Getting input query file");
            getInputQueryFile();
            logger.info("Splitting input query file");
            splitInputQueryFile();
            logger.info("Split into " + splitInputFileList.size() + " files");
            logger.info("Initializing MPI params from task");
            initMpiParamsFromTask();
            logger.info("Creating TG work directory");
            tgWorkDirPath = TG_WORK_DIR + "/" + SERVICE_DIR_NAME + "/" + tgBlastTask.getObjectId();
            TeraGridHelper.runTgCmd("mkdir " + tgWorkDirPath, logger, 5);
            while (isDoneFileSet.size() < splitInputFileList.size()) {
                runAndWaitForAllJobs();
            }
            logger.info("Done waiting for all jobs - starting unifyCollections");
            unifyCollections();
            logger.info("Done collection unification - starting formatBlastOutputsFromCollection");
            formatBlastOutputsFromCollection();
            logger.info("Done with formatBlastOutputsFromCollection");
            sc.cleanup();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exception in TeragridSimpleBlastService: " + ex.getMessage());
            if (parentTask != null) {
                setParentTaskToError();
            }
            setTaskToError();
            throw new ServiceException(ex);
        }
    }

    protected void runAndWaitForAllJobs() throws Exception {
        globalRunAndWaitCount++;
        // Clean lists
//        tgJobIdentifierList.clear();
//        mpiBlastCmdFileList.clear();
//        mpiBlastCmdCaptureFileList.clear();
        int index = 1;
        int startIndex = 1;
        int qsubIndex = 0;
        logger.info("Starting runAndWaitForAllJobs - iteration=" + globalRunAndWaitCount + " files already processed=" + isDoneFileSet.size() + " of total=" + splitInputFileList.size());
        for (File f : splitInputFileList) {
            int properQsubIndex = (index / MPI_RUNS_PER_QSUB) + 1;
            if (properQsubIndex != qsubIndex) {
                if (qsubIndex != 0) {
                    launchQsub(qsubIndex, startIndex, index - 1);
                }
                startIndex = index;
                qsubIndex = properQsubIndex;
            }
            // We only need to copy the query files once
            if (globalRunAndWaitCount == 1) {
                logger.info("Processing input file index=" + index);
                logger.info("Copying file to teragrid=" + f.getAbsolutePath() + " as index=" + index + " for qsubIndex=" + qsubIndex);
                TeraGridHelper.copyFileToTg(f, tgWorkDirPath + "/" + getTgQueryFilename(index), logger, 100);
            }
            else {
                logger.info("Skipping copy of query index=" + index + " since this is runAndWaitForAllJobs iteration=" + globalRunAndWaitCount);
            }
            index++;
        }
        // Finish command for remaining jobs
        if (qsubIndex > 0 && ((index - 1) >= startIndex)) {
            launchQsub(qsubIndex, startIndex, index - 1);
        }
        // If this value is not already initialized, then do so
        if (maxQsubIndex == 0) {
            maxQsubIndex = qsubIndex;
        }
        // Restart counter to observe jobs
        qsubIndex = 1;
        logger.info("Starting job wait loop");
        while (qsubIndex <= maxQsubIndex) {
            logger.info("Waiting for qsubIndex=" + qsubIndex);
            waitForJob(qsubIndex);
            int startIndex2 = qsubStartMap.get(qsubIndex);
            int endIndex2 = qsubEndMap.get(qsubIndex);
            for (int j = startIndex2; j <= endIndex2; j++) {
                File inputFile = splitInputFileList.get(j - 1);
                if (!isDoneFileSet.contains(inputFile)) {
                    String tgDoneFilePath = tgWorkDirPath + "/" + getTgDoneFilename(j);
                    if (TeraGridHelper.tgFileExists(tgDoneFilePath, logger, 100)) {
                        File resultFile = new File(resultDir, getTgResultFilename(j));
                        logger.info("Index=" + j + " is done - starting to copy this file from teragrid=" + resultFile.getAbsolutePath());
                        TeraGridHelper.copyFileFromTg(tgWorkDirPath + "/" + getTgResultFilename(j), resultFile, logger, 100);
                        logger.info("Done with copy");
                        if (TG_REMOVE_RESULT) {
                            Thread.sleep(1000);
                            logger.info("Cleaning result file from teragrid for index=" + j);
                            TeraGridHelper.runTgCmd("rm " + tgWorkDirPath + "/" + getTgResultFilename(j), logger, 3);
                        }
                        logger.info("Remapping labels from mpi result for index=" + j);
                        remapXmlResult(j);
                        logger.info("Converting mpi to ncbi xml format for index=" + j);
                        convertMpiToNcbiXml(j);
                        logger.info("Generating oos collection for index=" + j);
                        generateCollectionFromXml(j);
                        logger.info("Done generating collection for index=" + j);
                        logger.info("Successfully finished results for inputFile=" + inputFile.getAbsolutePath());
                        isDoneFileSet.add(inputFile);
                    }
                    else {
                        logger.info("Could not confirm results of run of index=" + j + " inputFile=" + inputFile.getAbsolutePath() + " therefore scheduling re-do");
                    }
                }
            }
            qsubIndex++;
        }
    }

    // This method returns zero if all files are already processed and no qsub job is submitted,
    // otherwise it returns the number of files within the job.
    protected int launchQsub(int qsubIndex, int startIndex, int endIndex) throws Exception {
        // We need to launch the previous file
        logger.info("Creating mpi blast command for qsubIndex=" + qsubIndex);
        qsubStartMap.put(qsubIndex, startIndex);
        qsubEndMap.put(qsubIndex, endIndex);
        File mpiBlastCmdFile = new File(resultDir, "mpiBlastCmd_" + resultNode.getObjectId() + "_" + qsubIndex + ".qsub");
        int runsInCmd = createMpiBlastCmd(mpiBlastCmdFile, qsubIndex, startIndex, endIndex);
        if (runsInCmd > 0) {
            File mpiBlastCmdCaptureFile = new File(resultDir, mpiBlastCmdFile.getName() + ".output");
            logger.info("Copying mpi blast cmd file to teragrid=" + mpiBlastCmdFile.getAbsolutePath());
            TeraGridHelper.copyFileToTg(mpiBlastCmdFile, tgWorkDirPath + "/" + mpiBlastCmdFile.getName(), logger, 100);
            int numTgQueuedJobs;
            do {
                numTgQueuedJobs = checkNumTgQueuedJobs();
                if (numTgQueuedJobs >= maxTgQueuedJobs) {
                    logger.info("Number of queued teragrid jobs is " + numTgQueuedJobs + ". Waiting for this to drop below " + maxTgQueuedJobs);
                    Thread.sleep(60000); // one-minute
                }
            }
            while (numTgQueuedJobs >= maxTgQueuedJobs);
            logger.info("Running qsub cmd at teragrid for mpiCaptureFile=" + mpiBlastCmdCaptureFile.getAbsolutePath());
            TeraGridHelper.runTgCmd(MPI_QSUB_CMD + " " + tgWorkDirPath + "/" + mpiBlastCmdFile.getName(), mpiBlastCmdCaptureFile, logger, 5);
            logger.info("Recovering jobId from teragrid");
            String jobId = recoverJobIdentifier(mpiBlastCmdCaptureFile);
            logger.info("For qsubIndex=" + qsubIndex + " assigned jobId=" + jobId);

//            tgJobIdentifierList.add(jobId);
//            mpiBlastCmdFileList.add(mpiBlastCmdFile);
//            mpiBlastCmdCaptureFileList.add(mpiBlastCmdCaptureFile);

            tgJobIdentifierMap.put(qsubIndex, jobId);
            mpiBlastCmdFileMap.put(qsubIndex, mpiBlastCmdFile);
            mpiBlastCmdCaptureFileMap.put(qsubIndex, mpiBlastCmdCaptureFile);
        }
        return runsInCmd;
    }

    protected int checkNumTgQueuedJobs() throws Exception {
        File tgNumQueuedFile = new File(resultDir, "teragridQstat.txt");
        if (tgNumQueuedFile.exists()) {
            boolean deleteSuccessful = tgNumQueuedFile.delete();
            if (!deleteSuccessful) {
                logger.error("Unable to delete the teragrid number queued file.  Continuing...");
            }
        }
        TeraGridHelper.runTgCmd("qstat", tgNumQueuedFile, logger, 100);
        FileReader fr = new FileReader(tgNumQueuedFile);
        BufferedReader br = new BufferedReader(fr);
        int lineCount = 0;
        while (br.readLine() != null) {
            lineCount++;
        }
        br.close();
        return lineCount;
    }

    private void remapXmlResult(int index) throws Exception {
// Example:
//         --add_deflines -i Xremapped_panda_20091112/smurphy_test_4.output
//                        -m Xremapped_panda_20091112/panda_20091112_remap.db
//                        -o Xremapped_panda_20091112/smurphy_test_4_wdefs.output
        String resultFilepath = new File(resultDir, getTgResultFilename(index)).getAbsolutePath();
        String overlapCmd =
                MetaGenoPerlConfig.getPerlEnvPrefix() +
                        MetaGenoPerlConfig.PERL_EXEC + " -pi -e 's/\\&quot;/\\\"/g' " + resultFilepath + "\n" +
                        MetaGenoPerlConfig.PERL_EXEC + " " + MetaGenoPerlConfig.PERL_BIN_DIR + "/" + TG_REMAP_SCRIPT + " --add_deflines" +
                        " -i " + resultFilepath +
                        " -m " + param_SqliteMapPath +
                        " -o " + resultFilepath + ".remapped_deflines" + "\n" +
                        "rm " + resultFilepath + "\n" +
                        MetaGenoPerlConfig.PERL_EXEC + " -pi -e 's/\\&/ /g; s/ < / /g; s/ > / /g;' " +
                        resultFilepath + ".remapped_deflines\n" +
                        "mv " + resultFilepath + ".remapped_deflines " + resultFilepath + ".tg" + "\n";
        int ev = sc.execute(overlapCmd, false);
        if (ev != 0) {
            throw new Exception("SystemCall produced non-zero exit value=" + overlapCmd);
        }
    }

    private void convertMpiToNcbiXml(int index) throws Exception {
        String resultFilepath = new File(resultDir, getTgResultFilename(index)).getAbsolutePath();
        String xmlCmd = MetaGenoPerlConfig.getCmdPrefix() + MPI_XML_CMD +
                " -i " + resultFilepath + ".tg " +
                " -o " + resultFilepath + ".ncbi\n";
        int ev = sc.execute(xmlCmd, false);
        if (ev != 0) {
            throw new Exception("SystemCall produced non-zero exit value=" + xmlCmd);
        }
    }

    private void waitForJob(int qsubIndex) throws Exception {
        boolean jobRunning = true;
        File statusFile = new File(resultDir, mpiBlastCmdFileMap.get(qsubIndex).getName() + ".status");
        while (jobRunning) {
            TeraGridHelper.runTgCmd("qstat", statusFile, logger, 100);
            Thread.sleep(1000);
            String jobLine = qstatContainsJobId(statusFile, tgJobIdentifierMap.get(qsubIndex));
            if (jobLine != null) {
                // Job is not done - continue waiting
                jobRunning = true;
                logger.info("Teragrid job " + resultNode.getObjectId() + " identifier " + tgJobIdentifierMap.get(qsubIndex) +
                        " is continuing: \"" + jobLine.trim() + "\"");
            }
            else {
                // Job is done - return
                jobRunning = false;
                logger.info("Teragrid job " + resultNode.getObjectId() + " identifier " + tgJobIdentifierMap.get(qsubIndex) + " is done");
                return;
            }
            Thread.sleep(TG_STAT_INTERVAL);
        }
    }

    private String qstatContainsJobId(File statusFile, String jobIdentifier) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(statusFile));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                if (tokens.length > 0) {
                    if (tokens[0].trim().equals(jobIdentifier)) {
                        return line;
                    }
                }
            }
        }
        finally {
            br.close();
        }
        return null;
    }

    private String recoverJobIdentifier(File mpiBlastCmdCaptureFile) throws Exception {
        Long jobId = null;
        BufferedReader br = new BufferedReader(new FileReader(mpiBlastCmdCaptureFile));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                String tr = line.trim();
                if (tr.startsWith("Your job") && tr.endsWith("has been submitted")) {
                    String[] tokens = line.split("\\s+");
                    jobId = new Long(tokens[2]);
                }
            }
        }
        finally {
            br.close();
        }
        if (jobId == null)
            throw new Exception("Could not parse file for jobId=" + mpiBlastCmdCaptureFile.getAbsolutePath());
        return jobId.toString();
    }

    private int createMpiBlastCmd(File mpiBlastCmdFile, int qsubIndex, int startIndex, int endIndex) throws Exception {
        Set<Integer> runSet = new HashSet<Integer>();
        for (int i = startIndex; i <= endIndex; i++) {
            File inputFile = splitInputFileList.get(i - 1);
            if (!isDoneFileSet.contains(inputFile)) {
                runSet.add(i);
            }
        }
        if (runSet.size() > 0) {
            FileWriter fw = new FileWriter(mpiBlastCmdFile);
            fw.write("#!/bin/bash\n");
            fw.write("\n");
            fw.write("#$ -A " + param_TeragridGrantNumber + "\n");
            fw.write("#$ -V\n");
            fw.write("#$ -cwd\n");
            fw.write("#$ -N blast_" + resultNode.getObjectId() + "_" + qsubIndex + "\n");
            fw.write("##$ -j y\n");
            fw.write("#$ -e " + tgWorkDirPath + "/" + "$JOB_NAME.$JOB_ID.err\n");
            fw.write("#$ -o " + tgWorkDirPath + "/" + "$JOB_NAME.$JOB_ID.out\n");
            fw.write("#$ " + TG_RESOURCE + "\n");
            fw.write("#$ -q " + TG_QUEUE + "\n");
            fw.write("#$ -l h_rt=" + TG_RUNTIME + "\n");
            fw.write("#$ -M " + TG_EMAIL + "\n");
            fw.write("#$ -m be\n");
            fw.write("\n");
            fw.write("date\n");
            fw.write("hostname\n");
            fw.write("module list\n");
            fw.write("\n");

            for (int i = startIndex; i <= endIndex; i++) {
                if (runSet.contains(i)) {
                    String resultFilePath = tgWorkDirPath + "/" + getTgResultFilename(i);
                    String doneFilePath = tgWorkDirPath + "/" + getTgDoneFilename(i);
                    fw.write("ibrun mpiblast -p " + param_MpiBlastProgram + " -d " + param_TeragridBlastDbName + " -i " + tgWorkDirPath + "/" + getTgQueryFilename(i) +
                            " -o " + resultFilePath + " --removedb " + param_MpiBlastParams + " -z " + param_TeragridBlastDbSize + " -m 7 --output-search-stats\n");
                    fw.write("touch " + doneFilePath + "\n");
                }
            }
            fw.close();
        }
        return runSet.size();
    }

    private String getTgQueryFilename(int index) {
        return "query_" + resultNode.getObjectId() + "_" + index + ".fasta";
    }

    private String getTgResultFilename(int index) {
        return "blast_" + resultNode.getObjectId() + "_" + index + ".xml";
    }

    private String getTgDoneFilename(int index) {
        return getTgResultFilename(index) + ".done";
    }

    private void initMpiParamsFromTask() throws Exception {
        param_TeragridGrantNumber = nullExceptionCheck(tgBlastTask.getParameter(TeragridSimpleBlastTask.PARAM_teragrid_grant_number));
        param_MpiBlastProgram = nullExceptionCheck(tgBlastTask.getParameter(TeragridSimpleBlastTask.PARAM_mpi_blast_program));
        param_TeragridBlastDbName = nullExceptionCheck(tgBlastTask.getParameter(TeragridSimpleBlastTask.PARAM_tg_db_name));
        param_TeragridBlastDbSize = nullExceptionCheck(tgBlastTask.getParameter(TeragridSimpleBlastTask.PARAM_tg_db_size));
        param_SqliteMapPath = nullExceptionCheck(tgBlastTask.getParameter(TeragridSimpleBlastTask.PARAM_path_to_sqlite_map_db));
        param_MpiBlastParams = nullExceptionCheck(tgBlastTask.getParameter(TeragridSimpleBlastTask.PARAM_mpi_blast_parameters));
    }

    private String nullExceptionCheck(String s) throws Exception {
        if (s == null) {
            throw new Exception("String is unexpectedly null");
        }
        return s;
    }

    private boolean setupAndCheckParentTask() throws Exception {
        if (tgBlastTask.getParentTaskId() != null && tgBlastTask.getParentTaskId() > 0) {
            parentTask = computeBean.getTaskById(tgBlastTask.getParentTaskId());
            String[] status = computeBean.getTaskStatus(parentTask.getObjectId());
            if (status[ComputeBaseDAO.STATUS_TYPE].equals(Event.ERROR_EVENT)) {
                logger.info("Parent task=" + parentTask.getObjectId() + " has error status - returning");
                return false;
            }
        }
        return true;
    }

    private void setParentTaskToError() {
        try {
            String[] status = computeBean.getTaskStatus(parentTask.getObjectId());
            if (!status[ComputeBaseDAO.STATUS_TYPE].equals(Event.ERROR_EVENT)) {
                logger.info("Adding error even to parent task=" + parentTask.getObjectId());
                parentTask.addEvent(new Event("Error in child task id=" + tgBlastTask.getObjectId(), new Date(), Event.ERROR_EVENT));
                computeBean.saveOrUpdateTask(parentTask);
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    private void setTaskToError() {
        try {
            String[] status = computeBean.getTaskStatus(tgBlastTask.getObjectId());
            if (!status[ComputeBaseDAO.STATUS_TYPE].equals(Event.ERROR_EVENT)) {
                logger.info("Adding error even to task=" + tgBlastTask.getObjectId());
                parentTask.addEvent(new Event("Error in task id=" + tgBlastTask.getObjectId(), new Date(), Event.ERROR_EVENT));
                computeBean.saveOrUpdateTask(tgBlastTask);
            }
        }
        catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    private void setupResultNode() throws Exception {
        BlastResultFileNode resultNode = (BlastResultFileNode) ProcessDataHelper.getResultFileNode(processData);
        if (resultNode == null) {
            throw new Exception("BlastResultFileNode is unexpectedly null");
        }
        resultDir = new File(resultNode.getDirectoryPath());
        FileUtil.ensureDirExists(resultDir.getAbsolutePath());
        this.resultNode = resultNode;
    }

    private void getInputQueryFile() throws Exception {
        Long queryNodeId = tgBlastTask.getQueryId();
        FastaFileNode queryNode = (FastaFileNode) computeBean.getNodeById(queryNodeId);
        inputQueryFile = new File(queryNode.getFastaFilePath());
        if (!inputQueryFile.exists()) {
            throw new Exception("Could not find expected query file=" + inputQueryFile.getAbsolutePath());
        }
    }

    private void generateCollectionFromXml(int index) throws Exception {
        String tempBlastOutputFileName = new File(resultDir, getTgResultFilename(index)).getAbsolutePath() + ".ncbi";
        File collectionCmdFile = new File(resultDir, "collectionCmd_" + index + ".sh");
        FileWriter fw = new FileWriter(collectionCmdFile);
        // MPI-style DOCTYPE line:
        // <!DOCTYPE BlastOutput PUBLIC "-//NCBI//NCBI BlastOutput/EN" "NCBI_BlastOutput.dtd">
        fw.write("sed -i \"/\\\"NCBI_BlastOutput\\.dtd\\\"/d\" " + tempBlastOutputFileName + "\n");
        fw.write(JAVA_PATH + " -Xmx" + JAVA_MAX_MEMORY + "m -classpath " + GRID_JAR_PATH + " " + MERGE_SORT_PROCESSOR + " -o " + tempBlastOutputFileName + "\n");
        File blastCollectionFile = new File(tempBlastOutputFileName + BlastResultFileNode.DEFAULT_SERIALIZED_PBRC_FILE_EXTENSION);
        if (blastCollectionFileMap.size() != index - 1) {
            throw new Exception("Generating collectionFromXml with index=" + index + " but file collection is out of order with " +
                    blastCollectionFileMap.size() + " entries");
        }
        blastCollectionFileMap.put(index, blastCollectionFile);
        fw.close();
        String executePermissionCmd = "chmod +x " + collectionCmdFile.getAbsolutePath();
        int ev = sc.execute(executePermissionCmd, false);
        if (ev != 0) {
            throw new Exception("SystemCall produced non-zero exit value=" + executePermissionCmd);
        }
        SimpleGridJobRunner job = new SimpleGridJobRunner(new File(resultNode.getDirectoryPath()),
                collectionCmdFile.getAbsolutePath(), JCVI_QUEUE, tgBlastTask.getParameter("project"), tgBlastTask.getObjectId());
        if (!job.execute()) {
            throw new Exception("Grid job failed with cmd=" + collectionCmdFile.getAbsolutePath());
        }
    }

    private void unifyCollections() throws Exception {
        File unifyCmdFile = new File(resultDir, "unifyCmd.sh");
        FileWriter fw = new FileWriter(unifyCmdFile);
//        System.out.println("usage: java " + classname +
//              -t <blast results dir>
//              -f <first partition processed>
//              -l <last partition processed>
//              -n <number of top hits saved - ranked by e-Value>\n");
        fw.write(JAVA_PATH + " -Xmx" + JAVA_MAX_MEMORY + "m -classpath " + GRID_JAR_PATH + " " + MERGE_SORT_PROCESSOR +
                " -t " + resultDir.getAbsolutePath() + "\n" +
                " -f 1\n" +
                " -l " + splitInputFileList.size() + "\n" +
                " -n " + MPI_BLAST_UNIFY_COLLECTION_HITS_TO_KEEP + "\n");
        fw.close();
        String executePermissionCmd = "chmod +x " + unifyCmdFile.getAbsolutePath();
        int ev = sc.execute(executePermissionCmd, false);
        if (ev != 0) {
            throw new Exception("SystemCall produced non-zero exit value=" + executePermissionCmd);
        }
        SimpleGridJobRunner job = new SimpleGridJobRunner(new File(resultNode.getDirectoryPath()),
                unifyCmdFile.getAbsolutePath(), JCVI_QUEUE, tgBlastTask.getParameter("project"), tgBlastTask.getObjectId());
        if (!job.execute()) {
            throw new Exception("Task failed with cmd=" + unifyCmdFile.getAbsolutePath());
        }
    }

    private void formatBlastOutputsFromCollection() throws Exception {
        int index;
        long qNum = 0;
        for (index = 1; index <= splitInputFileList.size(); index++) {
            String resultFilepath = new File(resultDir, getTgResultFilename(index)).getAbsolutePath() + ".ncbi";
            File hitsFile = new File(resultDir, BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_BASENAME +
                    "_" + index + ".queryCountWithHits");
            String hitCountCmd = "cat " + resultFilepath + " | grep Iteration_iter-num | wc -l > " + hitsFile.getAbsolutePath();
            sc.execute(hitCountCmd, false);
            if (!hitsFile.exists()) {
                throw new Exception("Could not locate hits file=" + hitsFile.getAbsolutePath());
            }
            BufferedReader br = new BufferedReader(new FileReader(hitsFile));
            String hitsLine;
            try {
                hitsLine = br.readLine();
            }
            finally {
                br.close();
            }
            qNum += (new Long(hitsLine.trim()));
        }
        File renamedOosFile = new File(resultDir, BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_BASENAME + ".oos");
        if (!renamedOosFile.exists()) {
            throw new Exception("formatBlastOutputsFromCollection could not locate expected oos file=" + renamedOosFile.getAbsolutePath());
        }
        File blastTaskOosFile = new File(resultDir, BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_BASENAME + ".blastTask");
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(blastTaskOosFile));
        BlastTask cleanTask = cleanTgBlastTask(); // this removes hibernate classes
        oos.writeObject(cleanTask);
        oos.close();
        File formatCmdFile = new File(resultDir, "formatCmd.sh");
        FileWriter fw = new FileWriter(formatCmdFile);
        String blastFormatCmd =
                JAVA_PATH + " -Xmx" + JAVA_MAX_MEMORY + "m -classpath " + GRID_JAR_PATH + " " + GRID_PERSISTXML_PROCESSOR + " " +
                        BlastResultCollectionConverter.KEY_BLAST_OUTPUT_DIR + "=" + resultDir.getAbsolutePath() + " " +
                        BlastResultCollectionConverter.KEY_START_INTERATION + "=1 " +
                        BlastResultCollectionConverter.KEY_EXPECTED_INTERATIONS + "=" + qNum + " " +
                        BlastResultCollectionConverter.KEY_IS_FIRST + "=yes " +
                        BlastResultCollectionConverter.KEY_IS_LAST + "=yes " +
                        BlastResultCollectionConverter.KEY_OUTPUT_FORMATS + "=" + MANDATORY_BLAST_FORMAT_TYPES + "\n";
        fw.write(blastFormatCmd);
        fw.close();
        String executePermissionCmd = "chmod +x " + formatCmdFile.getAbsolutePath();
        int ev = sc.execute(executePermissionCmd, false);
        if (ev != 0) {
            throw new Exception("SystemCall produced non-zero exit value=" + executePermissionCmd);
        }
        SimpleGridJobRunner job = new SimpleGridJobRunner(new File(resultNode.getDirectoryPath()),
                formatCmdFile.getAbsolutePath(), JCVI_QUEUE, tgBlastTask.getParameter("project"), tgBlastTask.getObjectId());
        if (!job.execute()) {
            throw new Exception("Grid job failed with cmd=" + formatCmdFile.getAbsolutePath());
        }
    }

    protected BlastTask cleanTgBlastTask() {
        BlastTask bt = new TeragridSimpleBlastTask();
        Set<String> keySet = tgBlastTask.getParameterKeySet();
        for (String key : keySet) {
            bt.setParameter(key, tgBlastTask.getParameter(key));
        }
        return bt;
    }

    protected void splitInputQueryFile() throws Exception {
        ProcessData tmpForSplitProcessData = new ProcessData();
        tmpForSplitProcessData.setProcessId(processData.getProcessId());
        SimpleMultiFastaSplitterService splitter = new SimpleMultiFastaSplitterService();
        splitter.splitFastaFile(tmpForSplitProcessData, inputQueryFile, new Integer(MPI_BLAST_DEFAULT_QUERIES_PER_EXEC.trim()));
        splitInputFileList = (List<File>) tmpForSplitProcessData.getItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST);
    }

}
