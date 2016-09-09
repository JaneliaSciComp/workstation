
package org.janelia.it.jacs.compute.service.metageno.orf;

import org.apache.log4j.Logger;
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
import org.janelia.it.jacs.compute.service.metageno.MetaGenoPerlConfig;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.metageno.SimpleOrfCallerTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.metageno.SimpleOrfCallerResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 5, 2009
 * Time: 9:27:08 AM
 * From jacs.properties
 * # Simple Orf Caller Properties
 SimpleOrfCaller.DefaultFastaEntriesPerExec=10000
 SimpleOrfCaller.Queue=-l default
 SimpleOrfCaller.Cmd=open_reading_frames.pl
 SimpleOrfCaller.TranslationTable=11
 SimpleOrfCaller.BeginningAsStart=1
 SimpleOrfCaller.EndAsStop=1
 SimpleOrfCaller.AssumeStops=0
 SimpleOrfCaller.FullOrfs=0
 SimpleOrfCaller.MinOrfSize=180
 SimpleOrfCaller.MaxOrfSize=0
 SimpleOrfCaller.MinUnmaskedSize=150
 SimpleOrfCaller.Frames=0
 SimpleOrfCaller.ForceMethionine=0
 SimpleOrfCaller.HeaderAdditions=
 */
public class SimpleOrfCallerService extends SubmitDrmaaJobService {
    private static Logger logger = Logger.getLogger(SimpleOrfCallerService.class);

    /*
        SCRIPT DEPENDENCIES

        SimpleOrfCaller.Cmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/jboss-test/bin/open_reading_frames.pl
            use lib '/usr/local/annotation/CAMERA/lib';
            use strict;
            use warnings;
            use Getopt::Long qw(:config no_ignore_case no_auto_abbrev pass_through);
            use Pod::Usage;
            use File::Basename qw(basename fileparse);
            #use GUID;
            use Ergatis::IdGenerator;

        MODULE SUMMARY
            Ergatis

     */

    // IService constants
    public static String SIMPLE_ORF_CALLER_TASK = "SIMPLE_ORF_CALLER_TASK";
    public static String SIMPLE_ORF_NT_OUTPUT_FILE = "SIMPLE_ORF_NT_OUTPUT_FILE";
    public static String SIMPLE_ORF_AA_OUTPUT_FILE = "SIMPLE_ORF_AA_OUTPUT_FILE";

    public static final int DEFAULT_ENTRIES_PER_EXEC = SystemConfigurationProperties.getInt("SimpleOrfCaller.DefaultFastaEntriesPerExec");

    private static final String resultFilename = SimpleOrfCallerResultNode.BASE_OUTPUT_FILENAME;

    private static final String simpleOrfCallerCmd = SystemConfigurationProperties.getString("SimpleOrfCaller.Cmd");


    private static final String queueName = SystemConfigurationProperties.getString("SimpleOrfCaller.Queue");

    private List<File> inputFiles;
    private List<File> outputFiles;
    SimpleOrfCallerTask simpleOrfCallerTask;
    SimpleOrfCallerResultNode simpleOrfCallerResultNode;
    private String sessionName;

    public static SimpleOrfCallerTask createDefaultTask() {
        String translationTable = SystemConfigurationProperties.getString("SimpleOrfCaller.TranslationTable");
        String beginningAsStart = SystemConfigurationProperties.getString("SimpleOrfCaller.BeginningAsStart");
        String endAsStop = SystemConfigurationProperties.getString("SimpleOrfCaller.EndAsStop");
        String assumeStops = SystemConfigurationProperties.getString("SimpleOrfCaller.AssumeStops");
        String fullOrfs = SystemConfigurationProperties.getString("SimpleOrfCaller.FullOrfs");
        String minOrfSize = SystemConfigurationProperties.getString("SimpleOrfCaller.MinOrfSize");
        String maxOrfSize = SystemConfigurationProperties.getString("SimpleOrfCaller.MaxOrfSize");
        String minUnmaskedSize = SystemConfigurationProperties.getString("SimpleOrfCaller.MinUnmaskedSize");
        String frames = SystemConfigurationProperties.getString("SimpleOrfCaller.Frames");
        String forceMethionine = SystemConfigurationProperties.getString("SimpleOrfCaller.ForceMethionine");
        String headerAdditions = SystemConfigurationProperties.getString("SimpleOrfCaller.HeaderAdditions");
        SimpleOrfCallerTask task = new SimpleOrfCallerTask();
        task.setParameter(SimpleOrfCallerTask.PARAM_translation_table, translationTable);
        task.setParameter(SimpleOrfCallerTask.PARAM_beginning_as_start, beginningAsStart);
        task.setParameter(SimpleOrfCallerTask.PARAM_end_as_stop, endAsStop);
        task.setParameter(SimpleOrfCallerTask.PARAM_assume_stops, assumeStops);
        task.setParameter(SimpleOrfCallerTask.PARAM_full_orfs, fullOrfs);
        task.setParameter(SimpleOrfCallerTask.PARAM_min_orf_size, minOrfSize);
        task.setParameter(SimpleOrfCallerTask.PARAM_max_orf_size, maxOrfSize);
        task.setParameter(SimpleOrfCallerTask.PARAM_min_unmasked_size, minUnmaskedSize);
        task.setParameter(SimpleOrfCallerTask.PARAM_frames, frames);
        task.setParameter(SimpleOrfCallerTask.PARAM_force_methionine, forceMethionine);
        task.setParameter(SimpleOrfCallerTask.PARAM_header_additions, headerAdditions);
        return task;
    }

