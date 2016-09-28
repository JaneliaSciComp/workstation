
package org.janelia.it.jacs.model.tasks.ap16s;

import org.janelia.it.jacs.model.common.AP16QualityThresholds;
import org.janelia.it.jacs.model.genomics.PrimerPair;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:03:13 PM
 * From jacs.properties
 * # AHP 16S Analysis Pipeline
 AP16S.PipelineCmd=16sDataAnalysis/site_analysis/analyze_bactPCR_sample_consolidated.sh
 #AP16S.ClassifierCmd=site_analysis/Use_MSU_RDP_Classifier.pl
 AP16S.ClassifierCmd=16sDataAnalysis/site_analysis/runLocalRDP.sh
 AP16S.MassagingCmd=16sDataAnalysis/site_analysis/RDP_massaging.csh
 */
public class AnalysisPipeline16sTask extends Task {
    transient public static final String TASK_NAME = "ahp16s";
    transient public static final String DISPLAY_NAME = "AHP 16S/18S Small Sub-Unit Analysis";
    // Sample fragment file
    // /usr/local/projects/16S/archaeal/AURUMEN02-A-01-1P5KB/jtc_frg_AURUMEN02-A-01-1P5KB_1112526251548_1210193172142_1.frg
    // Example project code - 600002 (Eli)

    // Parameter Keys
    transient public static final String PARAM_fragmentFiles = "fragment files";
    transient public static final String PARAM_subjectDatabase = "subject database";
    transient public static final String PARAM_filenamePrefix = "filename prefix";
    transient public static final String PARAM_skipClustalW = "skip clustalw step";
    transient public static final String PARAM_iterateCdHitESTClustering = "iterate cd-hit-est based clustering";
    transient public static final String PARAM_useMsuRdpClassifier = "use MSU RDP Classifier";
    transient public static final String PARAM_primer1Sequence = "primer 1 sequence";
    transient public static final String PARAM_primer2Sequence = "primer 2 sequence";
    transient public static final String PARAM_primer1Defline = "primer 1 defline";
    transient public static final String PARAM_primer2Defline = "primer 2 defline";
    transient public static final String PARAM_ampliconSize = "amplicon size";

    transient public static final String PARAM_readLengthMinimum = "read length minimum";
    transient public static final String PARAM_minAvgQV = "minimum average quality value";
    transient public static final String PARAM_maxNCount = "maximum N-count in a read";
    transient public static final String PARAM_minIdentCount = "minimum identity count in 16S hit";
    transient public static final String PARAM_qualFile = "quality file";


    public static final String SEQUENCER_TYPE_454 = "454";
    public static final String SEQUENCER_TYPE_SANGER = "Sanger";
    public static final String SEQUENCER_TYPE_CUSTOM = "Custom Settings";

    // Default values - default overrides
    transient public static final String filenamePrefix_DEFAULT = "mySample";

    private static List<PrimerPair> primerList = new ArrayList<PrimerPair>();
    private static List<AP16QualityThresholds> sequencerList = new ArrayList<AP16QualityThresholds>();


    /**
     * Rather than couple the system with another lookup file, I decided to hard-wire the primer pairs.  This list
     * of 7 has been unchanged for an entire year (Eli)
     */
    static {
        PrimerPair tmpPairOne = new PrimerPair(PrimerPair.RRNA_SUBUNIT_16S, PrimerPair.ORGANISM_TYPE_BACTERIAL,
                "27f-MP", "AGRGTTTGATCMTGGCTCAG",
                "1492r-MP", "TACGGYTACCTTGTTAYGACTT", "1300", "GOS & HMP");
        primerList.add(tmpPairOne);

        PrimerPair tmpPairTwo = new PrimerPair(PrimerPair.RRNA_SUBUNIT_18S, PrimerPair.ORGANISM_TYPE_EUKARYOTIC,
                "S12.2", "GATYAGATACCGTCGTAGTC",
                "SB", "TGATCCTTCTGCAGGTTCACCTAC", "800", "GOS");
        primerList.add(tmpPairTwo);

        PrimerPair tmpPairThree = new PrimerPair(PrimerPair.RRNA_SUBUNIT_16S, PrimerPair.ORGANISM_TYPE_PLASTID,
                "PLA491F", "GAGGAATAAGCATCGGCTAA",
                "OXY1313R", "CTTCAYGYAGGCGAGTTGCAGC", "830", "GOS");
        primerList.add(tmpPairThree);

        PrimerPair tmpPairFour = new PrimerPair(PrimerPair.RRNA_SUBUNIT_16S, PrimerPair.ORGANISM_TYPE_ARCHAEL,
                "Arc21f", "TTCCGGTTGATCCTGCCGGA",
                "Univ529r", "ACCGCGGCKGCTGGC", "475", "GOS");
        primerList.add(tmpPairFour);

        PrimerPair tmpPairFive = new PrimerPair(PrimerPair.RRNA_SUBUNIT_16S, PrimerPair.ORGANISM_TYPE_BACTERIAL,
                "27f", "GAGTTTGATCCTGGCTCAG",
                "1492r", "GGTTACCTTGTTACGACTT", "1400", "Moore 155");
        primerList.add(tmpPairFive);

        PrimerPair tmpPairSix = new PrimerPair(PrimerPair.RRNA_SUBUNIT_16S, PrimerPair.ORGANISM_TYPE_ARCHAEL,
                "A571F", "GCYTAAAGSRICCGTAGC",
                "UA1406R", "TTMGGGGCATRCIKACCT", "700", "Moore 155");
        primerList.add(tmpPairSix);

//        PrimerPair tmpPairSeven = new PrimerPair(PrimerPair.RRNA_SUBUNIT_18S, PrimerPair.ORGANISM_TYPE_FUNGAL,
//                "EF4", "GGAAGGGRTGTATTTATTAG",
//                "FUNG5", "GTAAAAGTCCTGGTTCCCC", "TBD", "Australia soils.  MOSTLY ASCOMYCOTA");
        // TBD - is no good for now
//        primerList.add(tmpPairSeven);

        AP16QualityThresholds flxSettings = new AP16QualityThresholds(SEQUENCER_TYPE_454, "flx.params", 100, 33, 0, 60);
        AP16QualityThresholds sangerSettings = new AP16QualityThresholds(SEQUENCER_TYPE_SANGER, "sanger.params", 250, 33, 1, 200);
        AP16QualityThresholds customSettings = new AP16QualityThresholds(SEQUENCER_TYPE_CUSTOM, "custom.params", 100, 33, 0, 60);
        sequencerList.add(flxSettings);
        sequencerList.add(sangerSettings);
        sequencerList.add(customSettings);
    }

