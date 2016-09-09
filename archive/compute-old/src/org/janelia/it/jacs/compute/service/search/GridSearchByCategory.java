
package src.org.janelia.it.jacs.compute.service.search;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.Session;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.SearchBeanLocal;
import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.JobStatusLogger;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SimpleJobStatusLogger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.status.GridJobStatus;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import src.org.janelia.it.jacs.shared.lucene.LuceneDataFactory;
import src.org.janelia.it.jacs.shared.lucene.SearchLuceneCmd;
import src.org.janelia.it.jacs.shared.lucene.searchers.LuceneSearcher;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 29, 2007
 * Time: 4:08:07 PM
 * From jacs.properties
 * # Text Search Properties
 ts.searchProjectCode=08020
 ts.indexRootPath=/nrs/jacs/jacsData/filestore/luceneIdx/
 ts.resultsRootPath=/nrs/jacs/jacsData/filestore/system/tmpSearchResults/
 ts.maxInsertBatchSize=10000
 ts.maxSearchResultSet=60000
 ts.GridSearchMinimumMemoryMB=384
 ts.GridSearchMaximumMemoryMB=1500
 ts.GridQueue=-l fast
 #grid pool period (msecs) and timeout (sec)
 ts.GridPoolTime=1000
 ts.WaitForGridTimeout=600

 # Only admin user can run copy function in postgres, for index generation and search results
 ts.jdbc.username=postgres
 ts.jdbc.password=postgres
 */
public class GridSearchByCategory implements IService {

    private Logger _logger;
    public static final String JAVA_PATH = SystemConfigurationProperties.getString("Java.Path");
    public static final String SEARCH_PROJECT_CODE = SystemConfigurationProperties.getString("ts.searchProjectCode");
    public static final String JAVA_MAX_MEMORY = SystemConfigurationProperties.getString("ts.GridSearchMaximumMemoryMB");
    public static final String JAVA_MIN_MEMORY = SystemConfigurationProperties.getString("ts.GridSearchMinimumMemoryMB");
    public static final String GRID_JAR_PATH = SystemConfigurationProperties.getFilePath("Grid.Lib.Path", "Grid.Jar.Name");
    private static final int MAX_SEARCH_RESULT_SET = SystemConfigurationProperties.getInt("ts.maxSearchResultSet");
    private static final String LUCENE_IDX_ROOT_PATH = SystemConfigurationProperties.getString("ts.indexRootPath");
    private static final String LUCENE_RESULTS_ROOT_PATH = SystemConfigurationProperties.getString("ts.resultsRootPath");
    private static final String TEXTSEARCH_GRID_QUEUE = SystemConfigurationProperties.getString("ts.GridQueue");
    private static final int TEXTSEARCH_GRID_POOLTIME = SystemConfigurationProperties.getInt("ts.GridPoolTime");
    private static final int SEARCH_TIMEOUT = SystemConfigurationProperties.getInt("ts.WaitForGridTimeout");
    private static final String GRID_TEMP_DIR = SystemConfigurationProperties.getString("SystemCall.ScratchDir");


    private SearchTask searchTask;
    private Long searchResultNodeId;

    public GridSearchByCategory() {
    }

    private void init(IProcessData processData) throws DaoException, MissingDataException {
        SearchBeanLocal searchBean = EJBFactory.getLocalSearchBean();
        searchTask = (SearchTask) searchBean.getTaskById(processData.getProcessId());

        SearchResultNode searchResultNode = searchBean.getSearchTaskResultNode(searchTask.getObjectId());
        if (searchResultNode == null) {
            throw new MissingDataException("Search task has no result node: " +
                    searchTask.getObjectId());
        }
        searchResultNodeId = searchResultNode.getObjectId();

//        ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
//        Node node = computeBean.getResultNodeByTaskId( searchTask.getObjectId());
//        searchResultNodeId = node.getObjectId();
    }

    private ArrayList<String> getTopics(IProcessData processData) throws MissingDataException {
        return (ArrayList<String>) processData.getMandatoryItem("SEARCH_GRID_CATEGORIES");
    }

