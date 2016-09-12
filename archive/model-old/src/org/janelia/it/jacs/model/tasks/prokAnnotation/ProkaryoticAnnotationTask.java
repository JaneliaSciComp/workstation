
package org.janelia.it.jacs.model.tasks.prokAnnotation;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:03:13 PM
 * From jacs.properties
 * # Prokaryotic Annotation Pipeline
 ProkAnnotation.BaseDir=/usr/local/annotation
 ProkAnnotation.ASBaseDir=/usr/local/annotation/OMNIUM/annotation_engine
 ProkAnnotation.NcbiBaseDir=/genbank/genomes/Bacteria/
 ProkAnnotation.PerlBaseDir=prok/
 ProkAnnotation.loadGenomeSetupScript=load_genome_setup.dbi
 ProkAnnotation.GBKPTTScript=GBK_and_PTT_to_DB.dbi
 ProkAnnotation.GBKRNAScript=GBK_RNA.dbi
 ProkAnnotation.NewDatabaseRequestURL=http://intranet.janelia.org/cms/node/2895
 ProkAnnotation.DefaultGridCode=08020
 ProkAnnotation.HelpURL=http://confluence.janelia.org/display/VISW/VICS+Prokaryotic+Pipeline+Info:


 */
public class ProkaryoticAnnotationTask extends Task {
    transient public static final String TASK_NAME = "prokAnnotationTask";
    transient public static final String DISPLAY_NAME = "Prokaryotic Annotation Pipeline";
    // Annotation Mode
    transient public static final String MODE_CMR_GENOME = "CMR Genome";
    transient public static final String MODE_ANNOTATION_SERVICE = "Annotation Service Genome";
    transient public static final String MODE_JCVI_GENOME = "JCVI Genome";

    // Steps for NCBI Genomes
    public static final String LOAD_CONTIGS = "LOAD_CONTIGS";
    public static final String GENOMELOADER_RUN = "GENOMELOADER_RUN";
    public static final String REWRITE_STEP = "REWRITE_STEP";
//    public static final String REWRITE_CHECKER = "REWRITE_CHECKER";
    public static final String GIP_RUNNER = "GIP_RUNNER";
//    public static final String GIP_CHECKER = "GIP_CHECKER";

    public static final String SgcSetup = "SGC Setup";
    public static final String ValetPepHmmIdentify = "ValetPep HMM Identify";
    public static final String ParseForNcRna    = "Parse For ncRna";
    public static final String SkewUpdate = "Skew Update";
    public static final String TerminatorsFinder = "Terminators Finder";
    public static final String RewriteSequences = "Rewrite Sequences";
    public static final String TransmembraneUpdate = "Transmembrane Update";
    public static final String MolecularWeightUpdate = "Molecular Weight Update";
    public static final String OuterMembraneProteinUpdate = "Outer Membrane Protein Update";
    public static final String SignalPUpdate = "SignalP Update";
    public static final String LipoproteinUpdate = "Lipoprotein Update";
    //    public static final String SgcPsortB        = "SGC PsortB";
    public static final String CogSearch = "Cog Search";
    public static final String Hmmer3Search = "HMM3 Search";
    public static final String BtabToMultiAlignment = "Btab-To-Multi Alignment";
    //    public static final String PrositeSearch    = "Prosite Search";
    public static final String AutoGeneCuration = "Auto-Gene Curation";
    public static final String LinkToNtFeatures = "Link To Nt Features";
    public static final String TaxonLoader = "Taxon Loader";
    public static final String EvaluateGenomeProperties = "Evaluate Genome Properties";
    public static final String AutoFrameShiftDetection = "Auto-Frame Shift Detection";
    public static final String BuildContigFile = "Build Contig File";
    //    public static final String PressDb1Con      = "PressDb 1Con File";
    public static final String BuildCoordinateSetFile = "Build Coordinate Set File";
    public static final String BuildSequenceFile = "Build Sequence File";
    //    public static final String PressDbSeq       = "PressDb Sequence File";
    public static final String BuildPeptideFile = "Build Peptide File";
    //    public static final String SetDb            = "SetDb";
    public static final String CoreHMMCheck = "Core HMM Check";

    public static final String GC_CONTENT_LOAD = "GC_CONTENT_LOAD";
    public static final String OVERLAP_RUNNER = "OVERLAP_RUNNER";
//    public static final String SGC_CHECKER = "SGC_CHECKER";
    public static final String SHORT_ORF_TRIM_RUNNER = "SHORT_ORF_TRIM_RUNNER";

    public static final String CONTIG_TYPE_FASTA = "FASTA";
    public static final String CONTIG_TYPE_GOPHER = "Gopher";

    // Additional Steps for Annotation Service and JCVI Genomes
    // todo Get the paralogous families runner working!
    //    public static final String PARALOGOUS_RUNNER= "PARALOGOUS_RUNNER";
    public static final String LOCUS_LOADER = "LOCUS_LOADER";
    public static final String ANNENG_PARSER = "ANNENG_PARSER";
    public static final String ACCESSION_BUILDER = "ACCESSION_BUILDER";
    public static final String CONSISTENCY_CHECKER = "Consistency Checker";
    public static final String CUSTOM_COMMANDS  = "CUSTOM_COMMANDS";

