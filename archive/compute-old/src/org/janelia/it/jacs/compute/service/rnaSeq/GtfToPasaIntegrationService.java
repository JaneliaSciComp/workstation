
package org.janelia.it.jacs.compute.service.rnaSeq;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.metageno.SimpleGridJobRunner;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.rnaSeq.GtfToPasaIntegrationTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.GtfFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.pasa.PasaResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 12, 2010
 * Time: 11:02:58 AM
 * From jacs.properties
 * # Pasa
 Pasa.ScriptDir=/usr/local/devel/ANNOTATION/PASA2/scripts
 Pasa.Gtf2FastaQueue=-l fast
 Pasa.Gtf2FastaScript=gtf2fasta.pl
 Pasa.Gtf2GffScript=create_gff_for_pasa_upload.pl
 Pasa.PipelineScript=Launch_PASA_pipeline.pl
 Pasa.PipelineScriptQueue=-l medium
 Pasa.Default.Align.MaxIntronLength=10000
 Pasa.Default.Align.MinPercentAligned=90
 Pasa.Default.Align.MinAvgPerId=95
 Pasa.Default.AnnotCompare.MinPercentOverlap=50
 Pasa.Default.AnnotCompare.MinPercentProtCoding=40
 Pasa.Default.AnnotCompare.MinPercidProtCompare=70
 Pasa.Default.AnnotCompare.MinPercentLengthFlCompare=70
 Pasa.Default.AnnotCompare.MinPercentAlignLength=70
 Pasa.Default.AnnotCompare.MinPercentOverlapGeneReplace=80
 Pasa.Default.AnnotCompare.MaxUtrExons=2
 Pasa.Default.GeneticCode=universal
 Pasa.AddDbToPasaAdminDbScriptFullPath=/usr/local/devel/ANNOTATION/PASA2/rnaseq_integration/add_db_to_pasa_admin_db.pl
 */
public class GtfToPasaIntegrationService implements IService {

// Example command lines
// gtf2fasta gtf -> est.fasta
// gtf2gff -> structure.gff
// ./Launch_PASA_pipeline.pl -c configuration_file -C -R -g reference.fasta -e est.fasta --IMPORT_CUSTOM_ALIGNMENTS_GFF3  structure.gff

    String PASA_SCRIPT_DIR = SystemConfigurationProperties.getString("Pasa.ScriptDir");
    String PASA_GTF2FASTA_SCRIPT = SystemConfigurationProperties.getString("Pasa.Gtf2FastaScript");
    String PASA_GTF2GFF_SCRIPT = SystemConfigurationProperties.getString("Pasa.Gtf2GffScript");
    String PASA_PIPELINE_SCRIPT = SystemConfigurationProperties.getString("Pasa.PipelineScript");
    String PASA_GTF_TO_FASTA_QUEUE = SystemConfigurationProperties.getString("Pasa.Gtf2FastaQueue");
    String PASA_PIPELINE_QUEUE = SystemConfigurationProperties.getString("Pasa.PipelineScriptQueue");
    String PASA_ADD_TO_ADMIN_DB_FULLPATH_SCRIPT = SystemConfigurationProperties.getString("Pasa.AddDbToPasaAdminDbScriptFullPath");

    String PASA_DEFAULT_ALIGN_MAX_INTRON_LENGTH = SystemConfigurationProperties.getString("Pasa.Default.Align.MaxIntronLength");
    String PASA_DEFAULT_ALIGN_MAX_PERCENT_ALIGNED = SystemConfigurationProperties.getString("Pasa.Default.Align.MinPercentAligned");
    String PASA_DEFAULT_ALIGN_MIN_AVG_PER_ID = SystemConfigurationProperties.getString("Pasa.Default.Align.MinAvgPerId");
    String PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCENT_OVERLAP = SystemConfigurationProperties.getString("Pasa.Default.AnnotCompare.MinPercentOverlap");
    String PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCENT_PROT_CODING = SystemConfigurationProperties.getString("Pasa.Default.AnnotCompare.MinPercentProtCoding");
    String PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCID_PROT_COMPARE = SystemConfigurationProperties.getString("Pasa.Default.AnnotCompare.MinPercidProtCompare");
    String PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCENT_LENGTH_FL_COMPARE = SystemConfigurationProperties.getString("Pasa.Default.AnnotCompare.MinPercentLengthFlCompare");
    String PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCENT_ALIGN_LENGTH = SystemConfigurationProperties.getString("Pasa.Default.AnnotCompare.MinPercentAlignLength");
    String PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCENT_OVERLAP_GENE_REPLACE = SystemConfigurationProperties.getString("Pasa.Default.AnnotCompare.MinPercentOverlapGeneReplace");
    String PASA_DEFAULT_ANNOT_COMPARE_MAX_UTR_EXONS = SystemConfigurationProperties.getString("Pasa.Default.AnnotCompare.MaxUtrExons");
    String PASA_DEFAULT_GENETIC_CODE = SystemConfigurationProperties.getString("Pasa.Default.GeneticCode");

    protected static String scratchDirPath = SystemConfigurationProperties.getString("SystemCall.ScratchDir");

