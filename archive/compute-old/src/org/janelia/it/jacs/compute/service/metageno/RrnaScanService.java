
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
import org.janelia.it.jacs.model.tasks.metageno.RrnaScanTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.metageno.RrnaScanResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 23, 2009
 * Time: 12:32:03 PM
 * From jacs.properties
 * # RrnaScan Properties
 RrnaScan.DefaultFastaEntriesPerExec=10000
 RrnaScan.Queue=-l default
 RrnaScan.Database=/usr/local/annotation/CAMERA/CustomDB/camera_rRNA_finder.all_rRNA.coded.cdhit_80.fsa
 RrnaScan.BlastCmd=blast-2.2.15/bin/blastall
 RrnaScan.FirstBlastOptions=-e 0.1 -F T -b 1 -v 1 -z 3000000000 -W 9
 RrnaScan.SecondBlastOptions=-e 1e-4 -F 'm L' -b 1500 -v 1500 -q 5 -r 4 -X 1500 -z 3000000000 -W 9 -U T
 RrnaScan.Blast2BtabCmd=wu-blast2btab.pl
 RrnaScan.Btab2BsmlCmd=blastbtab2bsml.pl
 RrnaScan.Bsml2FastaCmd=camera_bsml_to_fasta_filter_on_alignment.pl
 RrnaScan.Blast2TableCmd=blast_raw_to_table.pl
 RrnaScan.BlastTableIntervalCmd=blast_table_interval_collapse.pl
 RrnaScan.RrnaFinder2BsmlCmd=camera_rrna_finder2bsml.pl
 RrnaScan.RrnaSequenceExtractCmd=camera_rrna_sequence_extract.pl
 RrnaScan.MaskCmd=mask_by_analysis.pl
 RrnaScan.Min5sLength=25
 RrnaScan.Min16sLength=250
 RrnaScan.Min18sLength=250
 RrnaScan.Min23sLength=250
 */
public class RrnaScanService extends SubmitDrmaaJobService {
    public static String RRNA_SCAN_TASK = "RRNA_SCAN_TASK";
    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("RrnaScan.DefaultFastaEntriesPerExec");

    private static final String resultFilename = RrnaScanResultNode.BASE_OUTPUT_FILENAME;

    /*
        SCRIPT DEPENDENCIES

        blast2btabCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/wu-blast2btab
            use strict;
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev pass_through);
            use Pod::Usage;
            use Ergatis::Logger;
            use Bio::SearchIO;
        btab2bsmlCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/blastbtab2bsml
            use strict;
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev);
            use English;
            use File::Basename;
            use File::Path;
            use Pod::Usage;
            use Ergatis::Logger;
            use BSML::BsmlRepository;
            use BSML::BsmlBuilder;
            use BSML::BsmlParserTwig;
        bsml2fastaCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/camera_bsml_to_fasta_filter_on_alignment
            use strict;
            use warnings;
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev);
            use File::Basename;
            use BSML::BsmlParserSerialSearch;
            use Ergatis::Logger;
            use Pod::Usage;
            use Fasta::SimpleIndexer;
        blast2TableCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/blast_raw_to_table
            use warnings;
            use strict;
            use Bio::SearchIO;
            use Carp;
        blastTableIntervalCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/blast_table_interval_collapse
            <none>
        finder2bsmlCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/camera_rrna_finder2bsml
            use strict;
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev);
            use Pod::Usage;
            use Ergatis::Logger;
            use BSML::BsmlBuilder;
        extractCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/camera_rrna_sequence_extract
            use Fasta::SimpleIndexer;
            use File::Basename;
            use TIGR::EUIDService;
            use Carp;
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev);
            use Pod::Usage;
            use Ergatis::Logger;
        maskCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/mask_by_analysis
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
            Ergatis, Bio, BSML, Fasta, TIGR
     */

