package org.janelia.it.jacs.compute.service.domain.sample;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
//import com.google.common.collect.LinkedListMultimap;
//import com.google.common.collect.Multimap;
//import org.janelia.it.jacs.compute.access.DaoException;
//import org.janelia.it.jacs.compute.access.SageDAO;
//import org.janelia.it.jacs.compute.service.domain.model.SlideImage;
//import org.janelia.it.jacs.compute.service.domain.model.SlideImageGroup;
//import org.janelia.it.jacs.model.domain.sample.LSMImage;
//import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.discovery.SageDiscoverServiceHelper;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.sample.DataSet;

import java.util.*;

/**
 * Discovers the SAGE samples associated with the given LSM and processes the corresponding samples.
 */
public class LSMSampleDiscoveryService extends AbstractDomainService {

    // Incoming parameter values are stored as members.
    private String datasetName;
    private String ownerKey;
    private List<String> lsmNames;
    private String orderNo;

    public void execute() throws Exception {
        captureParameters();

        Set<String> sampleIdStrings = processSamples();

        logger.info("Setting the sample ids output: " + sampleIdStrings);
        processData.putItem("SAMPLE_ID", ImmutableList.copyOf(sampleIdStrings));
    }

    /**
     * Driver method for processing samples.
     *
     * @return ids of all samples processed.
     * @throws Exception
     */
    private Set<String> processSamples() throws Exception {
//        // Build a mapping from slide code to all slide images for that code.
//        Multimap<String, SlideImage> slideImagesGroupedBySlideCode = groupSlideImagesBySlideCode(lsmNames);
        // Get the data set.
        DataSet dataset = getDataSet();

        // Populate the sample helper.
        SampleHelperNG sampleHelper = new SampleHelperNG(dataset.getOwnerKey(), logger);
        sampleHelper.setProcess("LSM processing");
        sampleHelper.setOrderNo(orderNo);
        sampleHelper.setDataSetNameFilter(datasetName);
        sampleHelper.getDataSets();

        SageDiscoverServiceHelper sageDiscoverServiceHelper = new SageDiscoverServiceHelper(sampleHelper);
        sageDiscoverServiceHelper.processSageDataSet(dataset, lsmNames);
        Set<Long> sampleIds = sageDiscoverServiceHelper.getVisitedSampleIds();
        Set<String> sampleIdStrings = new HashSet<>();
        for (Long sampleId: sampleIds) {
            sampleIdStrings.add(sampleId.toString());
        }

//        // Process data from all the slide image codes.  Collect affected samples' ids for downstream processing.
//        Set<String> sampleIds = new LinkedHashSet<>();
//        prepareSamplesBySlideCode(sampleHelper, dataset, slideImagesGroupedBySlideCode, sampleIds);
//        sampleHelper.annexSamples();  // Called method is stubbed: here for future ref, if it gets implemented. LLF
//        return sampleIds;
        return sampleIdStrings;
    }

    private void captureParameters() throws MissingDataException {
        datasetName = processData.getString("DATASET_NAME");
        ownerKey = processData.getString("OWNER");
        orderNo = processData.getString("ORDER_NO");

        // Get all LSM names from the process configuration data.
        lsmNames = ImmutableList.copyOf(
                Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split((String) processData.getMandatoryItem("LSM_NAMES")));
    }

    private DataSet getDataSet() throws Exception {
        DataSet dataset;
        try {
            DomainDAL dal = DomainDAL.getInstance();
            dataset = dal.getDataSetByIdentifier(ownerKey, datasetName);
            //processData.
            //                    entityBean.getUserEntitiesWithAttributeValueAndTypeName(null,
            //                    EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER,
            //                    datasetName, EntityConstants.TYPE_DATA_SET);
        } catch (Exception e) {
            String message = "Error retrieving dataset for " + datasetName;
            logger.error(message, e);
            throw new Exception(message, e);
        }
        return dataset;
    }

//    private Multimap<String, SlideImage> groupSlideImagesBySlideCode(List<String> lsmNames) {
//        SageDAO sageDao = new SageDAO(logger);
//        Multimap<String, SlideImage> slideGroups = LinkedListMultimap.create();
//        for (String lsmName : lsmNames) {
//            try {
//                SlideImage slideImage = sageDao.getSlideImageByLSMName(lsmName);
//                if (! slideImage.getDatasetName().equals(datasetName)) {
//                    throw new IllegalArgumentException(
//                        "Data set name from lsm (" + slideImage.getDatasetName() + ") does not match " + datasetName
//                    );
//                }
//                if (slideImage.getSlideCode() != null)
//                    slideGroups.put(slideImage.getSlideCode(), slideImage);
//                else
//                    throw new IllegalArgumentException("Invalid slide code value - slideCode should not be null");
//            } catch (DaoException e) {
//                logger.warn("Error while retrieving image for " + lsmName, e);
//            }
//        }
//        return slideGroups;
//    }

//    private void prepareSamplesBySlideCode(SampleHelperNG sampleHelper,
//                                           DataSet dataset,
//                                           Multimap<String, SlideImage> slideImagesGroupedBySlideCode,
//                                           Collection<String> sampleIds) {
//        for (String slideCode : slideImagesGroupedBySlideCode.keySet()) {
//            try {
//                Collection<SlideImage> slideImages = slideImagesGroupedBySlideCode.get(slideCode);
//                Sample sample = getOrCreateLsmsAndSample(sampleHelper, dataset, slideCode, slideImages);
//                sampleIds.add(sample.getId().toString());
//            } catch (Exception e) {
//                logger.error("Error while preparing image groups for  " + datasetName + ": " + slideCode, e);
//            }
//        }
//    }

//    private Sample getOrCreateLsmsAndSample(SampleHelperNG sampleHelper,
//                                            DataSet dataset,
//                                            String slideCode,
//                                            Collection<SlideImage> slideImages)
//            throws Exception {
//        logger.info("Group images for slideCode " + slideCode);
//        Map<String, SlideImageGroup> tileGroups = new LinkedHashMap<>();
//
//        int tileNum = 0;
//        List<LSMImage> allImages = new ArrayList<>();
//        for (SlideImage slideImage : slideImages) {
//            String area = slideImage.getArea();
//            String tag = slideImage.getTileType();
//            if (tag==null) {
//                tag = "Tile "+(tileNum+1);
//            }
//            String groupKey = area+"_"+tag;
//            SlideImageGroup tileGroup = tileGroups.get(groupKey);
//            if (tileGroup==null) {
//                tileGroup = new SlideImageGroup(area, tag);
//                tileGroups.put(groupKey, tileGroup);
//            }
//
//            // Creating LSM image in Jacs.
//            LSMImage lsmImage = sampleHelper.createOrUpdateLSM(slideImage);
//            tileGroup.addFile(lsmImage);
//            allImages.add(lsmImage);
//            tileNum++;
//        }
//        List<SlideImageGroup> tileGroupList = new ArrayList<>(tileGroups.values());
//
//        // Sort the pairs by their tag name
//        Collections.sort(tileGroupList, new Comparator<SlideImageGroup>() {
//            @Override
//            public int compare(SlideImageGroup o1, SlideImageGroup o2) {
//                return o1.getTag().compareTo(o2.getTag());
//            }
//        });
//
//        //  Obtaining a sample in jacs.
//        return sampleHelper.createOrUpdateSample(slideCode, dataset, allImages);
//    }
}
