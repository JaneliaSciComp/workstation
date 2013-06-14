package org.janelia.it.FlyWorkstation.model.domain;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.MaskedVolume;
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
    private MaskedVolume maskedVolume;

    public Sample(RootedEntity entity) {
        super(entity);
    }
    
    public List<AlignmentContext> getAvailableAlignmentContexts() throws Exception  {

        log.debug("Loading alignment contexts for sample (id={})",getName(),getId());
        List<AlignmentContext> contexts = new ArrayList<AlignmentContext>();
        
        ModelMgr.getModelMgr().loadLazyEntity(getInternalEntity(), false);

        for(RootedEntity pipelineRun : getInternalRootedEntity().getChildrenOfType(EntityConstants.TYPE_PIPELINE_RUN)) {
            log.debug("Checking pipeline run '{}' (id={})",pipelineRun.getName(),pipelineRun.getEntityId());
            ModelMgr.getModelMgr().loadLazyEntity(pipelineRun.getEntity(), false);
            
            for(RootedEntity pipelineResult : pipelineRun.getChildrenForAttribute(EntityConstants.ATTRIBUTE_RESULT)) {
                if (pipelineResult.getEntity().getValueByAttributeName(EntityConstants.TYPE_ALIGNMENT_SPACE)!=null) {
                
                    log.debug("  Checking '{}'",pipelineResult.getName());    
                    ModelMgr.getModelMgr().loadLazyEntity(pipelineResult.getEntity(), false);
                                        
                    if (pipelineResult.getType().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
                        RootedEntity supportingFiles = pipelineResult.getChildForAttribute(EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
                        ModelMgr.getModelMgr().loadLazyEntity(supportingFiles.getEntity(), false);
                        
                        for(RootedEntity alignedVolume : supportingFiles.getChildrenOfType(EntityConstants.TYPE_IMAGE_3D)) {
                            log.debug("    Checking aligned volume '{}' (id={})",alignedVolume.getName(),alignedVolume.getEntityId());
                            ModelMgr.getModelMgr().loadLazyEntity(alignedVolume.getEntity(), false);
                            
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
        ModelMgr.getModelMgr().loadLazyEntity(getInternalEntity(), false);
        
        this.neuronSet = new ArrayList<Neuron>();
        
        String matchedAlignmentSpace = null;
        String matchedOpticalRes = null;
        String matchedPixelRes = null;
        RootedEntity volume = null;
        RootedEntity separation = null;
        RootedEntity fragmentCollection = null;
        
        for(RootedEntity pipelineRun : getInternalRootedEntity().getChildrenOfType(EntityConstants.TYPE_PIPELINE_RUN)) {
            log.debug("Checking pipeline run '{}' (id={})",pipelineRun.getName(),pipelineRun.getEntityId());
            ModelMgr.getModelMgr().loadLazyEntity(pipelineRun.getEntity(), false);
            
            for(RootedEntity pipelineResult : pipelineRun.getChildrenForAttribute(EntityConstants.ATTRIBUTE_RESULT)) {
                String alignmentSpaceName = pipelineResult.getEntity().getValueByAttributeName(EntityConstants.TYPE_ALIGNMENT_SPACE);
                log.debug("  Checking '{}'",pipelineResult.getName());    
                ModelMgr.getModelMgr().loadLazyEntity(pipelineResult.getEntity(), false);
                
                if (alignmentSpaceName!=null && alignmentSpaceName.equals(alignmentContext.getAlignmentSpaceName())) {
                    matchedAlignmentSpace = alignmentContext.getAlignmentSpaceName();
                    
                    if (pipelineResult.getType().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
                        
                        RootedEntity supportingFiles = pipelineResult.getChildForAttribute(EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
                        ModelMgr.getModelMgr().loadLazyEntity(supportingFiles.getEntity(), false);
                        
                        for(RootedEntity alignedVolume : supportingFiles.getChildrenOfType(EntityConstants.TYPE_IMAGE_3D)) {
                            log.debug("    Checking aligned volume '{}' (id={})",alignedVolume.getName(),alignedVolume.getEntityId());
                            ModelMgr.getModelMgr().loadLazyEntity(alignedVolume.getEntity(), false);
                            
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
                        
                        for(RootedEntity sep : pipelineResult.getChildrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                            String opticalRes = sep.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
                            String pixelRes = sep.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
                            log.debug("    Checking neuron separation '{}' (id={})",sep.getName(),sep.getEntityId());
                            ModelMgr.getModelMgr().loadLazyEntity(sep.getEntity(), false);
                            
                            if (opticalRes!=null && opticalRes.equals(alignmentContext.getOpticalResolution())) {
                                matchedOpticalRes = opticalRes;
                                if (pixelRes!=null && pixelRes.equals(alignmentContext.getPixelResolution())) {
                                    log.debug("      Accepted neuron separation (id={})",sep.getEntityId());  
                                    matchedPixelRes = pixelRes;
                                    separation = sep;
                                    fragmentCollection = sep.getChildOfType(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (volume!=null) {
            RootedEntity fast3dImage = volume.getChildForAttribute(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE);
            if (fast3dImage != null) {
                imagePathFast3d = fast3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                log.debug("Got fast 3d image: {}",imagePathFast3d);
            }
        }
        
        if (separation!=null) {
            maskedVolume = new MaskedVolume(separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
            if (maskedVolume!=null) {
                log.debug("Got masked volume: {}",maskedVolume.getSignalLabelPath());
            }
            
            if (fragmentCollection!=null) {
                ModelMgr.getModelMgr().loadLazyEntity(fragmentCollection.getEntity(), false);
                for(RootedEntity neuronFragment : fragmentCollection.getChildrenOfType(EntityConstants.TYPE_NEURON_FRAGMENT)) {
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

    @Override
    public String getFast3dImageFilepath() {
        return imagePathFast3d;
    }

    public List<Neuron> getNeuronSet() {
        return neuronSet;
    }

    @Override
    public MaskedVolume getMaskedVolume() {
        return maskedVolume;
    }

}