    private static final String databasePath = SystemConfigurationProperties.getString("RrnaScan.Database");
    private static final String blastCmd = SystemConfigurationProperties.getString("Executables.ModuleBase")+
            SystemConfigurationProperties.getString("RrnaScan.BlastCmd");
    private static final String blast2btabCmd = SystemConfigurationProperties.getString("RrnaScan.Blast2BtabCmd");
    private static final String btab2bsmlCmd = SystemConfigurationProperties.getString("RrnaScan.Btab2BsmlCmd");
    private static final String bsml2fastaCmd = SystemConfigurationProperties.getString("RrnaScan.Bsml2FastaCmd");
    private static final String blast2tableCmd = SystemConfigurationProperties.getString("RrnaScan.Blast2TableCmd");
    private static final String blastTableIntervalCmd = SystemConfigurationProperties.getString("RrnaScan.BlastTableIntervalCmd");
    private static final String finder2bsmlCmd = SystemConfigurationProperties.getString("RrnaScan.RrnaFinder2BsmlCmd");
    private static final String extractCmd = SystemConfigurationProperties.getString("RrnaScan.RrnaSequenceExtractCmd");
    private static final String maskCmd = SystemConfigurationProperties.getString("RrnaScan.MaskCmd");

    private static final String queueName = SystemConfigurationProperties.getString("RrnaScan.Queue");

    private List<File> inputFiles;
    private List<File> outputFiles;
    RrnaScanTask rrnaScanTask;
    RrnaScanResultNode rrnaScanResultNode;
    int totalBaseCount = 0;
    private String sessionName;

    public static RrnaScanTask createDefaultTask() {
        RrnaScanTask task = new RrnaScanTask();
        String firstBlastOptions = SystemConfigurationProperties.getString("RrnaScan.FirstBlastOptions");
        String secondBlastOptions = SystemConfigurationProperties.getString("RrnaScan.SecondBlastOptions");
        String min5sLength = SystemConfigurationProperties.getString("RrnaScan.Min5sLength");
        String min16sLength = SystemConfigurationProperties.getString("RrnaScan.Min16sLength");
        String min18sLength = SystemConfigurationProperties.getString("RrnaScan.Min18sLength");
        String min23sLength = SystemConfigurationProperties.getString("RrnaScan.Min23sLength");
        task.setParameter(RrnaScanTask.PARAM_initial_blast_options, firstBlastOptions);
        task.setParameter(RrnaScanTask.PARAM_second_blast_options, secondBlastOptions);
        task.setParameter(RrnaScanTask.PARAM_min_5S_length, min5sLength);
        task.setParameter(RrnaScanTask.PARAM_min_16S_length, min16sLength);
        task.setParameter(RrnaScanTask.PARAM_min_18S_length, min18sLength);
        task.setParameter(RrnaScanTask.PARAM_min_23S_length, min23sLength);
        return task;
    }

