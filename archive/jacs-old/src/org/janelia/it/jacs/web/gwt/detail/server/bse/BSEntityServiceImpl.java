
package org.janelia.it.jacs.web.gwt.detail.server.bse;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.BioSequence;
import org.janelia.it.jacs.model.genomics.Sample;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.server.access.FeatureDAO;
import org.janelia.it.jacs.server.access.MetadataDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.web.gwt.common.client.ui.SpanList;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.janelia.it.jacs.web.gwt.detail.client.bse.BSEntityService;
import org.janelia.it.jacs.web.gwt.detail.client.bse.SequenceUIData;
import org.janelia.it.jacs.web.gwt.detail.server.bse.orf.ORFInitializer;
import org.janelia.it.jacs.web.gwt.detail.server.bse.peptide.PeptideInitializer;
import org.janelia.it.jacs.web.gwt.detail.server.bse.read.ReadInitializer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used by GWT client to retrieve data objects needed for detail UI
 *
 * @author Tareq Nabeel
 */
public class BSEntityServiceImpl extends JcviGWTSpringController implements BSEntityService {

    private static Logger logger = Logger.getLogger(BSEntityServiceImpl.class);

    private static List<EvidencePattern> externalEvidencePatternList = null;

    private final BSEntityInitializer[] supportedEntityInitializers = new BSEntityInitializer[]{
            new ORFInitializer(this),
            new PeptideInitializer(this),
            new ReadInitializer(this),
            new DefaultBSEInitializer(this)
    };

    private MetadataDAO metadataDAO;
    private FeatureDAO featureDAO;

    public void setMetadataDAO(MetadataDAO dao) {
        metadataDAO = dao;
    }

    public void setFeatureDAO(FeatureDAO dao) {
        featureDAO = dao;
        for (BSEntityInitializer supportedEntityInitializer : supportedEntityInitializers) {
            supportedEntityInitializer.setFeatureDAO(dao);
        }
    }

    /**
     * Returns a GWT-consumable BaseSequenceEntity instance given a BaseSequenceEntity accession
     *
     * @param accession the camera accession
     * @return BaseSequenceEntity instance
     */
    public Serializable getEntity(String accession) {
        BaseSequenceEntity bsEntity;
        try {
            logger.debug("getting entityInitialzier for accession=" + accession);
            BSEntityInitializer entityInitializer = getBSEntityInitializer(accession);
            if (entityInitializer.getFeatureDAO() == null) {
                logger.error("entityInitializer featureDAO is null");
                throw new RuntimeException("FeatureDAO is null for class=" + entityInitializer.getClass().getName());
            }
            bsEntity = entityInitializer.retrieveBseEntity(accession);
            cleanForGWT(bsEntity);
        }
        catch (DaoException daoe) {
            logger.error("DaoException in BSEntityServiceImpl.getEntity: ", daoe);
            throw new RuntimeException(daoe);
        }
        catch (Throwable e) {
            logger.error("Unexpected exception in BSEntityServiceImpl.getEntity: ", e);
            throw new RuntimeException(e);
        }
        return bsEntity;
    }

    /**
     * Returns a GWT-consumable BaseSequenceEntity instance given a BaseSequenceEntity accession
     *
     * @param accession the camera accession
     * @return BaseSequenceEntity instance
     */
    public BaseSequenceEntity getBseEntity(String accession) {
        try {
            BaseSequenceEntity bsEntity = featureDAO.findBseByAcc(accession);
            cleanForGWT(bsEntity);
            return bsEntity;
        }
        catch (DaoException daoe) {
            logger.error("DaoException in BSEntityServiceImpl.getBseEntity: ", daoe);
            throw new RuntimeException(daoe);
        }
    }

    /**
     * This method returns a GWT-consumable BioSequence object given a BaseSequenceEntity accession
     *
     * @param accession BaseSequenceEntity accession
     * @return BioSequence instance
     */
    public BioSequence getBioSequence(String accession) {
        try {
            // BioSequence does not need clean-up
            // The operation could be expensive especially for large sequence
            //cleanForGWT(bioSequence);
            return featureDAO.getBioSequenceByAccession(accession);
        }
        catch (DaoException daoe) {
            logger.error("DaoException in BSEntityServiceImpl.getBioSequence: ", daoe);
            throw new RuntimeException(daoe);
        }
    }

    /**
     * This method returns a GWT-consumable list of Sample objects given a Library Id
     *
     * @param libraryId the library Id
     * @return list of Samples
     */
    public List<Sample> getSamplesByLibraryId(Long libraryId) {
        try {
            List<Sample> samples = metadataDAO.getSamplesWithSitesByLibraryId(Long.valueOf(libraryId));
            cleanForGWT(samples);
            return samples;
        }
        catch (DaoException daoe) {
            logger.error("DaoException in BSEntityServiceImpl.getSamplesByLibraryId: ", daoe);
            throw new RuntimeException(daoe);
        }
    }

