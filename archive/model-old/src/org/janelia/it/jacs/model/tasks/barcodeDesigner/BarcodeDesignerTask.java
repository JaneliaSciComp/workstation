
package org.janelia.it.jacs.model.tasks.barcodeDesigner;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:03:13 PM
 * From jacs.properties
 * #BarcodeDesign.PERL5LIB=/usr/local/perl:/usr/local/devel/DAS/software/reseq:/usr/local/projects/CAMERA/servers/camdev5/executables/BarcodeDesigner
 BarcodeDesign.PERL5LIB=/usr/local/perl
 BarcodeDesign.Cmd=primer_design/PrimerDesigner/ProjectTools/SuperScripts/BarcodeDesigner/designBarcodes.pl
 BarcodeDesign.PalindromeBin=/usr/local/packages/EMBOSS-5.0.0/bin/palindrome
 */
public class BarcodeDesignerTask extends Task {
    transient public static final String TASK_NAME = "barcodeDesigner";
    transient public static final String DISPLAY_NAME = "Barcode Designer";

    // Parameter Keys
    transient public static final String PARAM_primerFile = "primer files";
    transient public static final String PARAM_ampliconsFile = "amplicon files";
    transient public static final String PARAM_numBarcodesPerPrimerPair = "number of barcodes per primer pair";
    transient public static final String PARAM_barcodeLength = "barcode length";
    transient public static final String PARAM_fivePrimeClamp = "five prime clamp";
    transient public static final String PARAM_maxFlows = "max flows";
    transient public static final String PARAM_flowSequence = "flow sequence";
    transient public static final String PARAM_keyChar = "key char";
    transient public static final String PARAM_minEditDistance = "min edit distance";
    transient public static final String PARAM_minPalindromeHBonds = "min Palindrome H Bonds";
    transient public static final String PARAM_maxPalindromeMateDistance = "max Palindrome mate distance";
    transient public static final String PARAM_intDimerMaxScore = "int Dimer max score";
    transient public static final String PARAM_endDimerMaxScore = "end Dimer max score";
    transient public static final String PARAM_forwardPrimerAdapterSequence = "forward primer adapter sequence";
    transient public static final String PARAM_reversePrimerAdapterSequence = "reverse primer adapter sequence";
    transient public static final String PARAM_attachBarcodeToForwardPrimer = "attach barcode to forward primer";
    transient public static final String PARAM_attachBarcodeToReversePrimer = "attach barcode to reverse primer";


    public BarcodeDesignerTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public BarcodeDesignerTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_primerFile, "");
        setParameter(PARAM_ampliconsFile, "");
        setParameter(PARAM_numBarcodesPerPrimerPair, "100");
        setParameter(PARAM_barcodeLength, "10");
        setParameter(PARAM_fivePrimeClamp, "CG");
        setParameter(PARAM_maxFlows, "5");
        setParameter(PARAM_flowSequence, "TACG");
        setParameter(PARAM_keyChar, "TCAG");
        setParameter(PARAM_minEditDistance, "3");
        setParameter(PARAM_minPalindromeHBonds, "14");
        setParameter(PARAM_maxPalindromeMateDistance, "11");
        setParameter(PARAM_intDimerMaxScore, "8");
        setParameter(PARAM_endDimerMaxScore, "3");
        setParameter(PARAM_forwardPrimerAdapterSequence, "");
        setParameter(PARAM_reversePrimerAdapterSequence, "");
        setParameter(PARAM_attachBarcodeToForwardPrimer, "false");
        setParameter(PARAM_attachBarcodeToReversePrimer, "false");

        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_numBarcodesPerPrimerPair)) {
            return new DoubleParameterVO(1, 1000, 100);
        }
        if (key.equals(PARAM_barcodeLength)) {
            return new DoubleParameterVO(1, 100, 10);
        }
        if (key.equals(PARAM_maxFlows)) {
            return new DoubleParameterVO(1, 10, 5);
        }
        if (key.equals(PARAM_minEditDistance)) {
            return new DoubleParameterVO(1, 100, 3);
        }
        if (key.equals(PARAM_minPalindromeHBonds)) {
            return new DoubleParameterVO(1, 100, 14);
        }
        if (key.equals(PARAM_maxPalindromeMateDistance)) {
            return new DoubleParameterVO(1, 100, 11);
        }
        if (key.equals(PARAM_intDimerMaxScore)) {
            return new DoubleParameterVO(1, 100, 8);
        }
        if (key.equals(PARAM_endDimerMaxScore)) {
            return new DoubleParameterVO(1, 100, 3);
        }
        if (key.equals(PARAM_fivePrimeClamp) || key.equals(PARAM_flowSequence) || key.equals(PARAM_keyChar) ||
                key.equals(PARAM_forwardPrimerAdapterSequence) || key.equals(PARAM_reversePrimerAdapterSequence)) {
            return new TextParameterVO(value);
        }
        if (key.equals(PARAM_primerFile) || key.equals(PARAM_ampliconsFile)) {
            return new MultiSelectVO(listOfStringsFromCsvString(value), listOfStringsFromCsvString(value));
        }
        if (key.equals(PARAM_attachBarcodeToForwardPrimer) || key.equals(PARAM_attachBarcodeToReversePrimer)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName) && !(!PARAM_barcodeLength.equals(parameterKeyName) &&
                !PARAM_maxFlows.equals(parameterKeyName) &&
                !PARAM_numBarcodesPerPrimerPair.equals(parameterKeyName) &&
                !PARAM_forwardPrimerAdapterSequence.equals(parameterKeyName) &&
                !PARAM_reversePrimerAdapterSequence.equals(parameterKeyName) &&
                !PARAM_attachBarcodeToForwardPrimer.equals(parameterKeyName) &&
                !PARAM_attachBarcodeToReversePrimer.equals(parameterKeyName));
    }

}