    /*
     * 1. First section is ncbi-blastn.rRNA_prefilter.config, which has these substeps:
     *
     *     1.1 <blast executable>
     *
     *     EXPECT=0.1
     *     FILTER=T
     *     DATABASE_MATCHES=1
     *     DESCRIPTIONS=1
     *     OTHER_OPTS=-z 3000000000 -W 9
     *     DATABASE_PATH=$CAMERA_RRNA_FINDER_DB
     *
     *     full cmd line: -e 0.1 -F T -b 1 -v 1 -z 3000000000 -W 9
     *
     *     -p blastn
     *     -i <input file>
     *     -d <database>
     *     -e EXPECT
     *     -F FILTER
     *     -b DATABASE_MATCHES
     *     -v DESCRIPTIONS
     *     <OTHER_OPTS>
     *
     *  Next step is wu-blast2btab
     *
     *     1.2 <'wu-blast2btab'>
     *
     *     --input <blast output>
     *     --output <btab file>
     *
     *  Next step is btab to bsml
     *
     *     1.3 <'blastbtab2bsml'>
     *
     *     --btab_file <input btab file>
     *     --output <output bsml file>
     *     --pvalue EXPECT
     *     --class dna
     *     --analysis_id ncbi-blastn_analysis
     *     --query_file_path <input file to blast from above>
     *
     * 2. Next subsection is component_camera_filter_blast.rRNA_prefilter which has these steps:
     *
     *     OUTPUT_ALIGNED=1
     *     OUTPUT_UNALIGNED=1
     *     INPUT_FILE_LIST=<.bsml files>
     *     OUTPUT_TOKEN=rRNA_prefilter
     *     FSA_ALIGNED_OUTPUT_LIST=*.aligned.fsa.list
     *     FSA_UNALIGNED_OUTPUT_LIST=*.unaligned.fsa.list
     *     COMPONENT_NAME=camera_filter_blast
     *
     *     2.1 <'camera_bsml_to_fasta_filter_on_alignment'>
     *
     *     --input_file <bsml file>
     *     --output_dir <a working dir for this particular file>
     *     --compress 0
     *     --aligned OUTPUT_ALIGNED
     *     --unaligned OUTPUT_UNALIGNED
     *
     *  3. Next subsection is component_camera_rrna_finder
     *
     *     EXPECT=1e-4
     *     FILTER=m L
     *     DATABASE_MATCHES=1500
     *     DESCRIPTIONS=1500
     *     MIN_5S_LENGTH=25
     *     MIN_16S_LENGTH=250
     *     MIN_18S_LENGTH=250
     *     MIN_23S_LENGTH=250
     *     INPUT_FILE_LIST=<*.aligned.fsa.list>
     *     DATABASE_PATH=CAMERA_RRNA_FINDER_DB
     *     COMPONENT_NAME=camera_rrna_finder
     *     BSML_OUTPUT_LIST=*.bsml.list
     *     RAW_OUTPUT_LIST=*.raw.list
     *     FSA_OUTPUT_LIST=*.fsa.list
     *     16S_FSA_OUTPUT_LIST=*.16S.fsa.list
     *     18S_FSA_OUTPUT_LIST=*.18S.fsa.list
     *     SSU_FSA_OUTPUT_LIST=*.SSU.fsa.list
     *     5S_FSA_OUTPUT_LIST=*.5S.fsa.list
     *     23S_FSA_OUTPUT_LIST=*.23S.fsa.list
     *     COMPONENT_NAME=camera_rrna_finder
     *
     *     3.1 <blast exec>
     *
     *     -i <input file>
     *     -p blastn
     *     -d DATABASE_PATH
     *     -e EXPECT
     *     -F FILTER
     *     -b DATABASE_MATCHES
     *     -v DESCRIPTIONS
     *     -q -5 -r 4 -X 1500 -z 3000000000 -W 9 -U T
     *     <stdout to *.blastn.raw>
     *
     *     3.2 <'blast_raw_to_table'>
     *
     *     <stdin < *.blastn.raw>
     *     <stdout > *.table.raw>
     *
     *     3.3 <'blast_table_interval_collapse'>
     *
     *     <stdin < *.table.raw>
     *     <stdout > *.raw>
     *
     *     3.4 <'camera_rrna_finder2bsml'>
     *
     *     --input <*.raw>
     *     --query_file_path <input file from blast>
     *     --output <*.bsml>
     *
     *    3.5 <'camera_rrna_sequence_extract'>
     *
     *    --input <*.raw>
     *    --query_file_path <input file from blast>
     *    --ergatis_id 1
     *    --output_dir <output working subdir>
     *    --min_5s MIN_5S_LENGTH
     *    --min_16s MIN_16S_LENGTH
     *    --min_18s MIN_18S_LENGTH
     *    --min_23s MIN_23S_LENGTH
     *
     * 4. Next subsection is component_mask_by_analysis.soft_mask_rRNA
     *
     *    MASK_CHARACTER=X
     *    ANALYSIS_TYPES=camera_rrna_finder
     *    FEATURE_CLASSES=<blank>
     *    SOFTMASK=1
     *    RANDOM=0
     *    INPUT_FILE_LIST=<camera_rrna_finder.bsml.list>
     *    COMPONENT_NAME=mask_by_analysis
     *
     *    4.1 <'mask_by_analysis'>
     *
     *    --input <*.bsml>
     *    --output <working dir>
     *    --output_prefix p
     *    --analysis_types ANALYSIS_TYPES
     *    --feature_types FEATURE_CLASSES
     *    --mask_char MASK_CHARACTER
     *    --random RANDOM
     *    --softmask SOFTMASK
     *
     */

