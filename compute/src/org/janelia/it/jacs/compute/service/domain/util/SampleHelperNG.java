package org.janelia.it.jacs.compute.service.domain.util;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.service.common.ContextLogger;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.domain.model.SlideImage;
import org.janelia.it.jacs.compute.service.domain.model.SlideImageGroup;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.enums.PipelineStatus;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.sample.*;
import org.janelia.it.jacs.model.domain.support.DomainObjectAttribute;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.ReprocessOnChange;
import org.janelia.it.jacs.model.domain.support.SAGEAttribute;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.reflections.ReflectionUtils;

/**
 * Helper methods for dealing with Samples.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleHelperNG extends DomainHelper {

    private static final String NO_CONSENSUS_VALUE = "NO_CONSENSUS";
    private static final String DEFAULT_SAMPLE_NAME_PATTERN = "{Line}-{Slide Code}";

    private List<DataSet> dataSets;
    private String dataSetNameFilter;

    // Lookup tables
    private Map<String,SageField> lsmSageFields;
    private Map<String,SageField> sampleSageFields;
    private Map<String,DomainObjectAttribute> sampleAttrs;
    
    // Processing state
    private Map<Long,LSMImage> lsmCache = new HashMap<>();
    private String process;
    private String orderNo;

    private Set<Long> reprocessLsmIds = new HashSet<>();
    private Set<Long> changedLsmIds = new HashSet<>();
    private Set<Long> changedSampleIds = new HashSet<>();
    private Set<String> sageAttrsNotFound = new HashSet<>();
    private int numSamplesCreated = 0;
    private int numSamplesUpdated = 0;
    private int numSamplesReprocessed = 0;

    
    public SampleHelperNG(String ownerKey, Logger logger) {
        this(ownerKey, logger, null, null);
    }

    public SampleHelperNG(String ownerKey, Logger logger, ContextLogger contextLogger) {
        this(ownerKey, logger, contextLogger, null);
    }

    public SampleHelperNG(String ownerKey, Logger logger, ContextLogger contextLogger, DomainDAL dal) {
        super(ownerKey, logger, contextLogger, dal);
    }


    public void setDataSetNameFilter(String dataSetNameFilter) {
        this.dataSetNameFilter = dataSetNameFilter;
    }

    public LSMImage createOrUpdateLSM(SlideImage slideImage) throws Exception {
        
    	logger.debug("createOrUpdateLSM("+slideImage.getName()+")");
        boolean dirty = false;

        LSMImage lsm = findBestLsm(slideImage.getSageId());
        if (lsm==null) {
            lsm = new LSMImage();
            lsm.setFiles(new HashMap<FileType,String>());
            reprocessLsmIds.add(lsm.getId());
            logger.info("Created new LSM for SAGE image#"+slideImage.getSageId());
            dirty = true;
        }
        
        if (updateLsmAttributes(lsm, slideImage)) {
            logger.info("Updated LSM properties for "+slideImage.getName());
            dirty = true;
        }
        
        if (dirty) {
            lsm = domainDAL.save(ownerKey, lsm);
        }
        else if (!lsm.getSageSynced()) {
            domainDAL.updateProperty(ownerKey, LSMImage.class, lsm.getId(), "sageSynced", true);
        }
        
        lsmCache.put(lsm.getId(), lsm);
        return lsm;
    }

    public LSMImage getLSMs(SlideImage slideImage) throws Exception {
        logger.debug("getLSMs("+slideImage.getName()+")");
        return findBestLsm(slideImage.getSageId());
    }

    private LSMImage findBestLsm(Integer sageId) {

        List<LSMImage> lsms = domainDAL.getUserLsmsBySageId(ownerKey, sageId);
        if (lsms.isEmpty()) {
            return null;
        }

        // If there is an active LSM, return that
        for(LSMImage lsm : lsms) {
            if (lsm.getSageSynced()) {
                return lsm;
            }
        }

        // Otherwise, return the most recently created LSM
        Collections.sort(lsms, new Comparator<LSMImage>() {
            @Override
            public int compare(LSMImage o1, LSMImage o2) {
                return o2.getCreationDate().compareTo(o1.getCreationDate());
            }
        });

        return lsms.get(0);
    }

    private boolean updateLsmAttributes(LSMImage lsm, SlideImage slideImage) throws Exception {

    	logger.debug("updateLsmAttribute(lsmId="+lsm.getId()+",sageId="+slideImage.getSageId()+")");
        boolean changed = false;
        boolean dirty = false;
        
        Map<String,SageField> lsmSageAttrs = getLsmSageFields();
        for(String key : slideImage.getProperties().keySet()) {
            try {
                SageField sageField = lsmSageAttrs.get(key);
                if (sageField==null) {
                	if (!sageAttrsNotFound.contains(key)) {
                		logger.warn("SAGE Attribute not found on LSMImage: "+key);
                		sageAttrsNotFound.add(key);
                	}
                	continue;
                }
                Object value = slideImage.getProperties().get(key);

                String strValue = value==null?null:value.toString();
                Object trueValue = null;
                if (value!=null) {
                    Class<?> fieldType = sageField.field.getType();
                    // Convert the incoming value from SAGE to the correct type in our domain model
                    if (fieldType.equals(String.class)) {
                        trueValue = value.toString();
                    }
                    else if (fieldType.equals(Date.class)) {
                        // Dates are represented as java.sql.Timestamps, which is a subclass of Date, 
                        // so this should be safe to assign directly
                        trueValue = value;
                    }
                    else if (fieldType.equals(Long.class)) {
                    	if (value instanceof Long) {
                    		trueValue = value;
                    	}
                    	else {
                            if (!StringUtils.isEmpty(strValue)) {
                            	trueValue = new Long(strValue);
                            }
                    	}
                    }
                    else if (fieldType.equals(Integer.class)) {
                    	if (value instanceof Integer) {
                    		trueValue = value;
                    	}
                    	else {
                            if (!StringUtils.isEmpty(strValue)) {
                            	trueValue = new Integer(strValue);
                            }
                    	}
                    }
                    else if (fieldType.equals(Boolean.class)) {
                        if (value instanceof Boolean) {
                            trueValue = value;
                        }
                        else if (value instanceof Integer) {
                            trueValue = new Boolean(((Integer)value)!=0);
                        }
                        else {
                            if (!StringUtils.isEmpty(strValue)) {
                            	trueValue = new Boolean(strValue);
                            }
                        }
                    }
                    else {
                        // This might take care of future types we may not have anticipated
                        trueValue = value;
                    }
                }

                UpdateType ut = updateValue(lsm, sageField.field.getName(), sageField, trueValue);
                if (ut != UpdateType.SAME) {
                    dirty = true;
                }
                if (ut == UpdateType.CHANGE) {
                    changed = true;

                    // check if this attribute triggers a reprocess.
                    Field lsmField = lsm.getClass().getDeclaredField(sageField.field.getName());
                    if (lsmField!=null) {
                        ReprocessOnChange reprocessAttr = lsmField.getAnnotation(ReprocessOnChange.class);
                        if (reprocessAttr!=null) {
                            reprocessLsmIds.add(lsm.getId());
                        }
                    }
                }
            }
            catch (Exception e) {
                logger.error("Error setting SAGE attribute value "+key+" for LSM#"+lsm.getId(),e);
            }
        }

        // Other attributes which are not automatically populated using @SAGEAttribute
        
        if (!StringUtils.areEqual(lsm.getName(), slideImage.getName())) {
            lsm.setName(slideImage.getName());
            dirty = true;
            changed = true;
        }
        
        String filepath = slideImage.getFilepath();
        if (!StringUtils.areEqual(lsm.getFilepath(), filepath)) {
            lsm.setFilepath(filepath);
            lsm.getFiles().put(FileType.LosslessStack,filepath);
            dirty = true;
            changed = true;
        }

        String objective = slideImage.getObjective();
        if (!StringUtils.areEqual(lsm.getObjective(), objective)) {
            lsm.setObjective(objective);
            dirty = true;
            changed = true;
        }
        
        if (lsm.getVoxelSizeX()!=null && lsm.getVoxelSizeY()!=null && lsm.getVoxelSizeZ()!=null) {
            String opticalRes = lsm.getVoxelSizeX()+"x"+lsm.getVoxelSizeY()+"x"+lsm.getVoxelSizeZ();
            if (!StringUtils.areEqual(lsm.getOpticalResolution(), opticalRes)) {
                lsm.setOpticalResolution(opticalRes);
                dirty = true;
                changed = true;
            }
        }

        if (lsm.getDimensionX()!=null && lsm.getDimensionY()!=null && lsm.getDimensionZ()!=null) {
            String imageSize = lsm.getDimensionX()+"x"+lsm.getDimensionY()+"x"+lsm.getDimensionZ();
            if (!StringUtils.areEqual(lsm.getImageSize(), imageSize)) {
                lsm.setImageSize(imageSize);
                dirty = true;
                changed = true;
            }
        }

        if (lsm.getAnatomicalArea()==null) {
            lsm.setAnatomicalArea("");
            dirty = true;
        }


        if (changed) {
            changedLsmIds.add(lsm.getId());
        }

        return dirty;
    }
        
    public Sample createOrUpdateSample(String slideCode, DataSet dataSet, Collection<LSMImage> lsms) throws Exception {

    	logger.info("Creating or updating sample: "+slideCode+" ("+(dataSet==null?"":"dataSet="+dataSet.getIdentifier())+")");

        Multimap<String,SlideImageGroup> objectiveGroups = HashMultimap.create();
    	boolean lsmAdded = false;
        int tileNum = 0;
        for(LSMImage lsm : lsms) {

        	// Have any of the LSMs been changed signficantly? If so, we need to mark the sample for reprocessing later.
        	if (reprocessLsmIds.contains(lsm.getId())) {
                lsmAdded = true;
        	}
        	
        	// Extract LSM metadata
        	String objective = lsm.getObjective();
            if (objective==null) {
                objective = "";
            }
            String tag = lsm.getTile();
            if (tag==null) {
                tag = "Tile "+(tileNum+1);
            }
            String area = lsm.getAnatomicalArea();

            // Group LSMs by objective, tile and area
            Collection<SlideImageGroup> subTileGroupList = objectiveGroups.get(objective);
            SlideImageGroup group = null;
            for (SlideImageGroup slideImageGroup : subTileGroupList) {
            	if (StringUtils.areEqual(slideImageGroup.getTag(), tag) && StringUtils.areEqual(slideImageGroup.getAnatomicalArea(), area)) {
            		group = slideImageGroup;
            		break;
            	}
            }
            if (group==null) {
            	group = new SlideImageGroup(area, tag);
            	objectiveGroups.put(objective, group);
            }
            group.addFile(lsm);
            
            tileNum++;
        }
        
        logger.debug("  Sample objectives: "+objectiveGroups.keySet());

        Sample sample = getOrCreateSample(slideCode, dataSet);
        boolean sampleNew = sample.getId()==null;
        boolean sampleDirty = sampleNew;

        if (setSampleAttributes(dataSet, sample, objectiveGroups.values())) {
            sampleDirty = true;
        }

        // marks Samples that have been changed (lsm added/removed)
        boolean needsReprocessing = lsmAdded;

        if (lsmAdded && !sampleNew) {
            logger.info("  LSMs modified significantly, will mark sample for reprocessing");
        }

        if (changedSampleIds.contains(sample.getId()) && !sampleNew) {
            logger.info("  Sample attributes changed, will mark sample for reprocessing");
        }

        // First, remove all tiles/LSMSs from objectives which are no longer found in SAGE
        for(ObjectiveSample objectiveSample : new ArrayList<>(sample.getObjectiveSamples())) {
        	if (!objectiveGroups.containsKey(objectiveSample.getObjective())) {

                if ("".equals(objectiveSample.getObjective()) && objectiveGroups.size()==1) {
                    logger.warn("  Leaving empty objective alone, because it is the only one");
                    continue;
                }

	        	if (!objectiveSample.hasPipelineRuns()) {
                    logger.warn("  Removing existing '"+objectiveSample.getObjective()+"' objective sample");
	        		sample.removeObjectiveSample(objectiveSample);
	        	}
	        	else {
                    logger.warn("  Resetting tiles for existing "+objectiveSample.getObjective()+" objective sample");
	        		objectiveSample.setTiles(new ArrayList<SampleTile>());
	        	}
	            sampleDirty = true;
        	}
        }
        
        List<String> objectives = new ArrayList<>(objectiveGroups.keySet());
        Collections.sort(objectives);
        for(String objective : objectives) {
            Collection<SlideImageGroup> subTileGroupList = objectiveGroups.get(objective);
                        
            // Figure out the number of channels that should be in the final merged/stitched sample
            int sampleNumSignals = getNumSignalChannels(subTileGroupList);
            int sampleNumChannels = sampleNumSignals+1;
            String channelSpec = ChanSpecUtils.createChanSpec(sampleNumChannels, sampleNumChannels);

            logger.info("  Processing objective '"+objective+"', signalChannels="+sampleNumSignals+", chanSpec="+channelSpec);
            
            // Find the sample, if it exists, or create a new one.
            UpdateType ut = createOrUpdateObjectiveSample(sample, objective, channelSpec, subTileGroupList);
            if (ut!=UpdateType.SAME) {
                sampleDirty = true;
            }
            if (ut==UpdateType.CHANGE && !sampleNew) {
                logger.info("  Objective sample '"+objective+"' changed, will mark sample for reprocessing");
                needsReprocessing = true;
            }
        }
        
        if (!sample.getSageSynced()) {
            sample.setSageSynced(true);
            sampleDirty = true;
        }

        if (needsReprocessing) {
        	markForProcessing(sample);
        	sampleDirty = true;
        }
        
        if (sampleDirty) {
            sample = domainDAL.save(ownerKey, sample);
            if (sampleNew)
                logStatusTransition(sample.getId(), PipelineStatus.Intake, PipelineStatus.New);
            logger.info("  Saving sample: "+sample.getName()+" (id="+sample.getId()+")");
            numSamplesUpdated++;
        }

        // Update all back-references from the sample's LSMs
        Reference sampleRef = Reference.createFor(sample);
        List<Reference> lsmRefs = sample.getLsmReferences();
        for(Reference lsmRef : lsmRefs) {
        	LSMImage lsm = lsmCache.get(lsmRef.getTargetId());
        	if (lsm==null) {
        		logger.warn("LSM (id="+lsmRef.getTargetId()+") not found in cache. This should never happen and indicates a bug.");
        		continue;
        	}
        	if (!StringUtils.areEqual(lsm.getSample(),sampleRef)) {
        		lsm.setSample(sampleRef);
        		saveLsm(lsm);
        		logger.info("  Updated sample reference for LSM#"+lsm.getId());
        	}
        }

        if (sampleDirty) {
            // Update the permissions on the Sample and its LSMs and neuron fragments
            domainDAL.addPermissions(dataSet.getOwnerKey(), Sample.class.getSimpleName(), sample.getId(), dataSet);
        }

        return sample;
    }

    /**
     * Annex all the samples that were tracked as needing annexing, during processing with findOrAnnexExistingSample().
     * @throws Exception
     */
    public void annexSamples() throws Exception {
        // Not yet porting this.  Unknown what to-annex means.
//        if (samplesToAnnex.isEmpty()) return;
//        logger.info("Will annexing "+samplesToAnnex.size()+" samples");
//        for(Long entityId : new ArrayList<>(samplesToAnnex)) {
//            try {
//                samplesToAnnex.remove(entityId);
//                entityBean.annexEntityTree(ownerKey, entityId);
//                numSamplesAnnexed++;
//            }
//            catch (Exception e) {
//                logger.error("Error annexing sample: "+entityId, e);
//            }
//        }
    }

    private Sample getOrCreateSample(String slideCode, DataSet dataSet) {
        
        Sample sample = findBestSample(dataSet.getIdentifier(), slideCode);
        if (sample != null) {
        	logger.info("  Found existing sample "+sample.getId()+" with status "+sample.getStatus());
        	return sample;
        }
        
        // If no matching samples were found, create a new sample
        sample = new Sample();
        sample.setDataSet(dataSet.getIdentifier());
        sample.setSlideCode(slideCode);
        sample.setStatus(PipelineStatus.New.getStatus());

    	logger.info("  Creating new sample for "+dataSet.getIdentifier()+"/"+slideCode);
        numSamplesCreated++;
        return sample;
    }

    private Sample findBestSample(String dataSetIdentifier, String slideCode) {

        List<Sample> samples = domainDAL.getUserSamplesBySlideCode(ownerKey, dataSetIdentifier, slideCode);

        if (samples.isEmpty()) {
            return null;
        }

        // If there is an active Sample, return that
        for(Sample sample : samples) {
            if (sample.getSageSynced()) {
                return sample;
            }
        }

        // Otherwise, return the most recently created Sample
        Collections.sort(samples, new Comparator<Sample>() {
            @Override
            public int compare(Sample o1, Sample o2) {
                return o2.getCreationDate().compareTo(o1.getCreationDate());
            }
        });

        return samples.get(0);
    }

    private boolean setSampleAttributes(DataSet dataSet, Sample sample, Collection<SlideImageGroup> tileGroupList) {

        boolean dirty = false;
        boolean changed = false;
        Date maxTmogDate = null;

        Map<String,Object> consensusValues = new HashMap<>();
        Map<String,SageField> lsmSageAttrs = getLsmSageFields();
        Set<String> nonconsensus = new HashSet<>();
        
        for(SlideImageGroup tileGroup : tileGroupList) {
            for(LSMImage lsm : tileGroup.getImages()) {
                
                for(SageField lsmAttr : lsmSageAttrs.values()) {
                    String fieldName = lsmAttr.field.getName();
                    Class<?> fieldType = lsmAttr.field.getType();
                    Object value = null;
                    try {
                        value = org.janelia.it.jacs.shared.utils.ReflectionUtils.getFieldValue(lsm, lsmAttr.field);
                    }
                    catch (Exception e) {
                        logger.error("  Problem getting value for LSMImage."+fieldName,e);
                    }
                    // Special consideration is given to the TMOG Date, so that the latest LSM TMOG date is recorded as the Sample TMOG date. 
                    if ("tmogDate".equals(fieldName)) {
                        Date date = (Date)value;
                        if (maxTmogDate==null || date.after(maxTmogDate)) {
                            maxTmogDate = date;
                        }
                    }
                    else if (!nonconsensus.contains(fieldName)) {
                        Object consensusValue = consensusValues.get(fieldName);
                        if (consensusValue==null) {
                            consensusValues.put(fieldName, value);
                        }
                        else if (!StringUtils.areEqual(consensusValue,value)) {
                            nonconsensus.add(fieldName);
                            consensusValues.put(fieldName, fieldType.equals(String.class)?NO_CONSENSUS_VALUE:null);
                        }    
                    }
                }
            }
        }

        if (maxTmogDate!=null) {
            consensusValues.put("tmogDate", maxTmogDate);
        }
        
        if (logger.isTraceEnabled()) {
	        logger.trace("  Consensus values: ");
	        for(String key : consensusValues.keySet()) {
	            Object value = consensusValues.get(key);
	            if (!NO_CONSENSUS_VALUE.equals(value)) {
	                logger.trace("    "+key+": "+value);
	            }
	        }
        }
        
        Map<String,SageField> sampleAttrs = getSampleSageFields();
        for(String fieldName : consensusValues.keySet()) {
        	SageField sampleAttr = sampleAttrs.get(fieldName);
            if (sampleAttr!=null) {
                try {
                    Object consensusValue = consensusValues.get(fieldName);
                    UpdateType ut = updateValue(sample, fieldName, sampleAttr, consensusValue);
                    if (ut != UpdateType.SAME) {
                        dirty = true;
                    }
                    if (ut == UpdateType.CHANGE) {
                        changed = true;
                    }
                }
                catch (Exception e) {
                    logger.error("  Problem setting Sample."+fieldName,e);
                }
            }
        }
        
        String newName = getSampleName(dataSet, sample);
        if (!StringUtils.areEqual(sample.getName(),newName)) {
            logger.info("  Updating sample name to: "+newName);
            sample.setName(newName);
            dirty = true;
            changed = true;
        }

        if (changed) {
            changedSampleIds.add(sample.getId());
        }

        return dirty;
    }

    /**
     * Create a new name for a sample, given the sample's attributes.
     * {Line}-{Slide Code}-Right_Optic_Lobe
     * {Line}-{Slide Code}-Left_Optic_Lobe
     * {VT line|Line}-{Slide Code}-Left_Optic_Lobe
     * {Line}-{Effector}-{Age}
     */
    public String getSampleName(DataSet dataSet, Sample sample) {
        
    	Map<String,DomainObjectAttribute> sampleAttrs = getSampleAttrs();
        Map<String,Object> valueMap = new HashMap<>();
        for(String key : sampleAttrs.keySet()) {
        	DomainObjectAttribute attr = sampleAttrs.get(key);
        	Object obj = null;
        	try {
        		obj = attr.getGetter().invoke(sample);
        	}
        	catch (Exception e) {
        		logger.error("Error getting sample attribute value for: "+key,e);
        	}
        	if (obj!=null) {
        		valueMap.put(key, obj.toString());
        	}
        }

        String sampleNamePattern = dataSet==null?DEFAULT_SAMPLE_NAME_PATTERN:dataSet.getSampleNamePattern();
        return StringUtils.replaceVariablePattern(sampleNamePattern, valueMap);
    }
    
    private UpdateType createOrUpdateObjectiveSample(Sample sample, String objective, String chanSpec, Collection<SlideImageGroup> tileGroupList) throws Exception {

        ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
        if (objectiveSample==null) {
            objectiveSample = sample.getObjectiveSample("");
            if (objectiveSample==null) {
                objectiveSample = new ObjectiveSample(objective);
                objectiveSample.setChanSpec(chanSpec);
                sample.addObjectiveSample(objectiveSample);
                synchronizeTiles(objectiveSample, tileGroupList);
                logger.debug("  Created new objective '"+objective+"' for sample "+sample.getName());
                return UpdateType.ADD;
            }
            else {
                objectiveSample.setObjective(objective);
                logger.debug("  Updated objective to '"+objective+"' for legacy sample with empty objective");
                return UpdateType.ADD;
            }
        }
        else if (synchronizeTiles(objectiveSample, tileGroupList)) {
            return UpdateType.CHANGE;
        }

        return UpdateType.SAME;
    }

    public boolean synchronizeTiles(ObjectiveSample objectiveSample, Collection<SlideImageGroup> tileGroupList) throws Exception {

        if (!tilesMatch(objectiveSample, tileGroupList)) {
            // Something has changed, so just recreate the tiles
            List<SampleTile> tiles = new ArrayList<>();
            for (SlideImageGroup tileGroup : tileGroupList) {
                SampleTile sampleTile = new SampleTile();
                sampleTile.setName(tileGroup.getTag());
                sampleTile.setAnatomicalArea(tileGroup.getAnatomicalArea());
                List<Reference> lsmReferences = new ArrayList<>();
                for(LSMImage lsm : tileGroup.getImages()) {
                    lsmReferences.add(Reference.createFor(lsm));
                }
                sampleTile.setLsmReferences(lsmReferences);
                tiles.add(sampleTile);
            }
            objectiveSample.setTiles(tiles);
            logger.info("  Updated tiles for objective '"+objectiveSample.getObjective()+"'");
            return true;
        }

        boolean dirty = false;

        for (SlideImageGroup tileGroup : tileGroupList) {
            SampleTile sampleTile = objectiveSample.getTileByNameAndArea(tileGroup.getTag(), tileGroup.getAnatomicalArea());
            if (sampleTile==null) {
            	throw new IllegalStateException("No such tile: "+tileGroup.getTag());
            }
            if (!StringUtils.areEqual(tileGroup.getAnatomicalArea(),sampleTile.getAnatomicalArea())) {
                sampleTile.setAnatomicalArea(tileGroup.getAnatomicalArea());
                logger.info("  Updated anatomical area for tile "+sampleTile.getName()+" to "+sampleTile.getAnatomicalArea());
                dirty = true;
            }
        }
        
        return dirty;
    }
    
    public boolean tilesMatch(ObjectiveSample objectiveSample, Collection<SlideImageGroup> tileGroupList) throws Exception {
        
        Set<SampleTile> seenTiles = new HashSet<>();
        
        logger.trace("  Checking if tiles match");
        
        for (SlideImageGroup tileGroup : tileGroupList) {
        	
        	logger.trace("  Checking for "+tileGroup.getTag());

            // Ensure each tile is in the sample
            SampleTile sampleTile = objectiveSample.getTileByNameAndArea(tileGroup.getTag(), tileGroup.getAnatomicalArea());
            if (sampleTile==null) {
            	logger.info("  Existing sample does not contain tile '"+tileGroup.getTag()+"' with anatomical area '"+tileGroup.getAnatomicalArea()+"'");
                return false;
            }
            seenTiles.add(sampleTile);
            
            Set<Long> lsmIds1 = new HashSet<>();
            for(LSMImage lsm : tileGroup.getImages()) {
                lsmIds1.add(lsm.getId());
            }

            Set<Long> lsmIds2 = new HashSet<>();
            for(Reference lsmReference : sampleTile.getLsmReferences()) {
                lsmIds2.add(lsmReference.getTargetId());
            }

            // Ensure each tiles references the correct LSMs
            if (!lsmIds1.equals(lsmIds2)) {
            	logger.info("  LSM sets are not the same ("+lsmIds1+"!="+lsmIds2+").");
                return false;
            }
        }
        
        if (objectiveSample.getTiles().size() != seenTiles.size()) {
            // Ensure that the sample has no extra tiles it doesn't need
        	logger.info("  Tile set sizes are not the same ("+objectiveSample.getTiles().size()+"!="+seenTiles.size()+").");
            return false;
        }
        
        logger.trace("  Tiles match!");
        return true;
    }

    private enum UpdateType {
        ADD,
        REMOVE,
        CHANGE,
        SAME
    }

    /**
     * Set the value of the given field name on the given object, if the new value is different from the current value.
     * @param object
     * @param fieldName
     * @param sageField
     * @param newValue
     * @return true if the value has changed
     * @throws Exception
     */
    private UpdateType updateValue(Object object, String fieldName, SageField sageField, Object newValue) throws Exception {
        Object currValue = org.janelia.it.jacs.shared.utils.ReflectionUtils.getFieldValue(object, sageField.field);
        if (!StringUtils.areEqual(currValue, newValue)) {
            org.janelia.it.jacs.shared.utils.ReflectionUtils.setFieldValue(object, sageField.field, newValue);

            if (currValue != null) {
                logger.debug("  Setting " + fieldName + "='" + newValue+"' (previously '"+currValue+"')");
            }
            else {
                logger.debug("  Setting " + fieldName + "='" + newValue+"'");
            }

            if (currValue == null) return UpdateType.ADD;
            else if (newValue == null) return UpdateType.REMOVE;
            else return UpdateType.CHANGE;
        }
        else {
            logger.trace("  Already set "+fieldName+"="+newValue);
            return UpdateType.SAME;
        }
    }
    
    private void markForProcessing(Sample sample) throws Exception {
        if (sample.isBlocked()) {
            return;
        }
        logStatusTransition(sample.getId(), PipelineStatus.valueOf(sample.getStatus()), PipelineStatus.Scheduled);
        sample.setStatus(PipelineStatus.Scheduled.toString());

        numSamplesReprocessed++;
    }
    
    private Map<String,DomainObjectAttribute> getSampleAttrs() {
        if (sampleAttrs==null) {
            logger.info("Building sample attribute map");
            this.sampleAttrs = new HashMap<>();
            for (DomainObjectAttribute attr : DomainUtils.getSearchAttributes(Sample.class)) {
                logger.info("  "+attr.getLabel()+" -> Sample."+attr.getName());
                sampleAttrs.put(attr.getLabel(), attr);
            }
        }
        return sampleAttrs;
    }  
    
    private Map<String,SageField> getLsmSageFields() {
        if (lsmSageFields==null) {
            logger.info("Building LSM SAGE field map");
            this.lsmSageFields = new HashMap<>();
            for (Field field : ReflectionUtils.getAllFields(LSMImage.class)) {
                SAGEAttribute sageAttribute = field.getAnnotation(SAGEAttribute.class);
                if (sageAttribute!=null) {
                    SageField attr = new SageField();
                    attr.cvName = sageAttribute.cvName();
                    attr.termName = sageAttribute.termName();
                    attr.field = field;
                    logger.info("  " + attr.getKey() + " -> LsmImage." + field.getName());
                    lsmSageFields.put(attr.getKey(), attr);
                }
            }
        }
        return lsmSageFields;
    }

    public void logStatusTransition(Long sampleId, PipelineStatus source, PipelineStatus target) throws Exception {
        domainDAL.addPipelineStatusTransition(sampleId, source, target, orderNo, process, null);
    }

    private Map<String,SageField> getSampleSageFields() {
        if (sampleSageFields==null) {
            logger.info("Building sample SAGE field map");
            this.sampleSageFields = new HashMap<>();
            for (Field field : ReflectionUtils.getAllFields(Sample.class)) {
                SAGEAttribute sageAttribute = field.getAnnotation(SAGEAttribute.class);
                if (sageAttribute!=null) {
                    SageField attr = new SageField();
                    attr.cvName = sageAttribute.cvName();
                    attr.termName = sageAttribute.termName();
                    attr.field = field;
                    logger.info("  "+field.getName()+" -> Sample."+field.getName());
                    sampleSageFields.put(field.getName(), attr);
                }
            }
        }
        return sampleSageFields;
    }    

    private class SageField {
        String cvName;
        String termName;
        Field field;
        public String getKey() {
            return cvName+"_"+termName;
        }
    }
    
    /**
     * Return the channel specification for the LSM (or create a default one using the number of channels).
     * @param lsm
     * @return
     */
    public String getLSMChannelSpec(LSMImage lsm, int refIndex) {
        
        String chanSpec = lsm.getChanSpec();
        if (!StringUtils.isEmpty(chanSpec)) {
            return chanSpec;
        }
        
        Integer numChannels = lsm.getNumChannels();
        if (numChannels!=null) {
            try {
            	return ChanSpecUtils.createChanSpec(numChannels, refIndex+1);    
            }
            catch (NumberFormatException e) {
                logger.warn("Could not parse Num Channels ('"+numChannels+"') on LSM with id="+lsm.getId());
            }
        }
        
        throw new IllegalStateException("LSM has no Channel Specification and no Num Channels");
    }

    /**
     * Calculate and return the number of signal channels across all the SlideImages in a tile group (and thus, the 
     * number of signal channels in the eventual merged tile that results from this group).
     * @param tileGroupList
     * @return
     */
    public int getNumSignalChannels(Collection<SlideImageGroup> tileGroupList) {
        int sampleNumSignals = -1;
        for(SlideImageGroup tileGroup : tileGroupList) {

            logger.trace("  Calculating number of channels in tile "+tileGroup.getTag());
            
            int tileNumSignals = 0;
            for(LSMImage lsm : tileGroup.getImages()) {
                String chanspec = lsm.getChanSpec();
                if (chanspec!=null) {
                    for(int j=0; j<chanspec.length(); j++) {
                        if (chanspec.charAt(j)=='s') {
                            tileNumSignals++;
                        }
                    }
                }
            }
            
            if (tileNumSignals<1) {
                logger.trace("  Falling back on channel number");
                // We didn't get the information from the channel spec, let's fall back on inference from numChannels
                for(LSMImage lsm : tileGroup.getImages()) {
                    Integer numChannels = lsm.getNumChannels();
                    if (numChannels!=null) {
                        tileNumSignals += numChannels - 1;
                    }
                }
            }
            
            logger.trace("  Tile '"+tileGroup.getTag()+"' has "+tileNumSignals+" signal channels");
            
            if (sampleNumSignals<0) {
                sampleNumSignals = tileNumSignals;
            }
            else if (sampleNumSignals != tileNumSignals) {
                logger.warn("  No consensus for number of signal channels per tile ("+sampleNumSignals+" != "+tileNumSignals+")");
            }
        }
        return sampleNumSignals;
    }

    /**
     * Go through a sample area's tiles and look for a concatenated LSM attribute with a given name. If a consensus can 
     * be reached across all the Tiles in the area, then return that consensus. Otherwise log a warning and return null.
     * @param sampleArea
     * @param attrName
     * @return
     * @throws Exception
     */
    public String getConsensusTileAttributeValue(AnatomicalArea sampleArea, String attrName, String delimiter) throws Exception {
        List<AnatomicalArea> sampleAreas = new ArrayList<>();
        sampleAreas.add(sampleArea);
        return getConsensusTileAttributeValue(sampleAreas, attrName, delimiter);
    }

    /**
     * Go through a set of sample areas' tiles and look for an attribute with a given name. If a consensus
     * can be reached across all the LSM's in the area then return that consensus. Otherwise log a warning and return null.
     * @param attrName
     * @return
     * @throws Exception
     */
    public String getConsensusTileAttributeValue(List<AnatomicalArea> sampleAreas, String attrName, String delimiter) throws Exception {
        Sample sample = null;
        String consensus = null;
        logger.trace("Determining consensus for " + attrName + " for sample areas: " + getSampleAreasCSV(sampleAreas));
        for(AnatomicalArea sampleArea : sampleAreas) {
        	logger.trace("  Determining consensus for "+attrName+" in "+sampleArea.getName()+" sample area");
		
        	if (sample==null) {
            	sample = domainDAL.getDomainObject(null, Sample.class, sampleArea.getSampleId());
        	}
        	else if (!sample.getId().equals(sampleArea.getSampleId())) {
        	    throw new IllegalStateException("All sample areas must come from the same sample");
        	}
        	
        	ObjectiveSample objectiveSample = sample.getObjectiveSample(sampleArea.getObjective());
        	for(SampleTile sampleTile : getTilesForArea(objectiveSample, sampleArea)) {
        	    logger.trace("    Determining consensus for "+attrName+" in "+sampleTile.getName()+" tile");
            	List<LSMImage> lsms = domainDAL.getDomainObjectsAs(sampleTile.getLsmReferences(), LSMImage.class);
	        	
            	StringBuilder sb = new StringBuilder();
                for(LSMImage image : lsms) {
                    Object value = DomainUtils.getAttributeValue(image, attrName);
                    if (sb.length()>0) sb.append(delimiter);
                    if (value!=null) sb.append(value);
                }
                
                String tileValue = sb.toString();
                if (consensus!=null && !StringUtils.areEqual(consensus,tileValue)) {
                    logger.warn("No consensus for attribute '"+attrName+"' can be reached for sample area "+sampleArea.getName());
                    return null;
                }
                else {
                    consensus = tileValue==null?null:tileValue.toString();
                }
        	}
        
        }
        return consensus;
    }
    
    /**
     * Go through a sample area's LSM supporting files and look for an attribute with a given name. If a consensus
     * can be reached across all the LSM's in the area then return that consensus. Otherwise log a warning and return null.
     * @param attrName
     * @return
     * @throws Exception
     */
    public String getConsensusLsmAttributeValue(AnatomicalArea sampleArea, String attrName) throws Exception {
        List<AnatomicalArea> sampleAreas = new ArrayList<>();
        sampleAreas.add(sampleArea);
        return getConsensusLsmAttributeValue(sampleAreas, attrName);
    }

    /**
     * Go through a set of sample areas' LSM supporting files and look for an attribute with a given name. If a consensus
     * can be reached across all the LSM's in the area then return that consensus. Otherwise log a warning and return null.
     * @param attrName
     * @return
     * @throws Exception
     */
    public String getConsensusLsmAttributeValue(List<AnatomicalArea> sampleAreas, String attrName) throws Exception {
        Sample sample = null;
        String consensus = null;
        logger.trace("Determining consensus for " + attrName + " for sample areas: " + getSampleAreasCSV(sampleAreas));
        for(AnatomicalArea sampleArea : sampleAreas) {
        	logger.trace("  Determining consensus for "+attrName+" in "+sampleArea.getName()+" sample area");
		
        	if (sample==null) {
            	sample = domainDAL.getDomainObject(null, Sample.class, sampleArea.getSampleId());
        	}
        	else if (!sample.getId().equals(sampleArea.getSampleId())) {
        	    throw new IllegalStateException("All sample areas must come from the same sample");
        	}
        	
        	ObjectiveSample objectiveSample = sample.getObjectiveSample(sampleArea.getObjective());
            for(SampleTile sampleTile : getTilesForArea(objectiveSample, sampleArea)) {
        	    logger.trace("    Determining consensus for "+attrName+" in "+sampleTile.getName()+" tile");
            	List<LSMImage> lsms = domainDAL.getDomainObjectsAs(sampleTile.getLsmReferences(), LSMImage.class);
            	
                for(LSMImage image : lsms) {
    	        	logger.trace("      Determining consensus for "+attrName+" in "+image.getName()+" LSM");
                    Object value = DomainUtils.getAttributeValue(image, attrName);
                    if (consensus!=null && !StringUtils.areEqual(consensus,value)) {
                        logger.warn("No consensus for attribute '"+attrName+"' can be reached for sample area "+sampleArea.getName());
                        return null;
                    }
                    else {
                        consensus = value==null?null:value.toString();
                    }
                }
        	}
        
        }
        return consensus;
    }

    /**
     * Go through a set of sample areas' LSM supporting files and look for first attribute with a given name. Try this
     * only after attempting to get the consensus.  Can return null, if attribute never found.
     * @param attrName attribute to lookup
     * @return first available, non-null value for that attribute.
     * @throws Exception
     */
    public String getFirstLsmAttributeValue(AnatomicalArea sampleArea, String attrName) throws Exception {
        Sample sample = domainDAL.getDomainObject(null, Sample.class, sampleArea.getSampleId());
        if (sample == null) {
            logger.warn("No sample found for sample area " + sampleArea);
            return null;
        }
        String rtnVal = null;
        logger.trace("  Returning first instance of "+attrName+" in "+sampleArea.getName()+" sample area");

        ObjectiveSample objectiveSample = sample.getObjectiveSample(sampleArea.getObjective());
        if (objectiveSample == null) {
            logger.warn("No objectiveSample found for sample area " + sampleArea.getObjective());
            return null;
        }
        for(SampleTile sampleTile : getTilesForArea(objectiveSample, sampleArea)) {
            List<LSMImage> lsms = domainDAL.getDomainObjectsAs(sampleTile.getLsmReferences(), LSMImage.class);

            for(LSMImage image : lsms) {
                Object value = DomainUtils.getAttributeValue(image, attrName);
                if (value != null) {
                    rtnVal = value.toString();
                    break;
                }
            }
        }
        return rtnVal;
    }

    private String getSampleAreasCSV(List<AnatomicalArea> sampleAreas) {
    	StringBuilder sb = new StringBuilder();
    	for(AnatomicalArea sampleArea : sampleAreas) {
    		if (sb.length()>0) sb.append(",");
    		sb.append(sampleArea.getName());
    	}
    	return sb.toString();
    }
    
    /**
     * Return the data sets for the configured owner.
     * @return
     * @throws Exception
     */
    public Collection<DataSet> getDataSets() throws Exception {
        if (dataSets==null) {
            loadDataSets();
        }
        return dataSets;
    }
    
    public int getNumSamplesCreated() {
        return numSamplesCreated;
    }

    public int getNumSamplesUpdated() {
        return numSamplesUpdated;
    }

    public int getNumSamplesReprocessed() {
        return numSamplesReprocessed;
    }

    private void loadDataSets() throws Exception {

        if (dataSets!=null) return;

        this.dataSets = domainDAL.getUserDomainObjects(ownerKey, DataSet.class);

        if (dataSetNameFilter != null) {
            List<DataSet> filteredDataSets = new ArrayList<>();
            for (DataSet dataSet : dataSets) {
                if (dataSetNameFilter.equals(dataSet.getName())) {
                    filteredDataSets.add(dataSet);
                    break;
                }
            }
            dataSets = filteredDataSets;
        }

        if (dataSets.isEmpty()) {
            logger.info("No data sets found for user: "+ownerKey);
            return;
        }

        Collections.sort(dataSets, new Comparator<DataSet>() {
			@Override
			public int compare(DataSet o1, DataSet o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
    }

    public List<SampleTile> getTilesForArea(ObjectiveSample objectiveSample, AnatomicalArea area) {
        List<SampleTile> tiles = new ArrayList<>();
        for(SampleTile tile : objectiveSample.getTiles()) {
            if (area.getName().equals(tile.getAnatomicalArea()) && area.getTileNames().contains(tile.getName())) {
                tiles.add(tile);
            }
        }
        return tiles;
    }

    /* --------------------------- */

    public void saveLsm(LSMImage lsm) throws Exception {
        domainDAL.save(ownerKey, lsm);
    }
    
    public void saveSample(Sample sample) throws Exception {
        domainDAL.save(ownerKey, sample);
    }

    public void saveNeuron(NeuronFragment neuron) throws Exception {
        domainDAL.save(ownerKey, neuron);
    }

    public SamplePipelineRun addNewPipelineRun(ObjectiveSample objectiveSample, String name, String pipelineProcess, int pipelineVersion) {
        SamplePipelineRun run = new SamplePipelineRun();
        run.setId(domainDAL.getNewId());
        run.setCreationDate(new Date());
        run.setName(name);
        run.setPipelineProcess(pipelineProcess);
        run.setPipelineVersion(pipelineVersion);
        objectiveSample.addRun(run);
        return run;
    }

    public LSMSummaryResult addNewLSMSummaryResult(SamplePipelineRun run, String resultName) {
        LSMSummaryResult result = new LSMSummaryResult();
        result.setId(domainDAL.getNewId());
        result.setCreationDate(new Date());
        result.setName(resultName);
        result.setFiles(new HashMap<FileType,String>());
        run.addResult(result);
        return result;
    }
    
    public SampleProcessingResult addNewSampleProcessingResult(SamplePipelineRun run, String resultName) {
        SampleProcessingResult result = new SampleProcessingResult();
        result.setId(domainDAL.getNewId());
        result.setCreationDate(new Date());
        result.setName(resultName);
        result.setFiles(new HashMap<FileType, String>());
        run.addResult(result);
        return result;
    }

    public SampleAlignmentResult addNewAlignmentResult(SamplePipelineRun run, String resultName) {
        SampleAlignmentResult result = new SampleAlignmentResult();
        result.setId(domainDAL.getNewId());
        result.setCreationDate(new Date());
        result.setName(resultName);
        result.setFiles(new HashMap<FileType,String>());
        run.addResult(result);
        return result;
    }
    
    public SamplePostProcessingResult addNewSamplePostProcessingResult(SamplePipelineRun run, String resultName) {
        SamplePostProcessingResult result = new SamplePostProcessingResult();
        result.setId(domainDAL.getNewId());
        result.setCreationDate(new Date());
        result.setName(resultName);
        result.setFiles(new HashMap<FileType,String>());
        run.addResult(result);
        return result;
    }
    
    public NeuronSeparation addNewNeuronSeparation(PipelineResult result, String resultName) {
        NeuronSeparation separation = new NeuronSeparation();
        separation.setId(domainDAL.getNewId());
        separation.setCreationDate(new Date());
        separation.setName(resultName);
        separation.setFiles(new HashMap<FileType,String>());

        ReverseReference fragmentsReference = new ReverseReference();
        fragmentsReference.setReferringClassName(NeuronFragment.class.getSimpleName());
        fragmentsReference.setReferenceAttr("separationId");
        fragmentsReference.setReferenceId(separation.getId());
        separation.setFragmentsReference(fragmentsReference);
        
        result.addResult(separation);
        return separation;
    }

    public NeuronFragment addNewNeuronFragment(NeuronSeparation separation, Integer index) {
        Sample sample = separation.getParentRun().getParent().getParent();
        NeuronFragment neuron = new NeuronFragment();
        neuron.setOwnerKey(sample.getOwnerKey());
        neuron.setReaders(sample.getReaders());
        neuron.setWriters(sample.getWriters());
        neuron.setCreationDate(new Date());
        neuron.setName("Neuron Fragment "+index);
        neuron.setNumber(index);
        neuron.setSample(Reference.createFor(sample));
        neuron.setSeparationId(separation.getId());
        neuron.setFilepath(separation.getFilepath());
        neuron.setFiles(new HashMap<FileType, String>());
        return neuron;
    }
    
    public PipelineError setPipelineRunError(SamplePipelineRun run, String filepath, String description, String classification) {
        PipelineError error = new PipelineError();
        error.setCreationDate(new Date());
        error.setFilepath(filepath);
        error.setDescription(description);
        error.setClassification(classification);
        run.setError(error);
        return error;
    }

    public PipelineResult addResult(SamplePipelineRun run, PipelineResult result) {
        run.addResult(result);
        return result;
    }

    public List<FileGroup> createFileGroups(HasFilepath parent, List<String> filepaths) throws Exception {

        Map<String,FileGroup> groups = new HashMap<>();
    
        for(String filepath : filepaths) {
            
            File file = new File(filepath);
            String filename = file.getName();
            int d = filename.lastIndexOf('.');
            String name = filename.substring(0, d);
            String ext = filename.substring(d+1);
            
            FileType fileType = null;

            String key = null;
            if (filename.endsWith(".lsm.json")) {
            	key = FilenameUtils.getBaseName(name);
                fileType = FileType.LsmMetadata;
            }
            else if (filename.endsWith(".lsm.metadata")) {
                // Ignore, to get rid of the old-style Perl metadata files
                continue;
            }
            else if ("properties".equals(ext)) {
                // Ignore properties files here, they should be specifically processed, not sucked into a file group
                continue;
            }
            else {
                int u = name.lastIndexOf('_');
                key = name.substring(0, u);
                String type = name.substring(u+1);
                if ("png".equals(ext)) {
                    if ("all".equals(type)) {
                        fileType = FileType.AllMip; 
                    }
                    else if ("reference".equals(type)) {
                        fileType = FileType.ReferenceMip;   
                    }
                    else if ("signal".equals(type)) {
                        fileType = FileType.SignalMip;  
                    }
                    else if ("signal1".equals(type)) {
                        fileType = FileType.Signal1Mip; 
                    }
                    else if ("signal2".equals(type)) {
                        fileType = FileType.Signal2Mip; 
                    }
                    else if ("signal3".equals(type)) {
                        fileType = FileType.Signal3Mip; 
                    }
                    else if ("refsignal1".equals(type)) {
                        fileType = FileType.RefSignal1Mip;  
                    }
                    else if ("refsignal2".equals(type)) {
                        fileType = FileType.RefSignal2Mip;  
                    }
                    else if ("refsignal3".equals(type)) {
                        fileType = FileType.RefSignal3Mip;  
                    }
                }
                else if ("mp4".equals(ext)) {
                    if ("all".equals(type) || "movie".equals(type)) {
                        fileType = FileType.AllMovie;   
                    }
                    else if ("reference".equals(type)) {
                        fileType = FileType.ReferenceMovie; 
                    }
                    else if ("signal".equals(type)) {
                        fileType = FileType.SignalMovie;    
                    }
                }
            }
            
            if (fileType==null) {
                logger.warn("  Could not determine file type for: "+filename);
                continue;
            }
            
            FileGroup group = groups.get(key);
            if (group==null) {
                group = new FileGroup(key);
                group.setFilepath(parent.getFilepath());
                group.setFiles(new HashMap<FileType,String>());
                groups.put(key, group);
            }
            
            DomainUtils.setFilepath(group, fileType, filepath);
        }

        return new ArrayList<>(groups.values());
    }

    public void sortChildrenByName(TreeNode treeNode) throws Exception {
        if (treeNode==null || !treeNode.hasChildren()) return;
        final Map<Long,DomainObject> map = DomainUtils.getMapById(domainDAL.getChildren(ownerKey, treeNode));
        Collections.sort(treeNode.getChildren(), new Comparator<Reference>() {
            @Override
            public int compare(Reference o1, Reference o2) {
                DomainObject d1 = map.get(o1.getTargetId());
                DomainObject d2 = map.get(o2.getTargetId());
                String d1Name = d1==null?null:d1.getName();
                String d2Name = d2==null?null:d2.getName();
                return ComparisonChain.start()
                        .compare(d1Name, d2Name, Ordering.natural().nullsLast())
                        .result();
            }
        });
        domainDAL.save(ownerKey, treeNode);
    }

    public Set<Long> getReprocessLsmIds() {
        return reprocessLsmIds;
    }
    public Set<Long> getChangedLsmIds() {
        return changedLsmIds;
    }
    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
}
