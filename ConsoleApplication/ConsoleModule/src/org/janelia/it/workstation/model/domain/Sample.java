package org.janelia.it.workstation.model.domain;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper for Sample entities. Provides access to common features of Samples, and loads separated Neurons as 
 * its children.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class Sample extends AlignedEntityWrapper implements Viewable2d, Viewable3d, Viewable4d {

    private static final Logger log = LoggerFactory.getLogger(Sample.class);
    
    private String imagePathFast3d;
    private List<Neuron> neuronSet;
    private VolumeImage reference;
    private org.janelia.it.workstation.model.viewer.MaskedVolume maskedVolume;

    public Sample(org.janelia.it.workstation.model.entity.RootedEntity entity) {
        super(entity);
    }
    
    public List<AlignmentContext> getAvailableAlignmentContexts() throws Exception  {

        log.debug("Loading alignment contexts for sample (id={})",getName(),getId());
        List<AlignmentContext> contexts = new ArrayList<AlignmentContext>();
        
        org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(getInternalEntity(), false);

        for(org.janelia.it.workstation.model.entity.RootedEntity pipelineRun : getInternalRootedEntity().getChildrenOfType(EntityConstants.TYPE_PIPELINE_RUN)) {
            log.debug("Checking pipeline run '{}' (id={})",pipelineRun.getName(),pipelineRun.getEntityId());
            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(pipelineRun.getEntity(), false);
            
            for(org.janelia.it.workstation.model.entity.RootedEntity pipelineResult : pipelineRun.getChildrenForAttribute(EntityConstants.ATTRIBUTE_RESULT)) {
                if (pipelineResult.getEntity().getValueByAttributeName(EntityConstants.TYPE_ALIGNMENT_SPACE)!=null) {
                
                    log.debug("  Checking '{}'",pipelineResult.getName());    
                    org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(pipelineResult.getEntity(), false);
                                        
                    if (pipelineResult.getType().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
                        org.janelia.it.workstation.model.entity.RootedEntity supportingFiles = pipelineResult.getChildForAttribute(EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
                        org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(supportingFiles.getEntity(), false);
                        
                        for(org.janelia.it.workstation.model.entity.RootedEntity alignedVolume : supportingFiles.getChildrenOfType(EntityConstants.TYPE_IMAGE_3D)) {
                            log.debug("    Checking aligned volume '{}' (id={})",alignedVolume.getName(),alignedVolume.getEntityId());
                            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(alignedVolume.getEntity(), false);
                            
                            String alignmentSpaceName = alignedVolume.getValueByAttributeName(EntityConstants.TYPE_ALIGNMENT_SPACE);
                            String opticalResolution = alignedVolume.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
                            String pixelResolution = alignedVolume.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
                            if (alignmentSpaceName!=null && opticalResolution!=null && pixelResolution!=null) {

                                log.debug("    Found alignment {} on image with id={}",alignmentSpaceName,alignedVolume.getEntityId());
                                contexts.add(new AlignmentContext(alignmentSpaceName, opticalResolution, pixelResolution));
                            }
                        }
                    }
                }
            }
        }

        return contexts;
    }

    @Override
    public void loadContextualizedChildren(AlignmentContext alignmentContext) throws Exception {
        
        log.debug("Loading contextualized children for sample '{}' (id={})",getName(),getId());
        
        initChildren();
        org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(getInternalEntity(), false);
        
        this.neuronSet = new ArrayList<Neuron>();
        
        String matchedAlignmentSpace = null;
        String matchedOpticalRes = null;
        String matchedPixelRes = null;
        org.janelia.it.workstation.model.entity.RootedEntity volume = null;
        org.janelia.it.workstation.model.entity.RootedEntity separation = null;
        org.janelia.it.workstation.model.entity.RootedEntity fragmentCollection = null;
        
        for(org.janelia.it.workstation.model.entity.RootedEntity pipelineRun : getInternalRootedEntity().getChildrenOfType(EntityConstants.TYPE_PIPELINE_RUN)) {
            log.debug("Checking pipeline run '{}' (id={})",pipelineRun.getName(),pipelineRun.getEntityId());
            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(pipelineRun.getEntity(), false);
            
            for(org.janelia.it.workstation.model.entity.RootedEntity pipelineResult : pipelineRun.getChildrenForAttribute(EntityConstants.ATTRIBUTE_RESULT)) {
                String alignmentSpaceName = pipelineResult.getEntity().getValueByAttributeName(EntityConstants.TYPE_ALIGNMENT_SPACE);
                log.debug("  Checking '{}'",pipelineResult.getName());    
                org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(pipelineResult.getEntity(), false);
                
                if (alignmentSpaceName!=null && alignmentSpaceName.equals(alignmentContext.getAlignmentSpaceName())) {
                    matchedAlignmentSpace = alignmentContext.getAlignmentSpaceName();
                    
                    if (pipelineResult.getType().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
                        
                        org.janelia.it.workstation.model.entity.RootedEntity supportingFiles = pipelineResult.getChildForAttribute(EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
                        org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(supportingFiles.getEntity(), false);
                        
                        for(org.janelia.it.workstation.model.entity.RootedEntity alignedVolume : supportingFiles.getChildrenOfType(EntityConstants.TYPE_IMAGE_3D)) {
                            log.debug("    Checking aligned volume '{}' (id={})",alignedVolume.getName(),alignedVolume.getEntityId());
                            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(alignedVolume.getEntity(), false);
                            
                            String opticalRes = alignedVolume.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
                            if (opticalRes!=null && opticalRes.equals(alignmentContext.getOpticalResolution())) {
                                matchedOpticalRes = opticalRes;
                                String pixelRes = alignedVolume.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
                                if (pixelRes!=null && pixelRes.equals(alignmentContext.getPixelResolution())) {
                                    log.debug("      Accepted aligned result (id={})",alignedVolume.getEntityId());    
                                    matchedPixelRes = pixelRes;
                                    volume = alignedVolume;
                                }
                            }
                        }
                        
                        for(org.janelia.it.workstation.model.entity.RootedEntity sep : pipelineResult.getChildrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                            String opticalRes = sep.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
                            String pixelRes = sep.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
                            log.debug("    Checking neuron separation '{}' (id={})",sep.getName(),sep.getEntityId());
                            org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(sep.getEntity(), false);
                            
                            if (opticalRes!=null && opticalRes.equals(alignmentContext.getOpticalResolution())) {
                                matchedOpticalRes = opticalRes;
                                if (pixelRes!=null && pixelRes.equals(alignmentContext.getPixelResolution())) {
                                	
                                	// This code runs every time we find an acceptable neuron separation, and each time
                                	// it overwrites the selected entities, so that the final neuron separation that we
                                	// accept is the one we end up using.
                                	
                                    log.debug("      Accepted neuron separation (id={})",sep.getEntityId());  
                                    matchedPixelRes = pixelRes;
                                    separation = sep;
                                    fragmentCollection = sep.getChildOfType(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);

                                    org.janelia.it.workstation.model.entity.RootedEntity sepSupportingFiles = sep.getChildForAttribute(EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
                                    org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(sepSupportingFiles.getEntity(), false);
                                    
                                    // Reset the reference, so that we can make sure that we get the one for this neuron separation
                                    reference = null;
                    	        	for(org.janelia.it.workstation.model.entity.RootedEntity image3d : sepSupportingFiles.getChildrenOfType(EntityConstants.TYPE_IMAGE_3D)) {
                    	        		if (image3d.getName().startsWith("Reference.")) {
                    	        			reference = new VolumeImage(image3d);
                    	        		};
                    	        	}
                    	        	
                    	        	if (reference==null) {
                    	        		log.warn("Neuron separation has no reference! This should never happen.");
                    	        	}
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (reference!=null && reference.getMask3dImageFilepath()!=null) {
            log.debug("Got reference mask: {}",reference.getMask3dImageFilepath());
            log.debug("Got reference chan: {}",reference.getChan3dImageFilepath());
        	addChild(reference);
        }
        
        if (volume!=null) {
            org.janelia.it.workstation.model.entity.RootedEntity fast3dImage = volume.getChildForAttribute(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE);
            if (fast3dImage != null) {
                imagePathFast3d = fast3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                log.debug("Got fast 3d image: {}",imagePathFast3d);
            }
        }
        
        if (separation!=null) {
            maskedVolume = new org.janelia.it.workstation.model.viewer.MaskedVolume(separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
            if (maskedVolume!=null) {
                log.debug("Got masked volume: {}",maskedVolume.getSignalLabelPath());
            }
            
            if (fragmentCollection!=null) {
                org.janelia.it.workstation.api.entity_model.management.ModelMgr.getModelMgr().loadLazyEntity(fragmentCollection.getEntity(), false);
                for(org.janelia.it.workstation.model.entity.RootedEntity neuronFragment : fragmentCollection.getChildrenOfType(EntityConstants.TYPE_NEURON_FRAGMENT)) {
                    Neuron neuron = new Neuron(neuronFragment);
                    neuronSet.add(neuron);
                    addChild(neuron);
                }
                log.debug("Got {} neurons",neuronSet.size());
            }
        }
        
        if (matchedAlignmentSpace!=null && matchedOpticalRes!=null && matchedPixelRes!=null) {
            setAlignmentContext(new AlignmentContext(matchedAlignmentSpace, matchedOpticalRes, matchedPixelRes));
            // Sanity check
            if (!alignmentContext.canDisplay(getAlignmentContext())) {
                throw new IllegalStateException("Sample's alignment does not match the context: "+alignmentContext);
            }
        }
    }
    
    public String getDataSetIdentifier() {
        return getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
    }

    public String getChannelSpecification() {
        return getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
    }

    public String getTilingPattern() {
        return getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_TILING_PATTERN);
    }

    @Override
    public String get2dImageFilepath() {
        return getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
    }
    
    @Override
    public String get3dImageFilepath() {
        return getInternalEntity().getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
    }

    public String getFast3dImageFilepath() {
        return imagePathFast3d;
    }

    public List<Neuron> getNeuronSet() {
        return neuronSet;
    }
    
    public VolumeImage getReference() {
    	return reference;
    }

    @Override
    public org.janelia.it.workstation.model.viewer.MaskedVolume getMaskedVolume() {
        return maskedVolume;
    }

}