    protected void init(IProcessData processData) throws Exception {
        rrnaScanTask = getRrnaScanTask(processData);
        task = rrnaScanTask;
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        rrnaScanResultNode = createResultFileNode();
        resultFileNode = rrnaScanResultNode;
        super.init(processData);
        Object totalBaseCountObj = processData.getItem("TOTAL_BASE_COUNT");
        if (totalBaseCountObj != null) {
            totalBaseCount = new Integer((String) totalBaseCountObj);
        }
        createIdRepository();
        // First check if we have been assigned a specific file to process. If so, then we will not
        // worry about its size, assuming this has already been configured.
        inputFiles = new ArrayList<File>();
        outputFiles = new ArrayList<File>();
        File inputFile = getMgInputFile(processData);
        if (inputFile == null) {
            // Since we have not been assigned a file, we will get the input node from the task
            Long inputFastaNodeId = new Long(rrnaScanTask.getParameter(RrnaScanTask.PARAM_input_fasta_node_id));
            FastaFileNode inputFastaNode = (FastaFileNode) computeDAO.getNodeById(inputFastaNodeId);
            inputFile = new File(inputFastaNode.getFastaFilePath());
        }
        inputFiles = SimpleMultiFastaSplitterService.splitInputFileToList(processData, inputFile, DEFAULT_ENTRIES_PER_EXEC, logger);
        makeLocalDatabaseCopy();
        logger.info(this.getClass().getName() + " rrnaScanTaskId=" + task.getObjectId() + " init() end");
    }

    private void makeLocalDatabaseCopy() throws Exception {
        File originalRrnaDbFile = new File(databasePath);
        String dbCopyCmd = "cp -a " + originalRrnaDbFile.getAbsolutePath() + "* " + resultFileNode.getDirectoryPath();
        SystemCall sc = new SystemCall(logger);
        int ev = sc.emulateCommandLine(dbCopyCmd, true);
        if (ev != 0) {
            throw new Exception("System cmd returned non-zero exit state=" + dbCopyCmd);
        }
    }

