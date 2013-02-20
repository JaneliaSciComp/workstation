package org.janelia.it.FlyWorkstation.model.domain;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
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

    @Override
    public void loadContextualizedChildren(AlignmentContext alignmentContext) throws Exception {
        
        initChildren();
        
        this.neuronSet = new ArrayList<Neuron>();
        
        ModelMgr.getModelMgr().loadLazyEntity(getInternalEntity(), true);

        String matchedAlignmentSpace = null;
        String matchedOpticalRes = null;
        String matchedPixelRes = null;
        RootedEntity volume = null;
        RootedEntity separation = null;
        RootedEntity fragmentCollection = null;
        
        for(RootedEntity pipelineRun : getInternalRootedEntity().getChildrenOfType(EntityConstants.TYPE_PIPELINE_RUN)) {
            log.info("Checking "+pipelineRun.getName()+" (id="+pipelineRun.getId()+")");
            
            for(RootedEntity pipelineResult : pipelineRun.getChildrenForAttribute(EntityConstants.ATTRIBUTE_RESULT)) {
                String alignmentSpaceName = pipelineResult.getEntity().getValueByAttributeName(EntityConstants.TYPE_ALIGNMENT_SPACE);
                log.info("  Checking "+pipelineResult.getName()+" in alignment space "+alignmentSpaceName);    
                
                if (alignmentContext==null || alignmentContext.getAlignmentSpaceName()==null) {
                    if (pipelineResult.getType().equals(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {
                        log.info("  Accepted sample processing result"+pipelineResult.getId());    
                        
                        volume = pipelineResult.getChildForAttribute(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                        RootedEntity sep = pipelineResult.getLatestChildOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
                        if (sep!=null) {
                            separation = sep;
                            fragmentCollection = pipelineResult.getChildOfType(EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
                        }
                    }
                }
                else {
                    if (alignmentSpaceName!=null && alignmentSpaceName.equals(alignmentContext.getAlignmentSpaceName())) {
                        matchedAlignmentSpace = alignmentContext.getAlignmentSpaceName();
                        
                        if (pipelineResult.getType().equals(EntityConstants.TYPE_ALIGNMENT_RESULT)) {
                            log.info("  Found aligned result"+pipelineResult.getId());    
                            
                            RootedEntity supportingFiles = pipelineResult.getChildForAttribute(EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
                            
                            for(RootedEntity alignedVolume : supportingFiles.getChildrenOfType(EntityConstants.TYPE_IMAGE_3D)) {
                                String opticalRes = alignedVolume.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
                                if (opticalRes!=null && opticalRes.equals(alignmentContext.getOpticalResolution())) {
                                    matchedOpticalRes = opticalRes;
                                    String pixelRes = alignedVolume.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
                                    if (pixelRes!=null && pixelRes.equals(alignmentContext.getPixelResolution())) {
                                        matchedPixelRes = pixelRes;
                                        volume = alignedVolume;
                                    }
                                }
                            }
                            
                            for(RootedEntity sep : pipelineResult.getChildrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                                String opticalRes = sep.getValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION);
                                String pixelRes = sep.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION);
                                log.info("    Checking neuron separation "+sep.getId()+" (opticalRes="+opticalRes+",pixelRes="+pixelRes+")");
                                
                                if (opticalRes!=null && opticalRes.equals(alignmentContext.getOpticalResolution())) {
                                    matchedOpticalRes = opticalRes;
                                    if (pixelRes!=null && pixelRes.equals(alignmentContext.getPixelResolution())) {
                                        log.info("      Accepted neuron separation");
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
        }
        
        if (volume!=null) {
            RootedEntity fast3dImage = volume.getChildForAttribute(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE);
            if (fast3dImage != null) {
                imagePathFast3d = fast3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
            }
        }
        
        if (separation!=null) {
            maskedVolume = new MaskedVolume(separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
            log.info("Got masked volume: "+maskedVolume);
            
            if (fragmentCollection!=null) {
                for(RootedEntity neuronFragment : fragmentCollection.getChildrenOfType(EntityConstants.TYPE_NEURON_FRAGMENT)) {
                    Neuron neuron = new Neuron(neuronFragment);
                    neuron.setParent(this);
                    neuronSet.add(neuron);
                    addChild(neuron);
                }
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

    public List<Neuron> getNeuronSet() throws Exception {
        return neuronSet;
    }

    @Override
    public MaskedVolume getMaskedVolume() {
        return maskedVolume;
    }
}