    protected void init(IProcessData processData) throws Exception {
        simpleOrfCallerTask = getSimpleOrfCallerTask(processData);
        task = simpleOrfCallerTask;
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        simpleOrfCallerResultNode = createResultFileNode();
        resultFileNode = simpleOrfCallerResultNode;
        super.init(processData);
        // First check if we have been assigned a specific file to process. If so, then we will not
        // worry about its size, assuming this has already been configured.
        inputFiles = new ArrayList<File>();
        outputFiles = new ArrayList<File>();
        File inputFile = getMgInputFile(processData);
        if (inputFile == null) {
            // Since we have not been assigned a file, we will get the input node from the task
            Long inputFastaNodeId = new Long(simpleOrfCallerTask.getParameter(SimpleOrfCallerTask.PARAM_input_fasta_node_id));
            FastaFileNode inputFastaNode = (FastaFileNode) computeDAO.getNodeById(inputFastaNodeId);
            inputFile = new File(inputFastaNode.getFastaFilePath());
        }
        inputFiles = SimpleMultiFastaSplitterService.splitInputFileToList(processData, inputFile, DEFAULT_ENTRIES_PER_EXEC, logger);
        logger.info(this.getClass().getName() + " simpleOrfCallerTaskId=" + task.getObjectId() + " init() end");
    }

    protected String getGridServicePrefixName() {
        return "simpleOrfCaller";
    }

    protected String getConfigPrefix() {
        return getGridServicePrefixName() + "Configuration.";
    }