    protected String getGridServicePrefixName() {
        return "rrnaScan";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    private RrnaScanTask getRrnaScanTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Object possibleTask = processData.getItem(RRNA_SCAN_TASK);
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
                return (RrnaScanTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getRrnaScanTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private RrnaScanResultNode createResultFileNode() throws ServiceException, IOException, DaoException {
        RrnaScanResultNode resultFileNode;

        // Check if we already have a result node for this task
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof RrnaScanResultNode) {
                return (RrnaScanResultNode) node;
            }
        }

        // Create new node
        resultFileNode = new RrnaScanResultNode(task.getOwner(), task,
                "RrnaScanResultNode", "RrnaScanResultNode for task " + task.getObjectId(),
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
                logger.info("RrnaScanService using input file=" + inputFile.getAbsolutePath());
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
        RrnaScanTask rrnaScanTask = (RrnaScanTask) task;

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(rrnaScanTask, writer);

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

    private String getEvalueFromBlastOptions(String optionsString) throws Exception {
        String[] components = optionsString.split("\\s+");
        for (int i = 0; i < components.length; i++) {
            if (components[i].equals("-e")) {
                return components[i + 1];
            }
        }
        throw new Exception("Could not parse evalue from blast options string=" + optionsString);
    }

    private void createShellScript(RrnaScanTask rrnaScanTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            File originalRrnaDbFile = new File(databasePath);
            File rrnaDbFile = new File(resultFileNode.getDirectoryPath() + "/" + originalRrnaDbFile.getName());
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");

            script.append("unset PERL5LIB\n");
            script.append("export PERL5LIB=").append(MetaGenoPerlConfig.PERL5LIB).append("\n");
            script.append("export PERL_MOD_DIR=").append(MetaGenoPerlConfig.PERL_MOD_DIR).append("\n");

            // 1.1 First blast
            String firstBlastCmd = formatFirstBlastCmd("\"$INPUTFILE\"", "\"$OUTPUTFILE\"", rrnaDbFile);
            script.append(firstBlastCmd).append("\n");

            // 1.2 blast2btab
            String blast2btabCmd = formatBlast2BtabCmd("\"$OUTPUTFILE\"");
            script.append(blast2btabCmd).append("\n");

            // 1.3 btab2bsml
            String btab2bsmlCmd = formatBtab2BsmlCmd("\"$INPUTFILE\"", "\"$OUTPUTFILE\"");
            script.append(btab2bsmlCmd).append("\n");

            // 2.1 bsml2fasta
            String bsml2fastaCmdStr = formatBsml2FastaCmd("\"$INPUTFILE\"", "\"$OUTPUTFILE\"");
            script.append(bsml2fastaCmdStr).append("\n");

            // 3.1 Second blast
            String secondBlastCmd = formatSecondBlastCmd("\"$INPUTFILE\"", "\"$OUTPUTFILE\"", rrnaDbFile);
            script.append(secondBlastCmd).append("\n");

            // 3.2 blast2table
            String blast2tableCmdStr = formatBlast2TableCmd("\"$OUTPUTFILE\"");
            script.append(blast2tableCmdStr).append("\n");

            // 3.3 tableInterval
            String blastTableIntervalCmdStr = formatTableIntervalCmd("\"$OUTPUTFILE\"");
            script.append(blastTableIntervalCmdStr).append("\n");

            // 3.4 finder2bsml
            String finder2bsmlCmdStr = formatFinder2BsmlCmd("\"$OUTPUTFILE\"");
            script.append(finder2bsmlCmdStr).append("\n");

            // 3.5 extract
            String extractCmdStr = formatExtractCmd("\"$OUTPUTFILE\"");
            script.append(extractCmdStr).append("\n");

            // 4.1 mask
            String maskCmdStr = formatMaskCmd("\"$OUTPUTFILE\"");
            script.append(maskCmdStr).append("\n");

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File maskedFile = new File(resultFileNode.getFilePathByTag(RrnaScanResultNode.TAG_POSTMASK_FASTA_OUTPUT));
            processData.putItem("MG_INPUT_ARRAY", maskedFile);
            logger.info(this.getClass().getName() + " rrnaScanTaskId=" + task.getObjectId() + " execute() finish");

            // Extract files
            File extractFile = new File(resultFileNode.getFilePathByTag(RrnaScanResultNode.TAG_RRNA_FASTA_OUTPUT));
            List<File> extractFiles = new ArrayList<File>();
            for (File file : outputFiles) {
                File extractDir = new File(file.getAbsolutePath() + ".extractDir");
                File[] extractFastaFiles = extractDir.listFiles();
                extractFiles.addAll(Arrays.asList(extractFastaFiles));
            }
            FileUtil.concatFilesUsingSystemCall(extractFiles, extractFile);
            // Mask files
            File maskFile = new File(resultFileNode.getFilePathByTag(RrnaScanResultNode.TAG_POSTMASK_FASTA_OUTPUT));
            List<File> maskFiles = new ArrayList<File>();
            for (File file : outputFiles) {
                // First, the unaligned portion
                File bsml2fastaDir = new File(file.getAbsolutePath() + ".bsml2fastaDir");
                File[] bfFiles = bsml2fastaDir.listFiles();
                for (File bf : bfFiles) {
                    if (bf.getName().endsWith(".unaligned.fsa")) {
                        maskFiles.add(bf);
                    }
                }
                // Next, the aligned portion
                File maskDir = new File(file.getAbsolutePath() + ".maskDir");
                File[] maskFastaFiles = maskDir.listFiles();
                for (File mf : maskFastaFiles) {
                    if (mf.getName().endsWith(".fsa")) {
                        maskFiles.add(mf);
                    }
                }
            }
            FileUtil.concatFilesUsingSystemCall(maskFiles, maskFile);
            deleteLocalDatabase();
            //deleteIntermediateFiles();
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

//    private void deleteIntermediateFiles() throws Exception {
//        File topDir = new File(resultFileNode.getDirectoryPath());
//        File[] level_1_files = topDir.listFiles();
//        for (File f : level_1_files) {
//            if (f.isDirectory()) {
//                File[] level_2_files = f.listFiles();
//                for (File f2 : level_2_files) {
//                    f2.delete();
//                }
//                f.delete();
//            }
//            else {
//                if (f.getName().startsWith(resultFilename + ".")) {
//                    f.delete();
//                }
//            }
//        }
//    }
//
    private void deleteLocalDatabase() throws Exception {
        File originalRrnaDbFile = new File(databasePath);
        File rrnaDbFile = new File(resultFileNode.getDirectoryPath() + "/" + originalRrnaDbFile.getName());
        String dbCleanCmd = "rm " + rrnaDbFile.getAbsolutePath() + "*";
        SystemCall sc = new SystemCall(logger);
        int ev = sc.emulateCommandLine(dbCleanCmd, true);
        if (ev != 0) {
            throw new Exception("System cmd returned non-zero exit state=" + dbCleanCmd);
        }
    }

    private String formatFirstBlastCmd(String inputFilepath, String outputFilepath, File rrnaDbFile) {
        return blastCmd +
                " -p blastn " + rrnaScanTask.getParameter(RrnaScanTask.PARAM_initial_blast_options) +
                " -i " + inputFilepath +
                " -d " + rrnaDbFile.getAbsolutePath() +
                " -o " + outputFilepath + ".blast_out";
    }

    private String formatBlast2BtabCmd(String outputFilepath) {
        return MetaGenoPerlConfig.PERL_EXEC + " " +
                MetaGenoPerlConfig.PERL_BIN_DIR + "/" + blast2btabCmd +
                " --input " + outputFilepath + ".blast_out" +
                " --output " + outputFilepath + ".blast_out.btab";
    }

    /*    blastbtab2bsml
     *     --btab_file <input btab file>
     *     --output <output bsml file>
     *     --pvalue EXPECT
     *     --class dna
     *     --analysis_id ncbi-blastn_analysis
     *     --query_file_path <input file to blast from above>
     */
    private String formatBtab2BsmlCmd(String inputFilepath, String outputFilepath) throws Exception {
        String pvalue = getEvalueFromBlastOptions(rrnaScanTask.getParameter(RrnaScanTask.PARAM_initial_blast_options));
        return MetaGenoPerlConfig.PERL_EXEC + " " +
                MetaGenoPerlConfig.PERL_BIN_DIR + "/" + btab2bsmlCmd +
                " --btab_file " + outputFilepath + ".blast_out.btab" +
                " --output " + outputFilepath + ".blast_out.btab.bsml" +
                " --pvalue " + pvalue +
                " --class dna " +
                " --analysis_id ncbi-blastn_analysis " +
                " --query_file_path " + inputFilepath;
    }

    /*
     *     2.1 <'camera_bsml_to_fasta_filter_on_alignment'>
     *
     *     --input_file <bsml file>
     *     --output_dir <a working dir for this particular file>
     *     --compress 0
     *     --aligned OUTPUT_ALIGNED
     *     --unaligned OUTPUT_UNALIGNED
     */
    private String formatBsml2FastaCmd(String inputFilepath, String outputFilepath) throws Exception {
        String workingDir = outputFilepath + ".bsml2fastaDir";
        String workingDirCmd = "mkdir " + workingDir + "\n";
        String fastaCmd = MetaGenoPerlConfig.PERL_EXEC + " " +
                MetaGenoPerlConfig.PERL_BIN_DIR + "/" + bsml2fastaCmd +
                " --input_file " + outputFilepath + ".blast_out.btab.bsml" +
                " --output_dir " + workingDir +
                " --compress 0" +
                " --aligned 1" +
                " --unaligned 1";
        return workingDirCmd + fastaCmd;
    }

    private String formatSecondBlastCmd(String inputFilepath, String outputFilepath, File rrnaDbFile) {
        String renameCmd = "cat " + outputFilepath + ".bsml2fastaDir/*.aligned.fsa > " + outputFilepath + ".bsml2fastaDir/aligned.fasta" + "\n";
        String secondBlastCmd = blastCmd +
                " -p blastn " +
                rrnaScanTask.getParameter(RrnaScanTask.PARAM_second_blast_options) +
                " -i " +
                outputFilepath + ".bsml2fastaDir/aligned.fasta" +
                " -d " +
                rrnaDbFile.getAbsolutePath() +
                " -o " +
                outputFilepath +
                ".blast_out2";
        return renameCmd + secondBlastCmd;
    }

    private String formatBlast2TableCmd(String outputFilepath) {
        return "cat " + outputFilepath + ".blast_out2 | " + MetaGenoPerlConfig.PERL_BIN_DIR + "/" + blast2tableCmd +
                " > " + outputFilepath + ".blast_out2.table";
    }

    private String formatTableIntervalCmd(String outputFilepath) {
        return "cat " + outputFilepath + ".blast_out2.table | " + MetaGenoPerlConfig.PERL_BIN_DIR + "/" + blastTableIntervalCmd +
                " > " + outputFilepath + ".blast_out2.interval";
    }

    /*
     *     --input <*.raw>
     *     --query_file_path <input file from blast>
     *     --output <*.bsml>
     */
    private String formatFinder2BsmlCmd(String outputFilepath) {
        return MetaGenoPerlConfig.PERL_EXEC + " " +
                MetaGenoPerlConfig.PERL_BIN_DIR + "/" + finder2bsmlCmd +
                " --input " + outputFilepath + ".blast_out2.interval" +
                " --query_file_path " + outputFilepath + ".bsml2fastaDir/aligned.fasta" +
                " --output " + outputFilepath + ".blast_out2.bsml";
    }

    /*
     *    3.5 <'camera_rrna_sequence_extract'>
     *
     *    --input <*.raw>
     *    --query_file_path <input file from blast>
     *    --ergatis_id 1
     *    --output_dir <output working subdir>
     *    --min_5s MIN_5S_LENGTH
     *    --min_16s MIN_16S_LENGTH
     *    --min_18s MIN_18S_LENGTH
     *    --min_23s MIN_23S_LENGTH
     */
    private String formatExtractCmd(String outputFilepath) {
        String workingDir = outputFilepath + ".extractDir";
        String workingDirCmd = "mkdir " + workingDir + "\n";
        String cmd = MetaGenoPerlConfig.PERL_EXEC + " " +
                MetaGenoPerlConfig.PERL_BIN_DIR + "/" + extractCmd +
                " --input " + outputFilepath + ".blast_out2.interval" +
                " --query_file_path " + outputFilepath + ".bsml2fastaDir/aligned.fasta" +
                " --ergatis_id 1" +
                " --output_dir " + workingDir +
                " --min_5s " + rrnaScanTask.getParameter(RrnaScanTask.PARAM_min_5S_length) +
                " --min_16s " + rrnaScanTask.getParameter(RrnaScanTask.PARAM_min_16S_length) +
                " --min_18s " + rrnaScanTask.getParameter(RrnaScanTask.PARAM_min_18S_length) +
                " --min_23s " + rrnaScanTask.getParameter(RrnaScanTask.PARAM_min_23S_length);
        return workingDirCmd + cmd;
    }

    /*
     *    MASK_CHARACTER=X
     *    ANALYSIS_TYPES=camera_rrna_finder
     *    FEATURE_CLASSES=<blank>
     *    SOFTMASK=1
     *    RANDOM=0
     *    INPUT_FILE_LIST=<camera_rrna_finder.bsml.list>
     *    COMPONENT_NAME=mask_by_analysis
     *
     *    4.1 <'mask_by_analysis'>
     *
     *    --input <*.bsml>
     *    --output <working dir>
     *    --output_prefix p
     *    --analysis_types ANALYSIS_TYPES
     *    --feature_types FEATURE_CLASSES
     *    --mask_char MASK_CHARACTER
     *    --random RANDOM
     *    --softmask SOFTMASK
     */
    private String formatMaskCmd(String outputFilepath) {
        String workingDir = outputFilepath + ".maskDir";
        String workingDirCmd = "mkdir " + workingDir + "\n";
        String cmd = MetaGenoPerlConfig.PERL_EXEC + " " +
                MetaGenoPerlConfig.PERL_BIN_DIR + "/" + maskCmd +
                " --input " + outputFilepath + ".blast_out2.bsml" +
                " --output " + workingDir +
                " --output_prefix p" +
                " --analysis_types camera_rrna_finder" +
                //      " --features_types " +      NOT USED
                " --mask_char X" +
                " --random 0" +
                " --softmask 1";
        return workingDirCmd + cmd;
    }

}

