
package org.janelia.it.jacs.model.tasks.psiBlast;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 11, 2008
 * Time: 3:28:52 PM
 * From jacs.properties
 * # PSI-BLAST Properties
 PgpBlast.MaxOutputFileSizeMB=200
 PgpBlast.MaxQueriesPerExec=100
 PgpBlast.MaxNumberOfJobs=45000
 PgpBlast.Cmd=blast-2.2.15/bin/blastpgp
 PgpBlast.ResultName=blastpgp_output
 PgpBlast.LowThreshold=50
  */
public class PsiBlastTask extends BlastTask {

    private static int MAX_GI_LIST_CHARS = 4000;

    transient public static final String BLAST_NAME = "blastpgp";
    transient public static final String DISPLAY_NAME = "PSI-BLAST (prot/prot)";

    // Base-level default values
    transient public static final Long gappingTriggerBits_DEFAULT = (long) 22;
    transient public static final Long startOfQueryRegion_DEFAULT = (long) 1;
    transient public static final Long endOfQueryRegion_DEFAULT = (long) -1; //-1 indicates end of query
    transient public static final Double evalueMultipassThreshold_DEFAULT = 0.002;
    transient public static final Long multipassPseudocountConstant_DEFAULT = (long) 9;
    transient public static final Long multipassMaxPasses_DEFAULT = (long) 1;
    transient public static final String seqAlignFile_DEFAULT = ""; // optional - ('Believe the query defline' must be TRUE)
    transient public static final String blastpgpCheckpointOutputFile_DEFAULT = ""; // optional - todo Necessary?
    transient public static final String blastpgpRestartInputFile_DEFAULT = ""; // optional - todo Necessary?
    transient public static final Boolean computeOptimalSWAlignmentsLocally_DEFAULT = Boolean.FALSE;
    // todo Need to handle other options for -p than blastpgp, which is the default?
    transient public static final String hitFileForPhiBlast_DEFAULT = "hit_file";
    transient public static final String psiBlastAsciiOutputFile_DEFAULT = ""; // optional - todo Necessary?
    transient public static final String inputAlignmentFileForRestart_DEFAULT = ""; // optional - todo Necessary?
    transient public static final String restrictSearchToGiList_DEFAULT = ""; // optional - todo Necessary?

    // Parameter Keys
    //todo: subdivide parameters into program parameters and environment parameters
    transient public static final String PARAM_gappingTriggerBits = "bit to trigger gapping (-N)";
    transient public static final String PARAM_startOfQueryRegion = "start of query required region (-S)";
    transient public static final String PARAM_endOfQueryRegion = "end of query required region (-H)";
    transient public static final String PARAM_evalueMultipassThreshold = "e-value threshold for multipass inclusion (-h)";
    transient public static final String PARAM_multipassPseudocountConstant = "psuedocount constant for multipass (-C)";
    transient public static final String PARAM_multipassMaxPasses = "multipass max passes (-j)";
//    transient public static final String PARAM_seqAlignFile = "sequence alignment file";
//    transient public static final String PARAM_blastpgpCheckpointOutputFile = "PSI-BLAST checkpoint file";
    //    transient public static final String PARAM_blastpgpRestartInputFile = "PSI-BLAST restart file";
    transient public static final String PARAM_computeOptimalSWAlignmentsLocally = "compute locally optimal Smith-Waterman alignments (-s)";
//    transient public static final String PARAM_hitFileForPhiBlast = "PHI-BLAST hit file";
//    transient public static final String PARAM_psiBlastAsciiOutputFile = "PSI-BLAST Matrix ASCII output file";
    //    transient public static final String PARAM_inputAlignmentFileForRestart = "input alignment file for PSI-BLAST restart";
    transient public static final String PARAM_restrictSearchToGiList = "restrict search to GI list (-l)";

    public PsiBlastTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public PsiBlastTask() {
        setDefaultValues();
    }