    public void execute(IProcessData processData) {
        Map<String, String> jobsMap = new HashMap<String, String>();
        try {
            _logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            init(processData);
            ArrayList<String> topics = getTopics(processData);

            if (topics == null || topics.size() == 0) {
                // no need to do anything
                return;
            }

            DrmaaHelper drmaa = new DrmaaHelper(_logger);
            SerializableJobTemplate jt = drmaa.createJobTemplate(new SerializableJobTemplate());
            Map<String, Integer> jobStatuses = new HashMap<String, Integer>();
            for (String topic : topics) {
                // command line lucene execution parameters:
                setUpGridJob(jt, topic);
//                _logger.debug("JobTemplate = " + jt.toString());
                String jobID = drmaa.runJob(jt);
                GridJobStatus s = new GridJobStatus(searchTask.getObjectId(), jobID, TEXTSEARCH_GRID_QUEUE, GridJobStatus.JobState.QUEUED);
                EJBFactory.getLocalJobControlBean().saveGridJobStatus(s);
                jobStatuses.put(jobID, 0);
                jobsMap.put(jobID, topic);
                // category, indexRootPath, outputFileName, search terms
                _logger.debug(topic + ": search submited to the grid");
            }
            JobStatusLogger jsl = new SimpleJobStatusLogger(searchTask.getObjectId());
            drmaa.deleteJobTemplate(jt); // don't need it anymore
            // now wait for jobs for a maximum of 10 min
            Date stopTime = new Date();
            stopTime.setTime(stopTime.getTime() + SEARCH_TIMEOUT * 1000);
            while (jobsMap.size() > 0) {
                if (stopTime.compareTo(new Date()) < 0) {
                    // try to cancel jobs
                    for (String failedJob : jobsMap.keySet()) {
                        drmaa.control(failedJob, Session.TERMINATE);
                    }
                    throw new Exception("WaitForGridTimeout exceded. Giving up now...");
                }
                Thread.sleep(TEXTSEARCH_GRID_POOLTIME);
                // have to make a copy here to avoid ConcurrentModificationException
                String[] runningJobs = setToArraty(jobsMap.keySet());
                for (String jobId : runningJobs) {

                    // check if it is done
                    int jobStatus = drmaa.getJobProgramStatus(jobId);
                    GridJobStatus.JobState newState = DrmaaHelper.translateStatusCode(jobStatus);
                    if (jobStatuses.get(jobId) != jobStatus) {
                        jobStatuses.put(jobId, jobStatus);
                        if (jobStatus != Session.DONE && jobStatus != Session.FAILED)
                            jsl.updateJobStatus(jobId, newState);
                    }
                    if (jobStatus == Session.DONE || jobStatus == Session.FAILED) {
                        JobInfo jobInfo = drmaa.wait(jobId, Session.TIMEOUT_NO_WAIT);
                        jsl.updateJobInfo(jobId, newState, jobInfo.getResourceUsage());
                        processCompletedJob(jobInfo, jobsMap.get(jobId));
                        jobsMap.remove(jobId);
                    }
                }
            }
        }
        catch (Exception e) {
            _logger.error(e);
            e.printStackTrace();
            // record error for all non-processed jobs
            try {
                for (String failedTopic : jobsMap.values()) {
                    addSubprocessEvent(Event.SUBTASKERROR_EVENT, failedTopic);
                    cleanUp(failedTopic);
                }
            }
            catch (DaoException e2) {
                _logger.error("THIS IS REALLY BAD. -- Unable to record errors to event table", e2);
            }
        }
    }

    private String[] setToArraty(Set<String> items) {
        String[] arr = new String[items.size()];
        Iterator iter = items.iterator();
        for (int i = 0; i < items.size(); i++)
            arr[i] = (String) iter.next();

        return arr;
    }

