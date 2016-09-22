
package org.janelia.it.jacs.compute.service.hmmer3;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.file.PartitionList;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.hmmer3.HMMER3Task;
import org.janelia.it.jacs.model.user_data.hmmer3.HMMER3ResultFileNode;
import org.janelia.it.jacs.model.user_data.hmmer3.Hmmer3DatabaseNode;
import org.janelia.it.jacs.model.vo.ParameterException;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * From jacs.properties
 * #HMMER3
 HMMER3.HMMER3Cmd=/misc/local/hmmer-3.0/bin/hmmscan
 HMMER3.Queue=-l medium
 HMMER3.MaxEntriesPerJob=200
 */
public class HMMER3SubmitJobService extends SubmitDrmaaJobService {
    public static final String HMMER3_CMD = SystemConfigurationProperties.getString("HMMER3.HMMER3Cmd");
    public static final String HMMER3_QUEUE = SystemConfigurationProperties.getString("HMMER3.Queue");
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("HMMER3.MaxEntriesPerJob");
    public static final String RESULT_PREFIX = "r";

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "hmmer3";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        HMMER3Task hmmer3Task = (HMMER3Task) task;
        PartitionList partitionList = (PartitionList) processData.getItem(FileServiceConstants.PARTITION_LIST);
        String dbNodeIdString = hmmer3Task.getParameter(HMMER3Task.PARAM_db_node_id);
        List<File> queryFiles = (List<File>) processData.getMandatoryItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST);
        Map<File, File> queryOutputFileMap = (Map<File, File>) processData.getMandatoryItem(FileServiceConstants.INPUT_OUTPUT_DIR_MAP);

        Long dbNodeId = new Long(dbNodeIdString);
        Hmmer3DatabaseNode dbNode = (Hmmer3DatabaseNode) computeDAO.genericLoad(Hmmer3DatabaseNode.class, dbNodeId);

        Integer totalHmmCount = dbNode.getNumberOfHmms();  // for now we assume only a single HmmDb, which may be partitioned
        if (partitionList.size() == 0) {
            throw new MissingDataException("PartitionList is empty");
        }

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(hmmer3Task, totalHmmCount, writer);

        int configIndex = 1;
        Map<File, List<File>> inputOutputFileListMap = new HashMap<File, List<File>>();
        for (File queryFile : queryFiles) {
            List<File> outputFileList = new ArrayList<File>();
            File outputDir = queryOutputFileMap.get(queryFile);
            if (outputDir == null) {
                throw new MissingDataException("Could not find output directory for query file=" + queryFile.getAbsolutePath());
            }
            if (!outputDir.exists()) {
                throw new IOException("Could not confirm that output directory exists=" + outputDir.getAbsolutePath());
            }
            for (int i = 0; i < partitionList.getFileList().size(); i++) {
                String partitionResultName = RESULT_PREFIX + "_" + i;
                File subjectDatabase = partitionList.getFileList().get(i);
                if (!subjectDatabase.exists()) {
                    throw new MissingDataException("Subject database " + subjectDatabase.getAbsolutePath() + " does not exist");
                }
                File outputFile = new File(new File(outputDir, HMMER3ResultFileNode.TAG_OUTPUT_FILE).getAbsolutePath() + partitionResultName);
                File perSeqHitsFile = new File(new File(outputDir, HMMER3ResultFileNode.TAG_PER_SEQ_HITS_FILE).getAbsolutePath() + partitionResultName);
                File perDomainHitsFile = new File(new File(outputDir, HMMER3ResultFileNode.TAG_PER_DOMAIN_HITS_FILE).getAbsolutePath() + partitionResultName);

                outputFileList.add(outputFile);
                configIndex = writeConfigFile(queryFile, subjectDatabase, outputFile, configIndex, perSeqHitsFile, perDomainHitsFile);
                configIndex++;
            }
            inputOutputFileListMap.put(queryFile, outputFileList);
        }
        processData.putItem(FileServiceConstants.INPUT_OUTPUT_FILE_LIST_MAP, inputOutputFileListMap);

        int configFileCount = new File(getSGEConfigurationDirectory()).listFiles(new ConfigurationFileFilter()).length;
        setJobIncrementStop(configFileCount);
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Hmm3 Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + HMMER3_QUEUE);
        jt.setNativeSpecification(HMMER3_QUEUE);
    }

    private int writeConfigFile(File queryFile, File subjectDatabase, File outputFile, int configIndex,
                                File perseqFile, File perdomainFile) throws IOException {
        // For each partition, we need to create an input file for the script
        File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName()+ "Configuration." + configIndex);
        FileWriter fw = new FileWriter(configFile);
        fw.write(queryFile.getAbsolutePath() + "\n");
        fw.write(subjectDatabase.getAbsolutePath() + "\n");
        fw.write(outputFile.getAbsolutePath() + "\n");
        fw.write(perseqFile.getAbsolutePath() + "\n");
        fw.write(perdomainFile.getAbsolutePath() + "\n");
        fw.close();

        return configIndex;
    }

    private void createShellScript(HMMER3Task hmmer3Task, long totalHmmCount, FileWriter writer)
            throws IOException, ParameterException {
        String initialCmd = hmmer3Task.generateCommandLineOptionString();
        if (totalHmmCount > 0) {
            initialCmd = initialCmd.trim() + " -Z " + totalHmmCount;
        }
        String hmm3FullCmd = HMMER3_CMD + " " + initialCmd +
                " -o $OUTPUTFILE --tblout $PERSEQHITSTABLE --domtblout $PERDOMAINHITSTABLE $HMM3DB $QUERYFILE ";
        StringBuffer script = new StringBuffer();
        script.append("read QUERYFILE\n");
        script.append("read HMM3DB\n");
        script.append("read OUTPUTFILE\n");
        script.append("read PERSEQHITSTABLE\n");
        script.append("read PERDOMAINHITSTABLE\n");
        script.append(hmm3FullCmd).append("\n");
        writer.write(script.toString());

        //print results
        logger.debug(getClass().getName()+"\n"+
            hmm3FullCmd+"\n"
        );
    }

    private static class ConfigurationFileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name != null && name.startsWith("hmmer3Configuration.");
        }
    }

}