    private Logger logger;
    GtfToPasaIntegrationTask task;
    String sessionName;
    PasaResultNode resultNode;
    String pasaDatabaseName;
    File resultDir;
    File estFastaFile;
    File referenceFile;
    File gtfFile;
    File gffFile;
    File pasaConfigFile;

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = (GtfToPasaIntegrationTask) ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            pasaDatabaseName = task.getParameter(GtfToPasaIntegrationTask.PARAM_pasa_database_name);
            if (!pasaDatabaseName.endsWith("_pasa")) {
                pasaDatabaseName += "_pasa";
                logger.info("For GtfToPasaIntegrationService taskId=" + task.getObjectId() + " changing non-conformal database name to=" + pasaDatabaseName);
            }
            referenceFile = getReferenceFile();
            gtfFile = getGtfFile();
            resultNode = createResultFileNode();
            estFastaFile = createEstFastaFile();
            gffFile = createGffFile();
            pasaConfigFile = createConfigFile();
            launchPasaPipeline();
            addToAdminDb();
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            throw new ServiceException(e);
        }
    }

    private File getReferenceFile() throws Exception {
        Long referenceFastaNodeId = new Long(task.getParameter(GtfToPasaIntegrationTask.PARAM_refgenome_fasta_node_id).trim());
        FastaFileNode referenceNode = (FastaFileNode) getComputeBean().getNodeById(referenceFastaNodeId);
        return new File(referenceNode.getFastaFilePath());
    }

    private File getGtfFile() throws Exception {
        Long gtfFileId = new Long(task.getParameter(GtfToPasaIntegrationTask.PARAM_gtf_node_id));
        GtfFileNode gtfFileNode = (GtfFileNode) getComputeBean().getNodeById(gtfFileId);
        return new File(gtfFileNode.getFilePathByTag(GtfFileNode.TAG_GTF));
    }

    public void writePasaConfigFile(File configFile, String databaseName) throws Exception {
        FileWriter fw = new FileWriter(configFile);
        fw.write("# PASA Config file for " + this.getClass().getName() + " taskId=" + task.getObjectId() + "\n");
        fw.write("MYSQLDB=" + databaseName + "\n");
        fw.write("validate_alignments_in_db.dbi:--MAX_INTRON_LENGTH=" + PASA_DEFAULT_ALIGN_MAX_INTRON_LENGTH + "\n");
        fw.write("validate_alignments_in_db.dbi:--MIN_PERCENT_ALIGNED=" + PASA_DEFAULT_ALIGN_MAX_PERCENT_ALIGNED + "\n");
        fw.write("validate_alignments_in_db.dbi:--MIN_AVG_PER_ID=" + PASA_DEFAULT_ALIGN_MIN_AVG_PER_ID + "\n");
        fw.write("subcluster_builder.dbi:-m=50" + "\n");
        fw.write("cDNA_annotation_comparer.dbi:--MIN_PERCENT_OVERLAP=" + PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCENT_OVERLAP + "\n");
        fw.write("cDNA_annotation_comparer.dbi:--MIN_PERCENT_PROT_CODING=" + PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCENT_PROT_CODING + "\n");
        fw.write("cDNA_annotation_comparer.dbi:--MIN_PERID_PROT_COMPARE=" + PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCID_PROT_COMPARE + "\n");
        fw.write("cDNA_annotation_comparer.dbi:--MIN_PERCENT_LENGTH_FL_COMPARE=" + PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCENT_LENGTH_FL_COMPARE + "\n");
        fw.write("cDNA_annotation_comparer.dbi:--MIN_PERCENT_LENGTH_NONFL_COMPARE=" + PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCENT_ALIGN_LENGTH + "\n");
        fw.write("cDNA_annotation_comparer.dbi:--MIN_FL_ORF_SIZE=<__MIN_FL_ORF_SIZE__>" + "\n");
        fw.write("cDNA_annotation_comparer.dbi:--MIN_PERCENT_ALIGN_LENGTH=" + PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCENT_ALIGN_LENGTH + "\n");
        fw.write("cDNA_annotation_comparer.dbi:--MIN_PERCENT_OVERLAP_GENE_REPLACE=" + PASA_DEFAULT_ANNOT_COMPARE_MIN_PERCENT_OVERLAP_GENE_REPLACE + "\n");
        fw.write("cDNA_annotation_comparer.dbi:--STOMP_HIGH_PERCENTAGE_OVERLAPPING_GENE=<__STOMP_HIGH_PERCENTAGE_OVERLAPPING_GENE__>" + "\n");
        fw.write("cDNA_annotation_comparer.dbi:--TRUST_FL_STATUS=<__TRUST_FL_STATUS__>" + "\n");
        fw.write("cDNA_annotation_comparer.dbi:--MAX_UTR_EXONS=" + PASA_DEFAULT_ANNOT_COMPARE_MAX_UTR_EXONS + "\n");
        fw.write("cDNA_annotation_comparer.dbi:--GENETIC_CODE=" + PASA_DEFAULT_GENETIC_CODE + "\n");
        fw.close();
    }

    private PasaResultNode createResultFileNode() throws ServiceException, IOException, DaoException {
        PasaResultNode resultFileNode;

        // Check if we already have a result node for this task
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof PasaResultNode) {
                return (PasaResultNode) node;
            }
        }

        // Create new node
        ComputeBeanRemote computeBean = getComputeBean();
        resultFileNode = new PasaResultNode(task.getOwner(), task,
                "PasaResultNode", "PasaResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        resultFileNode = (PasaResultNode) computeBean.saveOrUpdateNode(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        resultDir = new File(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    protected ComputeBeanRemote getComputeBean() {
        return EJBFactory.getRemoteComputeBean();
    }

    private File createEstFastaFile() throws Exception {
        String gtf2FastaPath = PASA_SCRIPT_DIR + "/" + PASA_GTF2FASTA_SCRIPT;
        File estFile = new File(resultDir, "est.fasta");
        String createEstFastaStr = "cd " + resultDir.getAbsolutePath() + "\n" +
                "perl " + gtf2FastaPath + " -fasta " + referenceFile.getAbsolutePath() + " " + gtfFile.getAbsolutePath() +
                " > " + estFile.getAbsolutePath() + "\n";
        SimpleGridJobRunner job = new SimpleGridJobRunner(resultDir, createEstFastaStr, PASA_GTF_TO_FASTA_QUEUE,
                task.getParameter("project"), task.getObjectId());
        if (!job.execute()) {
            throw new Exception("Grid job failed with cmd=" + createEstFastaStr);
        }
        return estFile;
    }

    private File createGffFile() throws Exception {
        String gtf2GffPath = PASA_SCRIPT_DIR + "/" + PASA_GTF2GFF_SCRIPT;
        String createGffStr = "cd " + resultDir.getAbsolutePath() + "\n" +
                "perl " + gtf2GffPath + " --db " + pasaDatabaseName + " --dir " + resultDir.getAbsolutePath() + " --input_file " +
                gtfFile.getAbsolutePath() + " > gtf2Gff.out\n";
        SimpleGridJobRunner job = new SimpleGridJobRunner(resultDir, createGffStr, PASA_GTF_TO_FASTA_QUEUE,
                task.getParameter("project"), task.getObjectId());
        if (!job.execute()) {
            throw new Exception("Grid job failed with cmd=" + createGffStr);
        }
        File[] files = resultDir.listFiles();
        for (File f : files) {
            if (f.getName().startsWith(pasaDatabaseName) && f.getName().endsWith(".gff")) {
                return f;
            }
        }
        throw new Exception("Could not locate expected gff file in dir=" + resultDir.getAbsolutePath());
    }

    private File createConfigFile() throws Exception {
        File configFile = new File(resultDir, "pasaConfigFile_" + task.getObjectId() + ".txt");
        writePasaConfigFile(configFile, pasaDatabaseName);
        return configFile;
    }

    private void launchPasaPipeline() throws Exception {
        String pasaPipelinePath = PASA_SCRIPT_DIR + "/" + PASA_PIPELINE_SCRIPT;
        String pasaPipelineStr = "cd " + resultDir.getAbsolutePath() + "\n" +
                "perl " + pasaPipelinePath + " -c " + pasaConfigFile.getAbsolutePath() + " -C -R -g " + referenceFile.getAbsolutePath() +
                " -t " + estFastaFile.getAbsolutePath() + " --IMPORT_CUSTOM_ALIGNMENTS_GFF3 " + gffFile.getAbsolutePath() + " > launchPasaPipeline.out\n";
        logger.info("GtfToPasaIntegration taskId=" + task.getObjectId() + " using LaunchPasaPipeline cmd=" + pasaPipelineStr);
        SimpleGridJobRunner job = new SimpleGridJobRunner(resultDir, pasaPipelineStr, PASA_PIPELINE_QUEUE,
                task.getParameter("project"), task.getObjectId());
        if (!job.execute()) {
            throw new Exception("Grid job failed with cmd=" + pasaPipelineStr);
        }
    }

    private void addToAdminDb() throws Exception {
        SystemCall sc = new SystemCall(null, new File(scratchDirPath), logger);
        File assemblyFile = new File(resultDir, pasaDatabaseName + ".assemblies.fasta");
        if (!assemblyFile.exists()) {
            throw new Exception("Could not locate expected assembly fasta file=" + assemblyFile.getAbsolutePath());
        }
        String adminCmd = PASA_ADD_TO_ADMIN_DB_FULLPATH_SCRIPT +
                " -c " + pasaConfigFile.getAbsolutePath() +
                " -w " + resultDir.getAbsolutePath() +
                " -g " + referenceFile.getAbsolutePath() +
                " -t " + assemblyFile.getAbsolutePath();
        logger.info("GtfToPasaIntegration taskId=" + task.getObjectId() + " using addToAdminDbCmd=" + adminCmd);
        int ev = sc.execute(adminCmd, false);
        if (ev != 0) {
            throw new Exception("SystemCall produced non-zero exit value=" + adminCmd);
        }
        else {
            logger.info("AdminDbCmd successful");
        }
    }

}
