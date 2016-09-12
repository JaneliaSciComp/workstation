
package org.janelia.it.jacs.compute.service.metageno;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.SimpleMultiFastaSplitterService;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.metageno.TrnaScanTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.metageno.TrnaScanResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 13, 2009
 * Time: 1:01:58 PM
 * From jacs.properties
 * # TrnaScan Properties
 TrnaScan.DefaultFastaEntriesPerExec=10000
 TrnaScan.Cmd=/usr/local/common/tRNAscan-SE
 TrnaScan.Queue=-l default
 TrnaScan.BsmlCmd=tRNAscan-SE2bsml.pl
 TrnaScan.ExtractCmd=camera_extract_trna_features.pl
 TrnaScan.MaskCmd=mask_by_analysis.pl
 */
public class TrnaScanService extends SubmitDrmaaJobService {
    public static String TRNA_SCAN_TASK = "TRNA_SCAN_TASK";
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("TrnaScan.DefaultFastaEntriesPerExec");

    /* SCRIPT DEPENDENCIES

        TrnaScan.Cmd=/usr/local/common/tRNAscan-SE
            <none>
        TrnaScan.BsmlCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/tRNAscan-SE2bsml
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev);
            use Pod::Usage;
            use Ergatis::Logger;
            use Ergatis::IdGenerator;
            use Chado::Gene;
            use BSML::GenePredictionBsml;
            use BSML::BsmlBuilder;
        TrnaScan.ExtractCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/camera_extract_trna_features
            use strict;
            use warnings;
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev);
            use Pod::Usage;
            use XML::Twig;
            use Ergatis::Logger;
            use Ergatis::IdGenerator;
            use BSML::BsmlBuilder;
            use File::Basename;
            use Fasta::SimpleIndexer;
        TrnaScan.MaskCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/mask_by_analysis
            use strict;
            use warnings;
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev);
            use Pod::Usage;
            use XML::Twig;
            use Ergatis::Logger;
            use BSML::BsmlBuilder;
            use File::Basename;
            use Fasta::SimpleIndexer;

        MODULE SUMMARY
            Ergatis, Chado, BSML, Fasta

     */

    private static final String trnascanCmd = SystemConfigurationProperties.getString("TrnaScan.Cmd");
    private static final String bsmlCmd = SystemConfigurationProperties.getString("TrnaScan.BsmlCmd");
    private static final String extractCmd = SystemConfigurationProperties.getString("TrnaScan.ExtractCmd");
    private static final String maskCmd = SystemConfigurationProperties.getString("TrnaScan.MaskCmd");
    private static final String resultFilename = TrnaScanResultNode.RAW_OUTPUT_FILENAME;
    private static final String queueName = SystemConfigurationProperties.getString("TrnaScan.Queue");
    public static final String RESULT_PREFIX = TrnaScanResultNode.RESULT_EXTENSION_PREFIX;

    private List<File> inputFiles;
    private List<File> outputFiles;
    TrnaScanTask trnaScanTask;
    TrnaScanResultNode trnaScanResultNode;
    private String sessionName;

    public static TrnaScanTask createDefaultTask() {
        // No service default params
        return new TrnaScanTask();
    }

    protected void init(IProcessData processData) throws Exception {
        trnaScanTask = getTrnaScanTask(processData);
        task = trnaScanTask;
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        trnaScanResultNode = createResultFileNode();
        resultFileNode = trnaScanResultNode;
        super.init(processData);
        createIdRepository();
        // First check if we have been assigned a specific file to process. If so, then we will not
        // worry about its size, assuming this has already been configured.
        inputFiles = new ArrayList<File>();
        outputFiles = new ArrayList<File>();
        File inputFile = getMgInputFile(processData);
        if (inputFile == null) {
            // Since we have not been assigned a file, we will get the input node from the task
            Long inputFastaNodeId = new Long(trnaScanTask.getParameter(TrnaScanTask.PARAM_input_fasta_node_id));
            FastaFileNode inputFastaNode = (FastaFileNode) computeDAO.getNodeById(inputFastaNodeId);
            inputFile = new File(inputFastaNode.getFastaFilePath());
            logger.info("TrnaScanService received null input file from ProcessData, so using this file from task=" + inputFile.getAbsolutePath());
        }
        else {
            logger.info("TrnaScanService received this input file from ProcessData=" + inputFile.getAbsolutePath());
        }
        inputFiles = SimpleMultiFastaSplitterService.splitInputFileToList(processData, inputFile, DEFAULT_ENTRIES_PER_EXEC, logger);
        logger.info(this.getClass().getName() + " trnaScanTaskId=" + task.getObjectId() + " init() end");
    }