    /**
     * This method returns a GWT-consumable list of Sample objects given a Read Id
     *
     * @param acc entity accession
     * @return the first sample associated with the entity
     */
    public Sample getEntitySampleByAcc(String acc) {
        try {
            Sample sample = metadataDAO.getSampleWithSitesByEntityAcc(acc);
            cleanForGWT(sample);
            return sample;
        }
        catch (DaoException daoe) {
            logger.error("DaoException in BSEntityServiceImpl.getEntitySampleByAcc: ", daoe);
            throw new RuntimeException(daoe);
        }
    }

    /**
     * This method returns a GWT-consumable list of Sample objects given a Sample accession
     *
     * @param sampleAcc the sample accession
     * @return the sample with the given accession
     */
    public Sample getSampleBySampleAcc(String sampleAcc) {
        try {
            Sample sample = metadataDAO.getSampleWithSitesBySampleAcc(sampleAcc);
            cleanForGWT(sample);
            return sample;
        }
        catch (DaoException daoe) {
            logger.error("DaoException in BSEntityServiceImpl.getEntitySampleBySampleAcc", daoe);
            throw new RuntimeException(daoe);
        }
    }

    /**
     * This method returns a GWT-consumable list of Sample objects given a Sample name
     *
     * @param name the sample name
     * @return the first sample associated with the entity
     */
    public Sample getEntitySampleByName(String name) {
        try {
            Sample sample = metadataDAO.getSampleWithSitesBySampleName(name);
            cleanForGWT(sample);
            return sample;
        }
        catch (DaoException daoe) {
            logger.error("DaoException in BSEntityServiceImpl.getEntitySampleByAcc: ", daoe);
            throw new RuntimeException(daoe);
        }
    }

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
    public SequenceUIData getSequenceUIData(String accession,
                                            int clearRangeBegin,
                                            int clearRangeEnd,
                                            int charsPerLine) {
        SequenceUIData sequenceUIData = new SequenceUIData();
        SpanList spanList = new SpanList();
        BioSequence bioSequence = getBioSequence(accession);
        int sequenceLength = bioSequence.getLength();
        String sequence = bioSequence.getSequence();

        // If the clear range is valid, then create three spans representing the different background
        // colors needed to show clear range
        if (isClearRangeValid(clearRangeBegin, clearRangeEnd, sequenceLength)) {
            String preClear = sequence.substring(0, clearRangeBegin);
            String clear = sequence.substring(clearRangeBegin, clearRangeEnd);
            String postClear = sequence.substring(clearRangeEnd, sequence.length());
            spanList.addSpan(preClear, "bseDetailSequenceText");
            spanList.addSpan(clear, "readDetailSequenceClearRangeText");
            spanList.addSpan(postClear, "bseDetailSequenceText");
        }
        else {
            spanList.addSpan(sequence, "bseDetailSequenceText");
        }

        spanList.insertString("<br/>", charsPerLine);
        sequenceUIData.setSequenceSpanList(spanList);
        sequenceUIData.setSequenceType(bioSequence.getSequenceType().getName());
        sequenceUIData.setSequenceLength(String.valueOf(bioSequence.getLength()));
        return sequenceUIData;
    }

    private BSEntityInitializer getBSEntityInitializer(String accessionNo) {
        BSEntityInitializer entityInitializer = null;
        for (BSEntityInitializer supportedEntityInitializer : supportedEntityInitializers) {
            if (supportedEntityInitializer.recognizeAccessionNo(accessionNo)) {
                entityInitializer = supportedEntityInitializer;
                break;
            }
        }
        return entityInitializer;
    }

    private boolean isClearRangeValid(int clearRangeBegin, int clearRangeEnd, int sequenceLength) {
        boolean isValidRange = clearRangeEnd > 0 &&
                clearRangeEnd <= sequenceLength &&
                clearRangeEnd > clearRangeBegin &&
                clearRangeBegin >= 0;
        if (!isValidRange) {
            if (logger.isDebugEnabled())
                logger.debug("Clear Range invalid ... clearRangeBegin=" + clearRangeBegin + " clearRangeEnd=" + clearRangeEnd + " sequenceLength=" + sequenceLength);
        }
        return isValidRange;
    }

    /**
     * This method returns a GWT-consumable list of string, each a synonym of the given taxon Id
     *
     * @param taxonId the taxon id
     * @return list of Samples
     */
    public List<String> getTaxonSynonyms(Integer taxonId) {
        try {
            List<String> synonyms = featureDAO.getTaxonSynonyms(taxonId);
            cleanForGWT(synonyms);
            return synonyms;
        }
        catch (DaoException daoe) {
            logger.error("DaoException in BSEntityServiceImpl.getTaxonSynonyms: ", daoe);
            throw new RuntimeException(daoe);
        }
    }

    private static class EvidencePattern {
        Pattern p;
        String linkPrefix;