    private void processCompletedJob(JobInfo jobInfo, String category) throws Exception {
        if (jobInfo.getExitStatus() == 0) // success
        {
            // see if there are any results returned
            String resultFileName = makeResultFileName(category);
            File rf = new File(resultFileName);
            if (rf.exists() && rf.canRead() && rf.length() > 0) {
                // try to get a number of records found
                if (_logger.isDebugEnabled()) {
                    Scanner scanner = new Scanner(new File(makeOutFileName(category)));
                    if (scanner.hasNextLine()) {
                        String line1 = scanner.nextLine();
                        _logger.debug(category + ": grid search found " + line1 + " hits");
                    }
                    else {
                        _logger.debug(category + ": grid search found UNKNOWN number of hits");
                    }
                }
                // load results into DB
                SearchQuerySessionContainer container = new SearchQuerySessionContainer(SearchQuerySessionContainer.SEARCH_ENGINE_LUCENE, null);
                LuceneSearcher searcher = LuceneDataFactory.getDocumentSearcher(category);
                container.copyResultsToDB(searcher, resultFileName, true);
            }
            else {
                _logger.debug(category + " search returned no results!");
            }
            // set status
            addSubprocessEvent(Event.SUBTASKCOMPLETED_EVENT, category);
            if (_logger.isDebugEnabled()) {
                _logger.debug(category + ": search completed; " +
                        EJBFactory.getLocalComputeBean().getNumCategoryResults(searchResultNodeId, category) +
                        " results are placed into the DB");
            }
        }
        else {
            StringBuffer jobstats = new StringBuffer();
            Map<String, String> infoMap = jobInfo.getResourceUsage();
            for (String key : infoMap.keySet()) {
                jobstats.append("\t").append(key).append(": ").append(infoMap.get(key)).append("\n");
            }
            _logger.error("DRMAA job " + jobInfo.getJobId() + " failed. Job statistics are:\n" + jobstats);

            // log error here
            _logger.error("Text search on Grid returned error: '" + getError(category) + "'");
            addSubprocessEvent(Event.SUBTASKERROR_EVENT, category);

        }

        cleanUp(category);
    }

    private String getError(String category) {
        try {
            File errorFile = new File(makeErrorFileName(category));
            if (!errorFile.exists()) {
                _logger.error("Could not find error file=" + errorFile.getAbsolutePath());
                return "Expected error file does not exist=" + errorFile.getAbsolutePath();
            }
            else {
                BufferedReader reader = new BufferedReader(new FileReader(errorFile));
                StringBuffer sb;
                try {
                    sb = new StringBuffer("");
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    _logger.error(sb.toString());
                }
                finally {
                    reader.close();
                }
                return makeErrorFileName(category) + " : " + sb.toString();
            }
        }
        catch (Exception e) {
            _logger.error(e.getMessage());
            return null;
        }

        // read error file into a string
//        StringBuffer sb = new StringBuffer();
//        Scanner scanner = new Scanner(makeErrorFileName(category));
//        while (scanner.hasNextLine())
//        {
//            sb.append(scanner.nextLine()).append("\n");
//        }
//        if (sb.length() > 0)
//        {
//            return sb.substring(0, sb.length() -1 ); // remove last end of line
//        }
//        else
//        {
//            return "Unknown error";
//        }

    }

    private void addSubprocessEvent(String eventType, String topic) throws DaoException {
        ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
        Object o = SearchSetup.getSynchObjectForTask(searchTask.getObjectId());
//        _logger.debug("For task " + task.getObjectId() + " synch object hash = " + System.identityHashCode(o));
        synchronized (o) {
        	try {
        		computeBean.saveEvent(searchTask.getObjectId(), eventType, topic, new Date());
        	}
        	catch (Exception e) {
        		throw new DaoException(e);
        	}
        }
    }

    protected String getGridServicePrefixName() {
        return "luceneSearch";
    }

    private void cleanUp(String category) {
        // delete extraneous files

        File tmpFile = new File(makeResultFileName(category));
        tmpFile.delete();
        tmpFile = new File(makeErrorFileName(category));
        tmpFile.delete();
        tmpFile = new File(makeOutFileName(category));
        tmpFile.delete();
        tmpFile = new File(makeScriptFileName(category));
        tmpFile.delete();

    }

    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws MissingDataException, IOException, DaoException, ParameterException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private String createJobCommandLine(String category, String tempFilePath) {

        StringBuffer command = new StringBuffer();
        command.append(JAVA_PATH).append(" -Xmx").append(JAVA_MAX_MEMORY).append("m -Xms").append(JAVA_MIN_MEMORY)
                .append("m -cp ").append(GRID_JAR_PATH)
                .append(" ").append(SearchLuceneCmd.class.getName())
                .append(" ").append(category)               // category
                .append(" ").append(MAX_SEARCH_RESULT_SET)  // max results
                .append(" ").append(searchResultNodeId)     // result node ID
                .append(" ").append(LUCENE_IDX_ROOT_PATH)   //lucene index root path
                .append(" ").append(tempFilePath)       // output file
                .append(" ").append(searchTask.getSearchString().replace('|', ' '));           // output file

        _logger.debug("Executing command \n" + command + "\n");
        return command.toString();
    }

