
package src.org.janelia.it.jacs.compute.service.common.file.grid.submit.teragrid;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobException;
import org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobService;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * This service submit a job to the Grid.  It's entirely extracted from work done by Sean Murphy
 * and Todd Safford.
 *
 * @author Sean Murphy
 * @author Todd Safford
 */
public abstract class SubmitTeraGridJobService implements SubmitJobService {

    protected Logger logger;

    protected Task task;
    protected IProcessData processData;
    protected FileNode resultFileNode;
    protected Set<String> jobSet = null;
    // This attribute keeps track of how many nodes we want to engage.  Minimum value is 1
    protected ComputeDAO computeDAO;

    /**
     * This method is part of IService interface and used when this class
     * or it's child is used as a processor in process file
     *
     * @param processData the running state of the process
     * @throws org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobException
     */
    public void submitJobAndWait(IProcessData processData) throws SubmitJobException {
        try {
            init(processData);
            submitJob();
        }
        catch (Exception e) {
            throw new SubmitJobException(e);
        }
    }

    /**
     * This method is invoked from GridSubmitAndWaitMDB
     *
     * @param processData the running state of the process
     * @throws org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobException
     *
     */
    public Process submitAsynchJob(IProcessData processData, String submissionKey) throws SubmitJobException {
        //logger.debug(getClass().getSimpleName() + " Process Data : " + processData);
        try {
            init(processData);
            return submitAsynchronousJob();
        }
        catch (Exception e) {
            throw new SubmitJobException(e);
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        this.processData = processData;
        // Permit the task to be predefined elsewhere
        if (this.task == null) {
            this.task = ProcessDataHelper.getTask(processData);
        }
        // Permit the resultNode to be defined elsewhere
        if (this.resultFileNode == null) {
            this.resultFileNode = ProcessDataHelper.getResultFileNode(processData);
        }
        this.jobSet = new HashSet<String>();
        if (resultFileNode == null) {
            throw new MissingDataException("ResultFileNode for createtask " + task.getObjectId() +
                    " must exist before a grid job is submitted");
        }
        // Needs to run in separate transaction
        if (computeDAO == null)
            computeDAO = new ComputeDAO(logger);
    }

    protected Process submitAsynchronousJob() throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("Preparing " + task.getTaskName() + " (task id = " + this.task.getObjectId() + " for asyncronous DRMAA submission)");
        }
        FileWriter writer = new FileWriter(new File(resultFileNode.getDirectoryPath() + File.separator + "tgScript.sh"));
        try {
            createJobScriptAndConfigurationFiles(writer);
        }
        finally {
            writer.flush();
            writer.close();
        }
//        Process proc = Runtime.getRuntime().exec(task.getObjectId(), task.getOwner(), resultFileNode.getDirectoryPath(),
//                jt, 1, jobIncrementStop, 1);

        return Runtime.getRuntime().exec("ls");

    }


    protected void submitJob() throws Exception {

        if (logger.isInfoEnabled()) {
            logger.info("Preparing " + task.getTaskName() + " (task id = " + this.task.getObjectId() + " for TeraGrid submission");
        }

        logger.info("******** " + jobSet.size() + " jobs submitted to grid **********");

        // now wait for completion
//        boolean gridActionSuccessful = waitForJobs(jobSet, "Computing results for " + resultFileNode.getObjectId(), jsl, -1);
//        if (!gridActionSuccessful)
//        {
//            String err = "Error ' \" + drmaa.getError() + \" ' executing grid jobs.";
//            logger.error("err");
//            throw new WaitForJobException(err);
//            }
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     *
     * @param writer file writer
     * @throws org.janelia.it.jacs.compute.engine.data.MissingDataException
     *                             - cannot find a file needed for processing
     * @throws java.io.IOException - error accessing a file
     * @throws org.janelia.it.jacs.compute.access.DaoException
     *                             - error interacting with the database
     * @throws org.janelia.it.jacs.model.vo.ParameterException
     *                             - error accessing task parameters
     */
    protected abstract void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception;

}