    protected void setDefaultValues() {
        super.setDefaultValues();
        setParameter(PARAM_gappingTriggerBits, gappingTriggerBits_DEFAULT.toString());
        setParameter(PARAM_startOfQueryRegion, startOfQueryRegion_DEFAULT.toString());
        setParameter(PARAM_endOfQueryRegion, endOfQueryRegion_DEFAULT.toString());
        setParameter(PARAM_evalueMultipassThreshold, evalueMultipassThreshold_DEFAULT.toString());
        setParameter(PARAM_multipassPseudocountConstant, multipassPseudocountConstant_DEFAULT.toString());
        setParameter(PARAM_multipassMaxPasses, multipassMaxPasses_DEFAULT.toString());
//        setParameter(PARAM_seqAlignFile, seqAlignFile_DEFAULT);
//        setParameter(PARAM_blastpgpCheckpointOutputFile, blastpgpCheckpointOutputFile_DEFAULT);
//        setParameter(PARAM_blastpgpRestartInputFile, blastpgpRestartInputFile_DEFAULT);
        setParameter(PARAM_computeOptimalSWAlignmentsLocally, computeOptimalSWAlignmentsLocally_DEFAULT.toString());
//        setParameter(PARAM_hitFileForPhiBlast, hitFileForPhiBlast_DEFAULT);
//        setParameter(PARAM_psiBlastAsciiOutputFile, psiBlastAsciiOutputFile_DEFAULT);
//        setParameter(PARAM_inputAlignmentFileForRestart, inputAlignmentFileForRestart_DEFAULT);
        setParameter(PARAM_restrictSearchToGiList, restrictSearchToGiList_DEFAULT);

        // override default blastall parameters
        setParameter(PARAM_multiHitWindowSize, "40");
        setParameter(PARAM_hitExtensionThreshold, "11");
        setParameter(PARAM_evalue, "1");
        setParameter(PARAM_ungappedExtensionDropoff, "7");
        setParameter(PARAM_filter, "F");
        setParameter(PARAM_gapOpenCost, "11");
        setParameter(PARAM_gapExtendCost, "1");
        setParameter(PARAM_gappedAlignmentDropoff, "15");
        setParameter(PARAM_finalGappedDropoff, "25");
        setParameter(PARAM_wordsize, "3");

        this.taskName = BLAST_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        ParameterVO pvo = super.getParameterVO(key);
        if (pvo != null)
            return pvo;
        if (key.equals(PARAM_gappingTriggerBits)) {
            return new LongParameterVO(0l, 40l, new Long(value));
        }
        if (key.equals(PARAM_startOfQueryRegion)) {
            return new LongParameterVO(1l, 10l, new Long(value));
        }
        if (key.equals(PARAM_endOfQueryRegion)) {
            return new LongParameterVO(2l, 40l, new Long(value));
        }
        if (key.equals(PARAM_evalueMultipassThreshold)) {
            return new DoubleParameterVO(-100, 10, new Double(value));
        }
        if (key.equals(PARAM_multipassPseudocountConstant)) {
            return new LongParameterVO(0l, 20l, new Long(value));
        }
        if (key.equals(PARAM_multipassMaxPasses)) {
            return new LongParameterVO(0l, 20l, new Long(value));
        }
//        if(key.equals(PARAM_seqAlignFile)){
//            return new TextParameterVO(value, MAX_CHARS);
//        }
//        if(key.equals(PARAM_blastpgpCheckpointOutputFile)){
//            return new TextParameterVO(value, MAX_CHARS);
//        }
//        if(key.equals(PARAM_blastpgpRestartInputFile)){
//            return new TextParameterVO(value, MAX_CHARS);
//        }
        if (key.equals(PARAM_computeOptimalSWAlignmentsLocally)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
//        if(key.equals(PARAM_hitFileForPhiBlast)){
//        // todo Need to handle other options for -p than blastpgp, which is the default?
//            return new TextParameterVO(value, MAX_CHARS);
//        }
//        if(key.equals(PARAM_psiBlastAsciiOutputFile)){
//            return new TextParameterVO(value, MAX_CHARS);
//        }
//        if(key.equals(PARAM_inputAlignmentFileForRestart)){
//            return new TextParameterVO(value, MAX_CHARS);
//        }
        if (key.equals(PARAM_restrictSearchToGiList)) {
            return new TextParameterVO(value, MAX_GI_LIST_CHARS);
        }
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String generateCommandStringNotIncludingIOParams() throws ParameterException {
        StringBuffer sb = new StringBuffer(super.generateDefaultCommandStringNotIncludingIOParams());
        for (String key : getParameterKeySet()) {
            if (key.equals(PARAM_gappingTriggerBits)) {
                sb.append("-N ").append(getParameterVO(PARAM_gappingTriggerBits).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_startOfQueryRegion)) {
                sb.append("-S ").append(getParameterVO(PARAM_startOfQueryRegion).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_endOfQueryRegion)) {
                sb.append("-H ").append(getParameterVO(PARAM_endOfQueryRegion).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_evalueMultipassThreshold)) {
                sb.append("-h ").append(getParameterVO(PARAM_evalueMultipassThreshold).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_multipassPseudocountConstant)) {
                sb.append("-C ").append(getParameterVO(PARAM_multipassPseudocountConstant).getStringValue()).append(" ");
            }
            if (key.equals(PARAM_multipassMaxPasses)) {
                sb.append("-j ").append(getParameterVO(PARAM_multipassMaxPasses).getStringValue()).append(" ");
            }
//            if (key.equals(PARAM_seqAlignFile)) {
//                sb.append("-O ").append(getParameterVO(PARAM_seqAlignFile).getStringValue()).append(" ");
//            }
//            if (key.equals(PARAM_blastpgpCheckpointOutputFile)) {
//                sb.append("-C ").append(getParameterVO(PARAM_blastpgpCheckpointOutputFile).getStringValue()).append(" ");
//            }
//            if (key.equals(PARAM_blastpgpRestartInputFile)) {
//                sb.append("-R ").append(getParameterVO(PARAM_blastpgpRestartInputFile).getStringValue()).append(" ");
//            }
            if (key.equals(PARAM_computeOptimalSWAlignmentsLocally)) {
                sb.append("-s ").append(getParameterVO(PARAM_computeOptimalSWAlignmentsLocally).getStringValue().equalsIgnoreCase("true") ? "T" : "F").append(" ");
            }
//            if (key.equals(PARAM_hitFileForPhiBlast)) {
//                sb.append("-k ").append(getParameterVO(PARAM_hitFileForPhiBlast).getStringValue()).append(" ");
//            }
//            if (key.equals(PARAM_psiBlastAsciiOutputFile)) {
//                sb.append("-Q ").append(getParameterVO(PARAM_psiBlastAsciiOutputFile).getStringValue()).append(" ");
//            }
//            if (key.equals(PARAM_inputAlignmentFileForRestart)) {
//                sb.append("-B ").append(getParameterVO(PARAM_inputAlignmentFileForRestart).getStringValue()).append(" ");
//            }
            if (key.equals(PARAM_restrictSearchToGiList) &&
                    (null != getParameterVO(PARAM_restrictSearchToGiList).getStringValue() && !"".equals(getParameterVO(PARAM_restrictSearchToGiList).getStringValue()))) {
                sb.append("-l ").append(getParameterVO(PARAM_restrictSearchToGiList).getStringValue()).append(" ");
            }
        }
        return sb.toString();
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName) || PARAM_multipassMaxPasses.equalsIgnoreCase(parameterKeyName);
    }
}