    private SimpleOrfCallerTask getSimpleOrfCallerTask(IProcessData processData) {
        try {
            if (computeDAO == null)
                computeDAO = new ComputeDAO(logger);
            Long taskId = null;
            Object possibleTask = processData.getItem(SIMPLE_ORF_CALLER_TASK);
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
                return (SimpleOrfCallerTask) computeDAO.getTaskById(taskId);
            }
            return null;
        }
        catch (Exception e) {
            logger.error("Received exception in getSimpleOrfCallerTask: " + e.getMessage());
            return null; // assume not in processData
        }
    }

    private SimpleOrfCallerResultNode createResultFileNode() throws ServiceException, IOException, DaoException {
        SimpleOrfCallerResultNode resultFileNode;

        // Check if we already have a result node for this task
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof SimpleOrfCallerResultNode) {
                return (SimpleOrfCallerResultNode) node;
            }
        }

        // Create new node
        resultFileNode = new SimpleOrfCallerResultNode(task.getOwner(), task,
                "SimpleOrfCallerResultNode", "SimpleOrfCallerResultNode for task " + task.getObjectId(),
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
                logger.info("SimpleOrfCallerService using input file=" + inputFile.getAbsolutePath());
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

        // Job template expects configuration.[intValue]  ... configuration.q[indx].[intValue] format didn't work
        createShellScript(simpleOrfCallerTask, writer);

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

    private void createShellScript(SimpleOrfCallerTask simpleOrfCallerTask, FileWriter writer)
            throws IOException, ParameterException {
        try {
            StringBuffer script = new StringBuffer();
            script.append("read INPUTFILE\n");
            script.append("read OUTPUTFILE\n");

            script.append("unset PERL5LIB\n");
            script.append("export PERL5LIB=").append(MetaGenoPerlConfig.PERL5LIB).append("\n");
            script.append("export PERL_MOD_DIR=").append(MetaGenoPerlConfig.PERL_MOD_DIR).append("\n");

            /*
            * The script for the simple orf caller is:
            *
            *     "open_reading_frames.pl"
            *
            *     --input_file <input fasta file>
            *     --translation_table <translationTable>
            *     --output_dir <working dir>
            *     --beginning_as_start <beginning as start>
            *     --end_as_stop <end as stop>
            *     --assume_stops <assume stops>
            *     --full_orfs <full orfs>
            *     --min_orf_size <min orf size>
            *     --max_orf_size <max orf size>
            *     --min_unmasked_size <min unmasked size>
            *     --frames <frames>
            *     --force_methionine <force methionine>
            *     --header_additions <header additions>
            *
            */

            String workingDir = "\"$OUTPUTFILE\"" + ".orfDir";
            String workingDirCmd = "mkdir " + workingDir + "\n";

            String cmd = MetaGenoPerlConfig.PERL_EXEC + " " +
                    MetaGenoPerlConfig.PERL_BIN_DIR + "/" + simpleOrfCallerCmd +
                    " --gzip_output 0" +
                    " --input_file \"$INPUTFILE\"" +
                    " --translation_table " + simpleOrfCallerTask.getParameter(SimpleOrfCallerTask.PARAM_translation_table) +
                    " --output_dir " + workingDir +
                    " --beginning_as_start " + simpleOrfCallerTask.getParameter(SimpleOrfCallerTask.PARAM_beginning_as_start) +
                    " --end_as_stop " + simpleOrfCallerTask.getParameter(SimpleOrfCallerTask.PARAM_end_as_stop) +
                    " --assume_stops " + simpleOrfCallerTask.getParameter(SimpleOrfCallerTask.PARAM_assume_stops) +
                    " --full_orfs " + simpleOrfCallerTask.getParameter(SimpleOrfCallerTask.PARAM_full_orfs) +
                    " --min_orf_size " + simpleOrfCallerTask.getParameter(SimpleOrfCallerTask.PARAM_min_orf_size) +
                    " --max_orf_size " + simpleOrfCallerTask.getParameter(SimpleOrfCallerTask.PARAM_max_orf_size) +
                    " --min_unmasked_size " + simpleOrfCallerTask.getParameter(SimpleOrfCallerTask.PARAM_min_unmasked_size) +
                    " --frames " + simpleOrfCallerTask.getParameter(SimpleOrfCallerTask.PARAM_frames) +
                    " --force_methionine " + simpleOrfCallerTask.getParameter(SimpleOrfCallerTask.PARAM_force_methionine) +
                    " --header_additions " + simpleOrfCallerTask.getParameter(SimpleOrfCallerTask.PARAM_header_additions) +
                    "\n";

            String mvNtCmd = "cat " + workingDir + "/*.fna >> \"$OUTPUTFILE\".nt.fasta\n";
            String mvAaCmd = "cat " + workingDir + "/*.faa >> \"$OUTPUTFILE\".aa.fasta\n";
            String delNtCmd = "rm " + workingDir + "/*.fna\n";
            String delAaCmd = "rm " + workingDir + "/*.faa\n";
            String rmdirCmd = "rmdir " + workingDir + "\n";

            script.append(workingDirCmd);
            script.append(cmd);
            script.append(mvNtCmd);
            script.append(mvAaCmd);
            script.append(delNtCmd);
            script.append(delAaCmd);
            script.append(rmdirCmd);

            writer.write(script.toString());
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new IOException(e);
        }
    }

    public void postProcess() throws MissingDataException {
        try {
            File ntFile = new File(resultFileNode.getFilePathByTag(SimpleOrfCallerResultNode.TAG_NUCLEOTIDE_ORF_FASTA_OUTPUT));
            File aaFile = new File(resultFileNode.getFilePathByTag(SimpleOrfCallerResultNode.TAG_PEPTIDE_ORF_FASTA_OUTPUT));
            processData.putItem(SIMPLE_ORF_NT_OUTPUT_FILE, ntFile);
            processData.putItem(SIMPLE_ORF_AA_OUTPUT_FILE, aaFile);
            logger.info(this.getClass().getName() + " simpleOrfCallerTaskId=" + task.getObjectId() + " execute() finish");

            // First, create consolidated result files
            List<File> ntFiles = new ArrayList<File>();
            List<File> aaFiles = new ArrayList<File>();
            for (File file : outputFiles) {
                ntFiles.add(new File(file.getAbsolutePath() + ".nt.fasta"));
                aaFiles.add(new File(file.getAbsolutePath() + ".aa.fasta"));
            }
            File ntResultFile = new File(resultFileNode.getFilePathByTag(SimpleOrfCallerResultNode.TAG_NUCLEOTIDE_ORF_FASTA_OUTPUT));
            File aaResultFile = new File(resultFileNode.getFilePathByTag(SimpleOrfCallerResultNode.TAG_PEPTIDE_ORF_FASTA_OUTPUT));
            FileUtil.concatFilesUsingSystemCall(ntFiles, ntResultFile);
            FileUtil.concatFilesUsingSystemCall(aaFiles, aaResultFile);
            // Next, clear incremental fasta files
            for (File f : ntFiles) {
                f.delete();
            }
            for (File f : aaFiles) {
                f.delete();
            }
        }
        catch (Exception e) {
            logger.error(e, e);
            throw new MissingDataException(e.getMessage());
        }
    }

}