    private String createExecutionScript(String category) throws Exception {
        String tempFilePath = FileUtil.ensureDirExists(GRID_TEMP_DIR + File.separator + searchResultNodeId) + File.separator + makeBaseFileName(category);
        String scriptFileName = makeScriptFileName(category);
        Writer out = new FileWriter(scriptFileName);

        StringBuffer script = new StringBuffer();
        script.append(createJobCommandLine(category, tempFilePath)).append("\n");
        script.append("retCode=$? \n");
        script.append("if [ $retCode == '0' ]; then \n");
        script.append("\ncp ").append(tempFilePath).append(" ").append(makeResultFileName(category)).append("\n");
        script.append("fi \n");
        script.append("\nrm -f ").append(tempFilePath).append("\n");
        script.append("exit  $retCode \n");
        out.write(script.toString());
        out.close();
        _logger.debug("Grid script: " + scriptFileName);
        return scriptFileName;
    }
//    protected void addSubprocessEvent(String eventType, Long processId, String topic, SearchBeanLocal searchBean)
//            throws DaoException {
//        searchBean.addSubprocessEvent(eventType, processId, topic);
//    }

    private SerializableJobTemplate setUpGridJob(SerializableJobTemplate jt, String topic) throws Exception {
        String outFile = makeOutFileName(topic);
        String errorFile = makeErrorFileName(topic);
//        String command = createJobCommandLine(topic);

        jt.setRemoteCommand("bash");
        jt.setArgs(Arrays.asList(createExecutionScript(topic)));
        jt.setWorkingDirectory(LUCENE_RESULTS_ROOT_PATH);
//        jt.setInputPath(":" + tmpDir.getAbsolutePath() + File.separator + getGridServicePrefixName() + "Configuration." + JobTemplate.PARAMETRIC_INDEX);
        jt.setErrorPath(":" + errorFile);
        jt.setOutputPath(":" + outFile);
        // Apply a RegEx to replace any non-alphanumeric character with "_".  SGE is finicky that way.
        jt.setJobName(searchTask.getOwner().replaceAll("\\W", "_") + "_" + getGridServicePrefixName());
        jt.setNativeSpecification(TEXTSEARCH_GRID_QUEUE);
        setProject(jt);
        return jt;
    }

    /**
     * This method is intended for adding native commands for sge to specify project
     * "-p <project>"
     *
     * @param jt SerializableJobTemplate
     * @throws org.ggf.drmaa.DrmaaException - Drmaa had an issue setting the specification
     */
    protected void setProject(SerializableJobTemplate jt) throws DrmaaException {
        _logger.info("setProject = -P " + SEARCH_PROJECT_CODE);
        jt.setNativeSpecification("-P " + SEARCH_PROJECT_CODE);
    }

    private String makeErrorFileName(String topic) {
        return makeBaseFilePath(topic) + ".err"; // output file
    }

    private String makeResultFileName(String topic) {
        return makeBaseFilePath(topic) + ".data"; // output file
    }

    private String makeOutFileName(String topic) {
        return makeBaseFilePath(topic) + ".out"; // output file
    }

    private String makeScriptFileName(String topic) {
        return makeBaseFilePath(topic) + ".sh"; // output file
    }

    private String makeBaseFilePath(String topic) {
        return LUCENE_RESULTS_ROOT_PATH + makeBaseFileName(topic);
    }

    private String makeBaseFileName(String topic) {
        return searchTask.getObjectId() + ("_") + topic;
    }

}