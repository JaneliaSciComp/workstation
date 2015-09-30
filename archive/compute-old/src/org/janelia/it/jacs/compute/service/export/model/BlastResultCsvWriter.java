
package org.janelia.it.jacs.compute.service.export.model;

import org.janelia.it.jacs.compute.service.export.writers.ExportWriter;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.BlastHit;
import org.janelia.it.jacs.model.genomics.EntityTypeGenomic;
import org.janelia.it.jacs.model.genomics.Sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 30, 2008
 * Time: 12:31:40 PM
 */
public class BlastResultCsvWriter {
    ExportWriter writer;
    List<BlastHitResult> bhrList;
    public static final int UNKNOWN_SUBJECT_TYPE = 0;
    public static final int PROTEIN_SUBJECT_TYPE = 1;
    public static final int NUC_SUBJECT_TYPE = 2;

    public BlastResultCsvWriter(ExportWriter writer, List<BlastHitResult> bhrList) {
        this.writer = writer;
        this.bhrList = bhrList;
    }

    public void write() throws IOException {
        List<String> headerList = new ArrayList<String>();
        headerList.addAll(BlastHitColumnFormatter.getHeaderList());
        headerList.addAll(SampleColumnFormatter.getHeaderList());
        writer.writeItem(headerList);
        if (bhrList == null || bhrList.size() == 0)
            return; // nothing to do
        for (BlastHitResult bhr : bhrList) {
            List<String> colList = new ArrayList<String>();
            colList.addAll(BlastHitColumnFormatter.formatColumns(bhr));
            Sample sample = null;
            BlastHit bh = bhr.getBlastHit();
            if (bh != null) {
                BaseSequenceEntity bse = bh.getSubjectEntity();
                if (bse != null) {
                    sample = bse.getSample();
                }
            }
            colList.addAll(SampleColumnFormatter.formatColumns(sample));
            writer.writeItem(colList);
        }
    }

    // May be used in the future
    protected int determineSubjectType() {
        BlastHitResult firstResult = bhrList.get(0);
        BlastHit firstBh = firstResult.getBlastHit();
        if (firstBh == null) {
            return UNKNOWN_SUBJECT_TYPE;
        }
        else {
            BaseSequenceEntity bse = firstBh.getSubjectEntity();
            if (bse == null) {
                return UNKNOWN_SUBJECT_TYPE;
            }
            else {
                if (bse.getEntityType().equals(EntityTypeGenomic.PROTEIN) ||
                        bse.getEntityType().equals(EntityTypeGenomic.PEPTIDE)) {
                    return PROTEIN_SUBJECT_TYPE;
                }
                else {
                    return NUC_SUBJECT_TYPE;
                }
            }
        }
    }

}
