
package org.janelia.it.jacs.compute.service.metageno.anno;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeBaseDAO;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.metageno.MetaGenoPerlConfig;
import org.janelia.it.jacs.compute.service.metageno.SimpleGridJobRunner;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.tasks.hmmer.HmmpfamTask;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoAnnotationTask;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoPersistAnnoSqlTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.hmmer.HmmerPfamDatabaseNode;
import org.janelia.it.jacs.model.user_data.metageno.MetaGenoAnnotationResultNode;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * User: smurphy
 * Date: Sep 3, 2009
 * Time: 3:46:01 PM
 * From jacs.properties
 * MgPersist.PerlDir=/usr/local/projects/CAMERA/mg_prok_sqlite_perl_bin
 MgPersist.PersistAnnotationCmd=mg_sqlite_persist_anno_cmd.pl
 MgPersist.PersistOrfCmd=mg_sqlite_persist_orf_cmd.pl
 */
public class MetaGenoPersistAnnoSqlService implements IService {

    /* SCRIPT DEPENDENCIES

        persistAnnotationCmd=Xmg_prok_sqlite_perl_bin/mg_sqlite_persist_anno_cmd.pl
            use strict;
            require "getopts.pl";
            use Cwd 'realpath';
            use File::Basename;
            use HTTP::Status;
            use HTTP::Response;
            use LWP::UserAgent;
            use URI::URL;
            my $program = realpath($0);
            my $myLib = dirname($program);
            push @INC, $myLib;
            require 'db.pm';
            require 'dataset.pm';

        MODULE SUMMARY
            <contents of directory Xmg_prok_sqlite_perl_bin, which contains assorted files>

     */