    transient public static List<String> allSectionFlags = Arrays.asList(LOAD_CONTIGS, GENOMELOADER_RUN, REWRITE_STEP, /*REWRITE_CHECKER,*/
            ParseForNcRna,Hmmer3Search, GIP_RUNNER, /*GIP_CHECKER,*/ SgcSetup, ValetPepHmmIdentify, SkewUpdate, TerminatorsFinder,
            RewriteSequences, TransmembraneUpdate, MolecularWeightUpdate, OuterMembraneProteinUpdate, SignalPUpdate, LipoproteinUpdate,
            /*SgcPsortB, */CogSearch, BtabToMultiAlignment, /*PrositeSearch, */AutoGeneCuration, OVERLAP_RUNNER, LinkToNtFeatures, TaxonLoader, EvaluateGenomeProperties,
            AutoFrameShiftDetection, BuildContigFile, /*PressDb1Con,*/ BuildCoordinateSetFile, BuildSequenceFile, /*PressDbSeq,*/
            BuildPeptideFile, /*SetDb, */CoreHMMCheck, GC_CONTENT_LOAD, /*SGC_CHECKER, */SHORT_ORF_TRIM_RUNNER,
            LOCUS_LOADER, ANNENG_PARSER, ACCESSION_BUILDER, CONSISTENCY_CHECKER, CUSTOM_COMMANDS);
    transient public static List<String> ncbiSectionFlags = Arrays.asList(GENOMELOADER_RUN, REWRITE_STEP, /*REWRITE_CHECKER,*/
            ParseForNcRna, Hmmer3Search, GIP_RUNNER, /*GIP_CHECKER, */SgcSetup, ValetPepHmmIdentify, SkewUpdate,
            TerminatorsFinder, RewriteSequences, TransmembraneUpdate, MolecularWeightUpdate, OuterMembraneProteinUpdate,
            SignalPUpdate, LipoproteinUpdate, /*SgcPsortB, */CogSearch, BtabToMultiAlignment, /*PrositeSearch,*/ AutoGeneCuration,
            OVERLAP_RUNNER, LinkToNtFeatures, TaxonLoader, EvaluateGenomeProperties, AutoFrameShiftDetection, BuildContigFile, /*PressDb1Con,*/
            BuildCoordinateSetFile, BuildSequenceFile, /*PressDbSeq, */BuildPeptideFile, /*SetDb, */CoreHMMCheck, GC_CONTENT_LOAD,
            /*SGC_CHECKER, */CONSISTENCY_CHECKER);
    transient public static List<String> annotationServiceSectionFlags = Arrays.asList(GENOMELOADER_RUN, /*REWRITE_CHECKER,*/
            ParseForNcRna, Hmmer3Search, GIP_RUNNER, /*GIP_CHECKER, */SgcSetup, ValetPepHmmIdentify, SkewUpdate,
            TerminatorsFinder, RewriteSequences, TransmembraneUpdate, MolecularWeightUpdate, OuterMembraneProteinUpdate,
            SignalPUpdate, LipoproteinUpdate, /*SgcPsortB, */CogSearch, BtabToMultiAlignment, /*PrositeSearch, */AutoGeneCuration,
            TaxonLoader, EvaluateGenomeProperties, AutoFrameShiftDetection, BuildContigFile, /*PressDb1Con,*/
            BuildCoordinateSetFile, BuildSequenceFile, /*PressDbSeq, */BuildPeptideFile, /*SetDb, */CoreHMMCheck, GC_CONTENT_LOAD,
            /*SGC_CHECKER, */LOCUS_LOADER, ANNENG_PARSER, ACCESSION_BUILDER, CONSISTENCY_CHECKER);
    transient public static List<String> jcviGenomeSectionFlags = Arrays.asList(LOAD_CONTIGS, ParseForNcRna, Hmmer3Search, GIP_RUNNER,
            /*GIP_CHECKER, */SgcSetup, ValetPepHmmIdentify, SkewUpdate, TerminatorsFinder, RewriteSequences, TransmembraneUpdate,
            MolecularWeightUpdate, OuterMembraneProteinUpdate, SignalPUpdate, LipoproteinUpdate, /*SgcPsortB, */CogSearch,
            BtabToMultiAlignment, /*PrositeSearch,*/ AutoGeneCuration, TaxonLoader, EvaluateGenomeProperties,
            AutoFrameShiftDetection, BuildContigFile, /*PressDb1Con, */BuildCoordinateSetFile, BuildSequenceFile, /*PressDbSeq,*/
            BuildPeptideFile, /*SetDb, */CoreHMMCheck, GC_CONTENT_LOAD, /*SGC_CHECKER, */SHORT_ORF_TRIM_RUNNER, LOCUS_LOADER, CONSISTENCY_CHECKER);

    // Parameter Keys
    transient public static final String PARAM_username = "username";
    transient public static final String PARAM_sybasePassword = "Sybase password";
    transient public static final String PARAM_targetDirectory = "targetDirectory";
    transient public static final String PARAM_actionSet = "actionSet";
    transient public static final String PARAM_customCommand = "customCommand";
    transient public static final String PARAM_gipConfigurationString = "gipConfigurationString";
    transient public static final String PARAM_contigFilePath = "contigFilePath";
    transient public static final String PARAM_annotationMode = "annotationMode";


    public ProkaryoticAnnotationTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    protected void setDefaultValues() {
        setParameter(PARAM_username, "");
        setParameter(PARAM_sybasePassword, "");
        setParameter(PARAM_targetDirectory, "");
        setParameter(PARAM_actionSet, "");
        this.taskName = TASK_NAME;
    }

    public ProkaryoticAnnotationTask() {
        super();
        setDefaultValues();
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_sybasePassword)) {
            return new TextParameterVO("**Hidden**", 500);
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public List<String> getActionList() {
        return Task.listOfStringsFromCsvString(getParameter(PARAM_actionSet));
    }

    public void setActionList(List<String> actions) {
        setParameter(PARAM_actionSet, Task.csvStringFromCollection(actions));
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName);
    }

}