        public EvidencePattern(String patternString, String linkPrefix) {
            p = Pattern.compile(patternString);
            this.linkPrefix = linkPrefix;
        }

        public String match(String evidence) {
            if (evidence == null)
                return null;
            String link = null;
            Matcher m = p.matcher(evidence.trim());
            if (m.matches()) {
                if (m.groupCount() > 0)
                    link = linkPrefix + m.group(1);
            }
            return link; // returns null if no match
        }
    }

    /**
     * Returns a list of external link paths for protein evidence corresponding
     * to the input list.
     *
     * @param evidence list of String
     * @return list of String
     */
    public List<String> getExternalEvidenceLinks(List<String> evidence) {
        return createExternalEvidenceLinks(evidence);
    }

    // Static method to permit other usage of this method
    public static List<String> createExternalEvidenceLinks(List<String> evidence) {
        if (externalEvidencePatternList == null) {
            EvidencePattern ncbiPattern = new EvidencePattern(
                    "(\\d+)\\|", "http://www.ncbi.nlm.nih.gov/sites/entrez?db=protein&cmd=search&term=");
            EvidencePattern gbPattern = new EvidencePattern(
                    "GB\\|\\S+\\|(\\d+)\\|\\S+", "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?db=protein&id=");
            EvidencePattern omniPattern = new EvidencePattern(
                    "OMNI\\|(\\S+)", "http://cmr.jcvi.org/cgi-bin/CMR/shared/GenePage.cgi?locus=");
            EvidencePattern pdbPattern = new EvidencePattern(
                    "PDB\\|(\\S+)\\_\\S+\\|\\d+\\|\\S+", "http://www.rcsb.org/pdb/cgi/explore.cgi?pdbId=");
            EvidencePattern pfPattern = new EvidencePattern(
                    "(PF\\d+)", "http://cmr.jcvi.org/cgi-bin/CMR/HmmReport.cgi?hmm_acc=");
            EvidencePattern prfPattern = new EvidencePattern(
                    "PRF\\|(\\S+)\\|\\S+\\|\\S+", "http://www.genome.jp/dbget-bin/www_bget?prf:");
            EvidencePattern rfPattern = new EvidencePattern(
                    "RF\\|\\S+\\|(\\d+)\\|\\S+", "http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?db=protein&id=");
            EvidencePattern spPattern = new EvidencePattern(
                    "SP\\|(\\S+)\\|\\S+", "http://beta.uniprot.org/uniprot/");
            EvidencePattern tigrPattern = new EvidencePattern(
                    "(TIGR\\d+)", "http://cmr.jcvi.org/cgi-bin/CMR/HmmReport.cgi?hmm_acc=");
            externalEvidencePatternList = new ArrayList<EvidencePattern>();
            externalEvidencePatternList.add(ncbiPattern);
            externalEvidencePatternList.add(gbPattern);
            externalEvidencePatternList.add(omniPattern);
            externalEvidencePatternList.add(pdbPattern);
            externalEvidencePatternList.add(pfPattern);
            externalEvidencePatternList.add(prfPattern);
            externalEvidencePatternList.add(rfPattern);
            externalEvidencePatternList.add(spPattern);
            externalEvidencePatternList.add(tigrPattern);
        }
        ArrayList<String> evidenceLinkList = new ArrayList<String>();
        for (Object evidenceObject : evidence) {
            String evidenceString = (String) evidenceObject;
            String linkString = null;
            for (EvidencePattern p : externalEvidencePatternList) {
                linkString = p.match(evidenceString);
                if (linkString != null)
                    break;
            }
            evidenceLinkList.add(linkString);
        }

        // Test - temporary
//        ArrayList<String> testList=new ArrayList<String>();
//        testList.add("13423160|");
//        testList.add("24054684|");
//        testList.add("PROJECT");
//        testList.add("GB|BAC57907.1|28569866|AB090816");
//        testList.add("LipoproteinMotif");
//        testList.add("OMNI|PIN_A1174");
//        testList.add("PDB|1FCQ_A|15988107|1FCQ_A");
//        testList.add("PF00628");
//        testList.add("PRF|0508174A|223108|0508174A");
//        testList.add("PRIAM");
//        testList.add("RF|XP_001230712.1|118780358|XM_001230711");
//        testList.add("RF|NP_651629.1|21357803|NM_143372");
//        testList.add("SP|P34859|NU4LM_APILI");
//        testList.add("TIGR00006");
//        testList.add("TMHMM");
//
//        logger.debug("E check6");
//        for (String testString : testList) {
//            logger.debug("E check7");
//            String linkString=null;
//            for (EvidencePattern p : externalEvidencePatternList) {
//                linkString=p.match(testString);
//                if (linkString!=null)
//                    break;
//            }
//            logger.debug("ExternalEvidenceLink test="+testString+" link="+linkString);
//        }
        return evidenceLinkList;
    }

}
