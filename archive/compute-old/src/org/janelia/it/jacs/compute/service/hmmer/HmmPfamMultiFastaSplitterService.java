
package org.janelia.it.jacs.compute.service.hmmer;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.file.MultiFastaSplitterService;
import org.janelia.it.jacs.compute.service.common.file.PartitionList;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.hmmer.HmmpfamTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.hmmer.HmmerPfamDatabaseNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 29, 2008
 * Time: 4:36:13 PM
 * From jacs.properties
 * # HmmerPfam Properties
 HmmerPfam.MaxOutputFileSizeMB=200
 HmmerPfam.MaxQueriesPerExec=5000
 HmmerPfam.MaxNumberOfJobs=45000
 HmmerPfam.Cmd=/home/ccbuild/bin/hmmpfam_cell.sh
 HmmerPfam.ResultName=hmmpfam_output
 HmmerPfam.LowThreshold=50
 */
public class HmmPfamMultiFastaSplitterService extends MultiFastaSplitterService {
    private HmmpfamTask hmmpfamTask;
    private ComputeDAO computeDAO;
    protected FileNode resultFileNode;
    private static final int MAX_BLAST_OUTPUT_FILE_SIZE = (SystemConfigurationProperties.getInt("HmmerPfam.MaxOutputFileSizeMB") * 1000000);
    private static final int MAX_QUERIES_PER_EXEC = SystemConfigurationProperties.getInt("HmmerPfam.MaxQueriesPerExec");
    private static final int MAX_NUMBER_OF_JOBS = SystemConfigurationProperties.getInt("HmmerPfam.MaxNumberOfJobs");
    private static final int XML_JUNK_SIZE_IN_OUTPUT = 1000;
    private static final int MATCH_PLUS_BONDS_PLUS_MATE = 3;
    private static final int DEFAULT_EXPECTED_ALIGNMENTS_WITH_NO_LIMIT = 100;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            this.hmmpfamTask = (HmmpfamTask) ProcessDataHelper.getTask(processData);
            computeDAO = new ComputeDAO(logger);
            this.resultFileNode = ProcessDataHelper.getResultFileNode(processData);
            File inputFile = getInputFileFromTask();
            processData.putItem(FileServiceConstants.INPUT_FILE, inputFile);
            int dbAlignmentsRequested = getDatabaseAlignments();
            if (dbAlignmentsRequested == 0) {
                processData.putItem(FileServiceConstants.MAX_RESULTS_PER_JOB, DEFAULT_EXPECTED_ALIGNMENTS_WITH_NO_LIMIT);
            }
            else {
                processData.putItem(FileServiceConstants.MAX_RESULTS_PER_JOB, dbAlignmentsRequested);
            }
            PartitionList partitionList = getPartitionList();
            processData.putItem(FileServiceConstants.PARTITION_LIST, partitionList);
            processData.putItem(FileServiceConstants.MAX_OUTPUT_SIZE, MAX_BLAST_OUTPUT_FILE_SIZE);
            processData.putItem(FileServiceConstants.MAX_INPUT_ENTRIES_PER_JOB, MAX_QUERIES_PER_EXEC);
            processData.putItem(FileServiceConstants.MAX_NUMBER_OF_JOBS, MAX_NUMBER_OF_JOBS);
            processData.putItem(FileServiceConstants.OUTPUT_ADDITIONAL_SIZE, XML_JUNK_SIZE_IN_OUTPUT);
            processData.putItem(FileServiceConstants.PER_INPUT_ENTRY_SIZE_MULTIPLIER, MATCH_PLUS_BONDS_PLUS_MATE);
            super.execute(processData);
        }
        catch (ServiceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private File getInputFileFromTask() throws ServiceException, IOException, InterruptedException {
        Long queryNodeId = Long.parseLong(hmmpfamTask.getParameter(HmmpfamTask.PARAM_query_node_id));
        File inputFile;
        try {
            // This would be the norm
            inputFile = getFastaFile(queryNodeId);
        }
        catch (ClassCastException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        return inputFile;
    }

    /**
     * Returns the original fasta file created by FileUploadController (either through upload or query entry on GUI)
     *
     * @param inputNodeId
     * @return
     * @throws ServiceException
     */
    private File getFastaFile(Long inputNodeId) throws ServiceException {
        FastaFileNode inputNode = (FastaFileNode) computeDAO.genericGet(FastaFileNode.class, inputNodeId);
        if (inputNode == null) {
            logger.info("FastaFileNode with inputNodeId:" + inputNodeId + " does not exist");
            return null;
        }
        else {
            return new File(inputNode.getFastaFilePath());
        }
    }

    private int getDatabaseAlignments() {
        String dbAlignments = hmmpfamTask.getParameter(HmmpfamTask.PARAM_max_best_domain_aligns);
        return Integer.parseInt(dbAlignments);
    }

    // Note: this method will have to be modified to deal with multiple databases per HMM job should we
    // support this in the future.
    private PartitionList getPartitionList() throws ServiceException {
        PartitionList partitionList = new PartitionList();
        Long hmmpfamDbNodeId = Long.parseLong(hmmpfamTask.getParameter(HmmpfamTask.PARAM_pfam_db_node_id));
        File dbFile;
        try {
            HmmerPfamDatabaseNode hmpDbNode = (HmmerPfamDatabaseNode) computeDAO.genericLoad(
                    HmmerPfamDatabaseNode.class, hmmpfamDbNodeId);
            dbFile = new File(hmpDbNode.getFilePathByTag(HmmerPfamDatabaseNode.TAG_PFAM));
            partitionList.setDatabaseLength(dbFile.length());
            List<File> fileList = new ArrayList<File>();
            fileList.add(dbFile);
            partitionList.setFileList(fileList);
            return partitionList;
        }
        catch (Exception ex) {
            throw new ServiceException(ex.getMessage(), ex);
        }
    }

}