    private Logger logger;
    protected static String mgPersistPerlDir = MetaGenoPerlConfig.PERL_BIN_DIR;
    protected static String mgPersistAnnoSqlCmd = SystemConfigurationProperties.getString("MgPersist.PersistAnnotationCmd");
    protected static String queue = SystemConfigurationProperties.getString("MgAnnotation.Queue");
    ComputeDAO computeDAO;
    MetaGenoAnnotationResultNode resultNode;
    MetaGenoAnnotationTask annoTask;
    boolean parentTaskErrorFlag = false;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            computeDAO = new ComputeDAO(logger);
            MetaGenoPersistAnnoSqlTask persistTask;
            Task t = ProcessDataHelper.getTask(processData);
            if (t == null || !(t instanceof MetaGenoPersistAnnoSqlTask)) {
                // Assume within annotation pipeline
                persistTask = handleAnnoPipelineContext(processData);
            }
            else {
                // Assume stand-alone functionality from web-service or similar interface
                persistTask = (MetaGenoPersistAnnoSqlTask) t;
            }
            String resultNodeIdString = persistTask.getParameter(MetaGenoPersistAnnoSqlTask.PARAM_input_anno_result_node_id).trim();
            logger.info("Using resultNodeIdString=" + resultNodeIdString);
            Long resultNodeId = new Long(resultNodeIdString);
            logger.info("Retrieving result node for nodeId=" + resultNodeId);
            resultNode = (MetaGenoAnnotationResultNode) computeDAO.getNodeById(resultNodeId);
            annoTask = (MetaGenoAnnotationTask) computeDAO.getTaskForNode(resultNode.getObjectId());
            //annoTask = (MetaGenoAnnotationTask) computeBean.getTaskForNodeId(resultNode.getObjectId());
            logger.info("Writing sqlInfoFile");
            writeSqlInfoFile();
            executePersistSqlScript();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exception in MetaGenoPersistAnnoSqlService: " + ex.getMessage());
            setParentTaskToErrorStatus(ex.getMessage(), processData);
            throw new ServiceException(ex.getMessage());
        }
    }

    private void writeSqlInfoFile() throws Exception {
        if (resultNode == null) {
            logger.error("writeSqlInfoFile: resultNode is null");
        }
        String resultNodePath = resultNode.getDirectoryPath();
        logger.info("resultNodePath=" + resultNodePath);
        File sqlInfoFile = new File(resultNode.getDirectoryPath(), "persistSqlInfoFile.properties");
        logger.info("Using sqlInfoFile path=" + sqlInfoFile.getAbsolutePath());
        FileWriter fw = new FileWriter(sqlInfoFile);
        addQueryPersistInfo(fw);
        addBlastPersistInfo(fw);
        addHmmPersistInfo(fw);
        addParentTaskInfo(fw);
        addBlastTaskInfo(fw);
        addHmmpfamTaskInfo(fw);
        fw.close();
    }

    private void executePersistSqlScript() throws Exception {
        String perlCmd = MetaGenoPerlConfig.getCmdPrefix() + mgPersistAnnoSqlCmd +
                " -d " + resultNode.getDirectoryPath();
        logger.info("Starting MetaGenoPersistAnnoSqlService, launching grid job queue=" + queue + " with cmd=" + perlCmd);
        SimpleGridJobRunner job = new SimpleGridJobRunner(new File(resultNode.getDirectoryPath()),
                perlCmd, queue, annoTask.getParameter("project"), annoTask.getObjectId());
        if (!job.execute()) {
            throw new Exception("Grid job failed with cmd=" + perlCmd);
        }
    }

    private void setParentTaskToErrorStatus(String message, IProcessData processData) {
        try {
            Task parentTask = ProcessDataHelper.getTask(processData);
            if (parentTask == null) {
                logger.error("Found null parent task - could not update to error status");
            }
            else {
                logger.info("MetaGenoPersistAnnoSqlService setParentTaskToErrorStatus() using parentTask=" + parentTask.getObjectId());
                ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
                computeBean.saveEvent(parentTask.getObjectId(), Event.ERROR_EVENT, message, new Date());
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addQueryPersistInfo(FileWriter fw) throws Exception {
        long inputNodeId = new Long(annoTask.getParameter(MetaGenoAnnotationTask.PARAM_input_node_id).trim());
        FastaFileNode inputNode = (FastaFileNode) computeDAO.getNodeById(inputNodeId);
        fw.write("QueryInputAnnotationFastaFileNodeId=" + inputNodeId + "\n");
        fw.write("QueryInputAnnotationFastaFilePath=" + inputNode.getFastaFilePath() + "\n");
        fw.write("QuerySequenceCount=" + inputNode.getSequenceCount() + "\n");
        fw.write("QueryDatasetName=" + inputNode.getName() + "\n");
        fw.write("QueryDatasetDescription=" + inputNode.getDescription() + "\n");
    }

    private void addBlastPersistInfo(FileWriter fw) throws Exception {
        List<Task> childTasks = computeDAO.getChildTasksByParentTaskId(annoTask.getObjectId());
        for (Task t : childTasks) {
            if (t instanceof BlastTask) {
                BlastTask blastTask = (BlastTask) t;
                String subjectDatabaseCsvString = blastTask.getParameter(BlastTask.PARAM_subjectDatabases);
                Long inputDatabaseNodeId = new Long(subjectDatabaseCsvString.trim());
                BlastDatabaseFileNode blastInputNode = (BlastDatabaseFileNode) computeDAO.getNodeById(inputDatabaseNodeId);
                fw.write("BlastPandaSubjectDatabaseId=" + blastInputNode.getObjectId() + "\n");
                fw.write("BlastPandaSubjectDatabaseName=" + blastInputNode.getName() + "\n");
                fw.write("BlastPandaSubjectDatabaseSourcePath=" + blastInputNode.getDirectoryPath() + "\n");
                fw.write("BlastPandaSubjectDatabaseDescription=" + blastInputNode.getDescription() + "\n");
                fw.write("BlastPandaSubjectDatabaseSequenceCount=" + blastInputNode.getSequenceCount() + "\n");
                return;
            }
        }
        throw new Exception("Could not locate blastTask for annotationTaskId=" + annoTask.getObjectId());
    }

    private void addHmmPersistInfo(FileWriter fw) throws Exception {
        List<Task> childTasks = computeDAO.getChildTasksByParentTaskId(annoTask.getObjectId());
        for (Task t : childTasks) {
            if (t instanceof HmmpfamTask) {
                HmmpfamTask hmmTask = (HmmpfamTask) t;
                String subjectDatabaseCsvString = hmmTask.getParameter(HmmpfamTask.PARAM_pfam_db_node_id);
                Long inputDatabaseNodeId = new Long(subjectDatabaseCsvString.trim());
                HmmerPfamDatabaseNode hmmInputNode = (HmmerPfamDatabaseNode) computeDAO.getNodeById(inputDatabaseNodeId);
                fw.write("HmmSubjectDatabaseId=" + hmmInputNode.getObjectId() + "\n");
                fw.write("HmmSubjectDatabaseName=" + hmmInputNode.getName() + "\n");
                fw.write("HmmSubjectDatabaseSourcePath=" + hmmInputNode.getDirectoryPath() + "\n");
                fw.write("HmmSubjectDatabaseDescription=" + hmmInputNode.getDescription() + "\n");
                fw.write("HmmSubjectDatabaseSequenceCount=" + hmmInputNode.getNumberOfHmms() + "\n");
                return;
            }
        }
        throw new Exception("Could not locate hmmTask for annotationTaskId=" + annoTask.getObjectId());
    }

    private void addParentTaskInfo(FileWriter fw) throws Exception {
        fw.write("MgAnnoParentTaskId=" + annoTask.getObjectId() + "\n");
        fw.write("MgAnnoParentTaskName=" + annoTask.getTaskName() + "\n");
        fw.write("MgAnnoParentTaskProjectCode=" + annoTask.getParameter(MetaGenoAnnotationTask.PARAM_project) + "\n");
        fw.write("MgAnnoParentTaskOwner=" + annoTask.getOwner() + "\n");

        List<Event> annoEvents = annoTask.getEvents();
        Date startTime = null;
        if (annoEvents == null) {
            logger.info("Received NULL annoEvents list for taskId=" + annoTask.getObjectId());
            throw new ServiceException("annoEvents ");
        }
        else {
            logger.info("Received event list with " + annoEvents.size() + " for taskId=" + annoTask.getObjectId());
        }
        for (Event e : annoEvents) {
            logger.info("Checking event type=" + e.getEventType() + " at time=" + e.getTimestamp());
            if (e.getEventType().equals(Event.PENDING_EVENT)) {
                startTime = e.getTimestamp();
            }
        }
        if (startTime == null) {
            throw new Exception("Could not locate start event for annotation task id=" + annoTask.getObjectId());
        }
        // What we want is the complete time for the annotation task, NOT INCLUDING this sqlite task.
        // Therefore, we will assume that this task is run at the end of the annotation pipeline, and
        // assume that the current time is an approximation of when the prior part of the pipeline
        // has completed.
        Date currentTime = new Date();
        fw.write("MgAnnoParentTaskStartTimeYYYY-MM-DDTHH:MM=" + createSqliteTimestamp(startTime) + "\n");
        fw.write("MgAnnoParentTaskCompleteTimeYYYY-MM-DDTHH:MM=" + createSqliteTimestamp(currentTime) + "\n");
    }

    private String createSqliteTimestamp(Date date) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        return sdf.format(date);
    }

    private void addBlastTaskInfo(FileWriter fw) throws Exception {
        BlastTask blastTask = getBlastTask();
        fw.write("MgAnnoBlastTaskId=" + blastTask.getObjectId() + "\n");
        fw.write("MgAnnoBlastTaskName=" + blastTask.getTaskName() + "\n");
        fw.write("MgAnnoBlastTaskProjectCode=" + blastTask.getParameter(BlastTask.PARAM_project) + "\n");
        fw.write("MgAnnoBlastTaskOwner=" + blastTask.getOwner() + "\n");
        List<Event> events = blastTask.getEvents();
        Date startTime = null;
        for (Event e : events) {
            if (e.getEventType().equals(Event.PENDING_EVENT)) {
                startTime = e.getTimestamp();
            }
        }
        if (startTime == null) {
            throw new Exception("Could not locate start event for blast task id=" + blastTask.getObjectId());
        }
        Date endTime = null;
        for (Event e : events) {
            if (e.getEventType().equals(Event.COMPLETED_EVENT)) {
                endTime = e.getTimestamp();
            }
        }
        if (endTime == null) {
            throw new Exception("Could not locate end event for blast task id=" + blastTask.getObjectId());
        }
        fw.write("MgAnnoBlastTaskStartTimeYYYY-MM-DDTHH:MM=" + createSqliteTimestamp(startTime) + "\n");
        fw.write("MgAnnoBlastTaskCompleteTimeYYYY-MM-DDTHH:MM=" + createSqliteTimestamp(endTime) + "\n");
    }

    private BlastTask getBlastTask() throws Exception {
        List<Task> childTasks = computeDAO.getChildTasksByParentTaskId(annoTask.getObjectId());
        for (Task t : childTasks) {
            if (t instanceof BlastTask) {
                return (BlastTask) t;
            }
        }
        return null;
    }

    private void addHmmpfamTaskInfo(FileWriter fw) throws Exception {
        HmmpfamTask hmmTask = getHmmpfamTask();
        fw.write("MgAnnoHmmpfamTaskId=" + hmmTask.getObjectId() + "\n");
        fw.write("MgAnnoHmmpfamTaskName=" + hmmTask.getTaskName() + "\n");
        fw.write("MgAnnoHmmpfamTaskProjectCode=" + hmmTask.getParameter(HmmpfamTask.PARAM_project) + "\n");
        fw.write("MgAnnoHmmpfamTaskOwner=" + hmmTask.getOwner() + "\n");
        List<Event> events = hmmTask.getEvents();
        Date startTime = null;
        for (Event e : events) {
            if (e.getEventType().equals(Event.PENDING_EVENT)) {
                startTime = e.getTimestamp();
            }
        }
        if (startTime == null) {
            throw new Exception("Could not locate start event for hmmpfam task id=" + hmmTask.getObjectId());
        }
        Date endTime = null;
        for (Event e : events) {
            if (e.getEventType().equals(Event.COMPLETED_EVENT)) {
                endTime = e.getTimestamp();
            }
        }
        if (endTime == null) {
            throw new Exception("Could not locate end event for hmmpfam task id=" + hmmTask.getObjectId());
        }
        fw.write("MgAnnoHmmpfamTaskStartTimeYYYY-MM-DDTHH:MM=" + createSqliteTimestamp(startTime) + "\n");
        fw.write("MgAnnoHmmpfamTaskCompleteTimeYYYY-MM-DDTHH:MM=" + createSqliteTimestamp(endTime) + "\n");
    }

    private HmmpfamTask getHmmpfamTask() throws Exception {
        List<Task> childTasks = computeDAO.getChildTasksByParentTaskId(annoTask.getObjectId());
        for (Task t : childTasks) {
            if (t instanceof HmmpfamTask) {
                return (HmmpfamTask) t;
            }
        }
        return null;
    }

    MetaGenoPersistAnnoSqlTask handleAnnoPipelineContext(IProcessData processData) throws Exception {
        annoTask = (MetaGenoAnnotationTask) ProcessDataHelper.getTask(processData);
        if (annoTask == null) {
            throw new MissingDataException("Could not get parent task for " + this.getClass().getName());
        }
        if (checkParentTaskForError()) {
            this.parentTaskErrorFlag = true;
            throw new MissingDataException("Parent task has ERROR event");
        }
        resultNode = (MetaGenoAnnotationResultNode) processData.getItem("META_GENO_ANNOTATION_RESULT_NODE");
        if (resultNode == null) {
            throw new MissingDataException("Could not get result node for task=" + annoTask.getObjectId());
        }
        MetaGenoPersistAnnoSqlTask persistTask = new MetaGenoPersistAnnoSqlTask();
        persistTask.setOwner(annoTask.getOwner());
        persistTask.setParameter(MetaGenoPersistAnnoSqlTask.PARAM_project, annoTask.getParameter(MetaGenoAnnotationTask.PARAM_project));
        persistTask.setParameter(MetaGenoPersistAnnoSqlTask.PARAM_input_anno_result_node_id, "" + resultNode.getObjectId());
        persistTask.setParentTaskId(annoTask.getObjectId());
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        persistTask = (MetaGenoPersistAnnoSqlTask) computeBean.saveOrUpdateTask(persistTask);
        return persistTask;
    }

    protected boolean checkParentTaskForError() {
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            String[] status = computeBean.getTaskStatus(annoTask.getObjectId());
            return status[ComputeBaseDAO.STATUS_TYPE].equals(Event.ERROR_EVENT);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return true;
        }
    }

}