    protected String getGridServicePrefixName() {
        return "trnaScan";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    private TrnaScanTask getTrnaScanTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Object possibleTask = processData.getItem(TRNA_SCAN_TASK);
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
                return (TrnaScanTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getTrnaScanTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private TrnaScanResultNode createResultFileNode() throws ServiceException, IOException, DaoException {
        TrnaScanResultNode resultFileNode;

        // Check if we already have a result node for this task
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof TrnaScanResultNode) {
                return (TrnaScanResultNode) node;
            }
        }

        // Create new node
        resultFileNode = new TrnaScanResultNode(task.getOwner(), task,
                "TrnaScanResultNode", "TrnaScanResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        computeDAO.saveOrUpdate(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    private File getMgInputFile(IProcessData processData) {
        try {
            File inputFile = (File) processData.getItem("MG_INPUT_ARRAY");
            if (inputFile != null) {
                logger.info("TrnaScanService using input file=" + inputFile.getAbsolutePath());
            }
            return inputFile;
        }
        catch (Exception e) {
            return null; // assume the value simply isn't in processData
        }
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        TrnaScanTask trnaScanTask = (TrnaScanTask) task;

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(trnaScanTask, writer);

        int configIndex = 1;
        File outputDir = new File(resultFileNode.getDirectoryPath());
        for (File inputFile : inputFiles) {
            File outputFile = new File(new File(outputDir, resultFilename).getAbsolutePath() + "." + configIndex);
            outputFiles.add(outputFile);
            configIndex = writeConfigFile(inputFile, outputFile, configIndex);
            configIndex++;
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

    private void createShellScript(TrnaScanTask trnaScanTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            String initialCmd = trnascanCmd + " " + trnaScanTask.generateCommandStringNotIncludingIOParams();
            String fullCmd = initialCmd + " \"$INPUTFILE\" > \"$OUTPUTFILE\"";
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");
            script.append("unset PERL5LIB\n");
            script.append("export PERL5LIB=").append(MetaGenoPerlConfig.PERL5LIB).append("\n");
            script.append("export PERL_MOD_DIR=").append(MetaGenoPerlConfig.PERL_MOD_DIR).append("\n");
            script.append("TMPDIR=\"$OUTPUTFILE\".tmp\n");
            script.append("mkdir \"$OUTPUTFILE\".tmp\n");
            script.append(fullCmd).append("\n");
            String bsmlCmd = formatBsmlCmd("\"$INPUTFILE\"", "\"$OUTPUTFILE\"");
            script.append(bsmlCmd).append("\n");
            String extractCmd = formatExtractCmd("\"$OUTPUTFILE\"");
            script.append(extractCmd).append("\n");
            String trnaFastaCmd = formatTrnaFastaCmd("\"$OUTPUTFILE\"");
            script.append(trnaFastaCmd).append("\n");
            String maskFastaCmd = formatMaskFastaCmd("\"$OUTPUTFILE\"");
            script.append(maskFastaCmd).append("\n");
            String createMaskFastaCmd = formatCreateMaskFastaCmd("\"$OUTPUTFILE\"");
            script.append(createMaskFastaCmd).append("\n");
            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    private int writeConfigFile(File inputFile, File outputFile, int configIndex) throws IOException {
        File configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(configIndex));
        while (configFile.exists()) {
            configFile = new File(getSGEConfigurationDirectory(), buildConfigFileName(++configIndex));
        }
        FileOutputStream fos = new FileOutputStream(configFile);
        PrintWriter configWriter = new PrintWriter(fos);
        try {
            configWriter.println(inputFile.getAbsolutePath());
            configWriter.println(outputFile.getAbsolutePath());
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

    public void postProcess() throws MissingDataException {
        try {
            File maskedFile = new File(resultFileNode.getFilePathByTag(TrnaScanResultNode.TAG_POSTMASK_FASTA_OUTPUT));
            processData.putItem("MG_INPUT_ARRAY", maskedFile);
            // Raw tRNA scan
            File rawOutputFile = new File(resultFileNode.getFilePathByTag(TrnaScanResultNode.TAG_RAW_OUTPUT));
            FileUtil.concatFilesUsingSystemCall(outputFiles, rawOutputFile);
            for (File file : outputFiles) {
                deleteFile(file);
                File tmpDir = new File(file.getAbsolutePath() + ".tmp");
                deleteFile(tmpDir);
            }
            // bsml
            for (File file : outputFiles) {
                File bsmlFile = new File(file.getAbsolutePath() + ".bsml");
                deleteFile(bsmlFile);
            }
            // extracted tRNA fasta files
            for (File file : outputFiles) {
                File tRNADir = new File(file.getAbsolutePath() + "_extract");
                File[] files = tRNADir.listFiles();
                for (File f : files) {
                    deleteFile(f);
                }
                deleteFile(tRNADir);
            }
            // Consolidate tRNA fasta files
            File tRNAFastaFile = new File(resultFileNode.getFilePathByTag(TrnaScanResultNode.TAG_TRNA_FASTA_OUTPUT));
            List<File> tRNAFastaList = new ArrayList<File>();
            for (File file : outputFiles) {
                File tf = new File(file.getAbsolutePath() + "_tRNA.fasta");
                tRNAFastaList.add(tf);
            }
            FileUtil.concatFilesUsingSystemCall(tRNAFastaList, tRNAFastaFile);
            for (File file : tRNAFastaList) {
                deleteFile(file);
            }
            // Remove mask working files
            for (File file : outputFiles) {
                File d = new File(file.getAbsolutePath() + "_soft_mask_tRNA");
                File[] files = d.listFiles();
                for (File f : files) {
                    deleteFile(f);
                }
                deleteFile(d);
                File bsmlFile = new File(d.getAbsolutePath() + ".bsml");
                deleteFile(bsmlFile);
            }
            // Consolidate masked fasta files
            File maskedFastaFile = new File(resultFileNode.getFilePathByTag(TrnaScanResultNode.TAG_POSTMASK_FASTA_OUTPUT));
            List<File> maskedFiles = new ArrayList<File>();
            for (File file : outputFiles) {
                File mf = new File(file.getAbsolutePath() + "_soft_mask_tRNA.fasta");
                maskedFiles.add(mf);
            }
            FileUtil.concatFilesUsingSystemCall(maskedFiles, maskedFastaFile);
            for (File file : maskedFiles) {
                deleteFile(file);
            }
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

    private void deleteFile(File targetFile) {
        boolean deleteSuccess = targetFile.delete();
        if (!deleteSuccess){
            logger.error("Unable to delete file "+targetFile.getAbsolutePath());
        }
    }

    private void createIdRepository() throws Exception {
        File idRepositoryDir = getIdRepositoryDir();
        if (!idRepositoryDir.mkdirs()) {
            throw new Exception("Could not create id repository dir=" + idRepositoryDir.getAbsolutePath());
        }
        File idRepositoryFile = new File(idRepositoryDir, "valid_id_repository");
        if (!idRepositoryFile.createNewFile()) {
            throw new Exception("Could not create valid repository file=" + idRepositoryFile.getAbsolutePath());
        }
    }

    private File getIdRepositoryDir() {
        File resultFileNodeDir = new File(resultFileNode.getDirectoryPath());
        return new File(resultFileNodeDir, "id_repository");
    }

    private String formatBsmlCmd(String inputFastaFilepath, String rawOutputFilepath) {
        return MetaGenoPerlConfig.PERL_EXEC + " " +
                MetaGenoPerlConfig.PERL_BIN_DIR + "/" + bsmlCmd + " --input " + rawOutputFilepath +
                " --output " + rawOutputFilepath + ".bsml" +
                " --fasta_input " + inputFastaFilepath +
                " --id_repository " + getIdRepositoryDir().getAbsolutePath();
    }

    private String formatExtractCmd(String rawOutputFilepath) throws Exception {
        /*
         * --input <bsml input file>
         * --output <output file>
         * --output_prefix <prefix>
         * --analysis_types tRNAscan-SE
         * --feature_types tRNA
         * --rna_type unknown
         * --id_repository <id repository>
         * --ergatis_id 1
         * -m
         */
        File bsmlInputFile = new File(rawOutputFilepath + ".bsml");
        File extractOutputDir = new File(rawOutputFilepath + "_extract");
        String mkDirCmd = "mkdir " + extractOutputDir.getName() + "\n";
        String extractFullCmd = MetaGenoPerlConfig.PERL_EXEC + " " +
                MetaGenoPerlConfig.PERL_BIN_DIR + "/" + extractCmd +
                " --input " + bsmlInputFile.getName() +
                " --output " + extractOutputDir.getName() +
                " --output_prefix p" +
                " --analysis_types tRNAscan-SE" +
                " --feature_types tRNA" +
                " --rna_type unknown" +
                " --id_repository " + getIdRepositoryDir().getAbsolutePath() +
                " --ergatis_id=1" +
                " -m";
        return mkDirCmd + extractFullCmd;
    }

    private String formatTrnaFastaCmd(String rawOutputFilepath) {
        File extractOutputDir = new File(rawOutputFilepath + "_extract");
        return "cat " + extractOutputDir.getName() + "/*.tRNA.fsa > " + rawOutputFilepath + "_tRNA.fasta";
    }

    private String formatMaskFastaCmd(String rawOutputFilepath) throws IOException {
        /*
         * Default values:
         *     MASK_CHARACTER = X
         *     ANALYSIS_TYPES = tRNAscan-SE
         *     SOFTMASK = 1
         *     RANDOM = 0
         *     OUTPUT_TOKEN = soft_mask_tRNA
         *     COMPONENT = mask_by_analysis
         *
         *     --input <input bsml file>
         *     --output <output file>
         *     --output_prefix <prefix>
         *     --analysis_types tRNAscan-SE
         *     --mask_char X
         *     --random 0
         *     --softmask 1
         */
        File bsmlInputFile = new File(rawOutputFilepath + ".bsml");
        File maskOutputDir = new File(rawOutputFilepath + "_soft_mask_tRNA");
        String mkDirCmd = "mkdir " + maskOutputDir.getName() + "\n";
        String extractFullCmd = MetaGenoPerlConfig.PERL_EXEC + " " +
                MetaGenoPerlConfig.PERL_BIN_DIR + "/" + maskCmd +
                " --input " + bsmlInputFile.getName() +
                " --output " + maskOutputDir.getName() +
                " --output_prefix p" +
                " --analysis_types tRNAscan-SE" +
                " --mask_char X" +
                " --random 0" +
                " --softmask 1";
        return mkDirCmd + extractFullCmd;
    }

    private String formatCreateMaskFastaCmd(String rawOutputFilepath) throws Exception {
        File maskOutputDir = new File(rawOutputFilepath + "_soft_mask_tRNA");
        // First copy fasta file
        String cmd = "cat " + maskOutputDir.getName() + "/*.fsa > " + rawOutputFilepath + "_soft_mask_tRNA.fasta" + "\n";
        // Next copy bsml file
        cmd += "cat " + maskOutputDir.getName() + "/*.bsml > " + rawOutputFilepath + "_soft_mask_tRNA.bsml";
        return cmd;
    }

}