    public AnalysisPipeline16sTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public AnalysisPipeline16sTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_fragmentFiles, "");
        setParameter(PARAM_qualFile, "");
        setParameter(PARAM_subjectDatabase, "");
        setParameter(PARAM_filenamePrefix, filenamePrefix_DEFAULT);
        setParameter(PARAM_skipClustalW, Boolean.TRUE.toString());
        setParameter(PARAM_iterateCdHitESTClustering, Boolean.FALSE.toString());
        setParameter(PARAM_useMsuRdpClassifier, Boolean.TRUE.toString());
        setParameter(PARAM_primer1Defline, "");
        setParameter(PARAM_primer2Defline, "");
        setParameter(PARAM_primer1Sequence, "");
        setParameter(PARAM_primer2Sequence, "");
        setParameter(PARAM_ampliconSize, "");
        setParameter(PARAM_readLengthMinimum, "0");
        setParameter(PARAM_minAvgQV, "0");
        setParameter(PARAM_maxNCount, "0");
        setParameter(PARAM_minIdentCount, "0");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_skipClustalW) || key.equals(PARAM_iterateCdHitESTClustering) ||
                key.equals(PARAM_useMsuRdpClassifier)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        if (key.equals(PARAM_filenamePrefix)) {
            return new TextParameterVO(value, 100);
        }
        if (key.equals(PARAM_qualFile)) {
            return new TextParameterVO(value, 400);
        }
        if (key.equals(PARAM_fragmentFiles)) {
            return new MultiSelectVO(listOfStringsFromCsvString(value), listOfStringsFromCsvString(value));
        }
        if (key.equals(PARAM_subjectDatabase)) {
            return new SingleSelectVO(getSubjectDatabaseList(), value);
        }
        if (key.equals(PARAM_readLengthMinimum)) {
            return new DoubleParameterVO(50, 800, Double.valueOf(value));
        }
        if (key.equals(PARAM_minAvgQV)) {
            return new DoubleParameterVO(0, 40, Double.valueOf(value));
        }
        if (key.equals(PARAM_maxNCount)) {
            return new DoubleParameterVO(0, 100, Double.valueOf(value));
        }
        if (key.equals(PARAM_minIdentCount)) {
            return new DoubleParameterVO(20, 800, Double.valueOf(value));
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return !(!super.isParameterRequired(parameterKeyName) || PARAM_skipClustalW.equals(parameterKeyName) ||
                PARAM_iterateCdHitESTClustering.equals(parameterKeyName));
    }

    public List<String> getSubjectDatabaseList() {
        ArrayList<String> tmpList = new ArrayList<String>();
        tmpList.add("archaeal16S");
        tmpList.add("bact16S");
        tmpList.add("chloroplast16S");
        tmpList.add("fungal18S");
        tmpList.add("s12sb18S");
        return tmpList;
    }

    public static PrimerPair getPrimerPair(int index) {
        return primerList.get(index);
    }

    public static List<PrimerPair> getPrimerList() {
        return primerList;
    }

    public static AP16QualityThresholds getSequencerSetting(int index) {
        return sequencerList.get(index);
    }

    public static List<AP16QualityThresholds> getSequencerList() {
        return sequencerList;
    }

}