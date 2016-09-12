
package org.janelia.it.jacs.compute.service.hmmer3;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.file.MultiFastaSplitterService;
import org.janelia.it.jacs.compute.service.common.file.PartitionList;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.hmmer3.HMMER3Task;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.hmmer3.Hmmer3DatabaseNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * From jacs.properties
 * # Hmmer3 Properties
 Hmmer3.MaxOutputFileSizeMB=200
 Hmmer3.MaxQueriesPerExec=5000
 Hmmer3.MaxNumberOfJobs=45000
 Hmmer3.Cmd=/usr/local/packages/hmmer-3.0/bin/hmmscan
 Hmmer3.ResultName=hmmer3_output
 Hmmer3.LowThreshold=50
 */
public class HMMER3MultiFastaSplitterService extends MultiFastaSplitterService {
    private HMMER3Task hmmer3Task;
    private ComputeDAO computeDAO;
    protected FileNode resultFileNode;
    private static final int MAX_BLAST_OUTPUT_FILE_SIZE = SystemConfigurationProperties.getInt("Hmmer3.MaxOutputFileSizeMB") * 1000000;
    private static final int MAX_QUERIES_PER_EXEC = SystemConfigurationProperties.getInt("Hmmer3.MaxQueriesPerExec");
    private static final int MAX_NUMBER_OF_JOBS = SystemConfigurationProperties.getInt("Hmmer3.MaxNumberOfJobs");
    private static final int XML_JUNK_SIZE_IN_OUTPUT = 1000;
    private static final int MATCH_PLUS_BONDS_PLUS_MATE = 3;
    private static final int DEFAULT_EXPECTED_ALIGNMENTS_WITH_NO_LIMIT = 100;

    public HMMER3MultiFastaSplitterService() {
    }

    public void execute(IProcessData processData)
            throws ServiceException {
        try {
            hmmer3Task = (HMMER3Task) ProcessDataHelper.getTask(processData);
            computeDAO = new ComputeDAO(logger);
            resultFileNode = ProcessDataHelper.getResultFileNode(processData);
            File inputFile = getInputFileFromTask();
            processData.putItem(FileServiceConstants.INPUT_FILE, inputFile);
            processData.putItem(FileServiceConstants.MAX_RESULTS_PER_JOB, DEFAULT_EXPECTED_ALIGNMENTS_WITH_NO_LIMIT);
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

    private File getInputFileFromTask()
            throws ServiceException, IOException, InterruptedException {
        Long queryNodeId = Long.valueOf(hmmer3Task.getParameter(HMMER3Task.PARAM_query_node_id));
        File inputFile;
        try {
            inputFile = getFastaFile(queryNodeId);
        }
        catch (ClassCastException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        return inputFile;
    }

    private File getFastaFile(Long inputNodeId)
            throws ServiceException {
        FastaFileNode inputNode = (FastaFileNode) computeDAO.genericGet(FastaFileNode.class, inputNodeId);
        if (inputNode == null) {
            logger.info((new StringBuilder()).append("FastaFileNode with inputNodeId:").append(inputNodeId).append(" does not exist").toString());
            return null;
        }
        else {
            return new File(inputNode.getFastaFilePath());
        }
    }

    private PartitionList getPartitionList()
            throws ServiceException {
        PartitionList partitionList = new PartitionList();
        Long hmmDbNodeId = Long.valueOf(hmmer3Task.getParameter(HMMER3Task.PARAM_db_node_id));
        try {
            Hmmer3DatabaseNode hmmDbNode = (Hmmer3DatabaseNode) computeDAO.genericLoad(Hmmer3DatabaseNode.class, hmmDbNodeId);
            File dbFile = new File(hmmDbNode.getFilePathByTag(Hmmer3DatabaseNode.TAG_ALL));
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
