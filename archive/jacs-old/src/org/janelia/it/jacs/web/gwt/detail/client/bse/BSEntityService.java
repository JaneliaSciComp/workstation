
package org.janelia.it.jacs.web.gwt.detail.client.bse;

import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.BioSequence;
import org.janelia.it.jacs.model.genomics.Sample;
import org.janelia.it.jacs.web.gwt.detail.client.DetailService;

import java.util.List;

/**
 * GWT RemoteService for retrieving data needed for BSEntity DetailSubPanels
 *
 * @author Tareq Nabeel
 */
public interface BSEntityService extends DetailService {

    /**
     * This method returns a GWT-consumable BioSequence object given a BaseSequenceEntity accession
     *
     * @param accession BaseSequenceEntity accession
     * @return BioSequence instance
     */
    public BioSequence getBioSequence(String accession);

    /**
     * This method returns a GWT-consumable list of Sample objects given a Read Id
     *
     * @param acc the read Id
     * @return list of Samples
     */
    public Sample getEntitySampleByAcc(String acc);

    /**
     * This method returns a GWT-consumable list of Sample objects given a Sample Name
     *
     * @return list of Samples
     */
    public Sample getEntitySampleByName(String name);

    /**
     * This method returns a GWT-consumable list of Sample objects given a Library Id
     *
     * @param libraryId the library Id
     * @return list of Samples
     */
    public List<Sample> getSamplesByLibraryId(Long libraryId);

    /**
     * This method returns a GWT-consumable list of Sample objects given a Sample Name
     */
    public Sample getSampleBySampleAcc(String name);

    /**
     * This method moves the computation intensive process of creating the sequence
     * span list to the server.
     *
     * @param accession       BaseSequenceEntity accession
     * @param clearRangeBegin clear range begin
     * @param clearRangeEnd   clear range end
     * @param charsPerLine    number of character to break at
     * @return SequenceUIData used to display the sequence
     */
    public SequenceUIData getSequenceUIData(String accession, int clearRangeBegin, int clearRangeEnd, int charsPerLine);

    /**
     * Returns a GWT-consumable BaseSequenceEntity instance given a BaseSequenceEntity accession
     *
     * @param accession the camera accession
     * @return BaseSequenceEntity instance
     */
    public BaseSequenceEntity getBseEntity(String accession);

    /**
     * Returns a GWT-consumable list of strings, each a synonym of the given taxon id
     *
     * @param taxonId the taxon Id
     * @return list of String
     */
    public List<String> getTaxonSynonyms(Integer taxonId);

    /**
     * Returns a list of external link paths for protein evidence corresponding
     * to the input list.
     *
     * @param evidence list of String
     * @return list of String
     */
    public List<String> getExternalEvidenceLinks(List<String> evidence);

}
