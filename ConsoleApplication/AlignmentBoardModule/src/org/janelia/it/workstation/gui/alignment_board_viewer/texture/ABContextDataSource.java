package org.janelia.it.workstation.gui.alignment_board_viewer.texture;

import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.RenderableDataSourceI;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.compartments.Compartment;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.alignment_board.util.RenderUtils;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.workstation.model.domain.VolumeImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;

/**
 * Implements the data source against the context of the alignment board.  New read pass each call.
 */
public class ABContextDataSource implements RenderableDataSourceI {

    // This data was manually extracted from some Yoshi/standard-space-size data.  It is named as:
    //   prefix_1822248087368761442_9.mask
    // where '9' above is the label number, the usual mask/chan extensions apply, and the number shown
    // should match the location for the previously-known files fetched from the older pipeline.
    private static final byte NON_RENDER_INTENSITY = (byte) 0f;
    public static final String FILE_SEP = System.getProperty("file.separator");
    private final AlignmentBoardContext context;

    private Sample currentSample; // NOTE: use of this precludes multi-threaded use of this data source!
    private CompartmentSet currentCompartmentSet;
    private AlignmentBoardItem sampleABItem;
    private AlignmentBoardItem compartmentSetABItem;

    private final Logger logger = LoggerFactory.getLogger( ABContextDataSource.class );
    public ABContextDataSource( AlignmentBoardContext context ) {
        this.context = context;
    }

    @Override
    public String getName() {
        return ABContextDataSource.class.getSimpleName() + " Data Source";
    }

    @Override
    public Collection<MaskChanRenderableData> getRenderableDatas() {
        logger.debug( "Getting renderable datas." );
        Collection<MaskChanRenderableData> rtnVal = new ArrayList<>();

        int nextTranslatedNum = 1;

        int liveFileCount = 0;
        DomainModel domainModel = DomainMgr.getDomainMgr().getModel();

        for ( AlignmentBoardItem alignmentBoardItem : context.getAlignmentBoardItems() ) {
            DomainObject dobj = domainModel.getDomainObject(alignmentBoardItem.getTarget());
            if (dobj instanceof Sample) {                
                currentSample = (Sample)dobj;
                sampleABItem = alignmentBoardItem;
                currentCompartmentSet = null;

                MaskChanRenderableData containerRenderable = getNonRenderingRenderableData(dobj, alignmentBoardItem);

                rtnVal.add( containerRenderable );

                Collection<AlignmentBoardItem> childItems = alignmentBoardItem.getChildren();
                if ( childItems != null ) {
                    for ( AlignmentBoardItem childItem: childItems ) {
                        DomainObject childDobj = domainModel.getDomainObject(childItem.getTarget());
                        if ( childDobj instanceof NeuronFragment) {
                            liveFileCount += getNeuronFragmentRenderableData( rtnVal, nextTranslatedNum++, false, childItem );
                        }
                        else if ( childDobj instanceof VolumeImage) {
                            VolumeImage image = (VolumeImage)childDobj;
                            liveFileCount += getRenderableData( rtnVal, nextTranslatedNum++, image, childItem );
                        }
                    }
                }
            }
            else if ( dobj instanceof NeuronFragment) {
                liveFileCount += getNeuronFragmentRenderableData(rtnVal, nextTranslatedNum, false, alignmentBoardItem);
            }
            else if ( dobj instanceof CompartmentSet) {
                currentSample = null;
                currentCompartmentSet = (CompartmentSet)dobj;
                compartmentSetABItem = alignmentBoardItem;

                MaskChanRenderableData containerRenderable = getNonRenderingRenderableData(dobj, alignmentBoardItem);

                rtnVal.add( containerRenderable );

                AlignmentContext targetAlignmentContext = context.getAlignmentContext();
                AlignmentContext compartmentAlignmentContext = new AlignmentContext(); 
                compartmentAlignmentContext.setAlignmentSpace(currentCompartmentSet.getAlignmentSpace());
                compartmentAlignmentContext.setImageSize(currentCompartmentSet.getImageSize());
                compartmentAlignmentContext.setOpticalResolution(currentCompartmentSet.getOpticalResolution());
                String basePath = currentCompartmentSet.getFilepath();

                Collection<AlignmentBoardItem> childItems = alignmentBoardItem.getChildren();
                if ( childItems != null ) {
                    for ( AlignmentBoardItem item: childItems ) {
                        DomainObject childDobj = domainModel.getDomainObject(item.getTarget());
                        if ( childDobj instanceof Compartment) {
                            Compartment compartment = (Compartment)childDobj;
                            if ( targetAlignmentContext.equals( compartmentAlignmentContext ) ) {
                                liveFileCount += getRenderableData(rtnVal, nextTranslatedNum++, true, basePath, item);
                            }
                        }
                    }
                }

            }
            else if ( dobj instanceof Compartment) {
                liveFileCount += getRenderableData(rtnVal, nextTranslatedNum, true, null, alignmentBoardItem);
            }
        }

        //  Prevent user from seeing "forever working" indicator when there is nothing to see.
        if ( liveFileCount == 0  &&  context.getAlignmentBoardItems().size() > 0 ) {
            String message = "No mask or channel file sets found.  Nothing to display.";
            logger.error( message );
            throw new RuntimeException( message );
        }

        return rtnVal;
    }

    //------------------------------------------------------------HELPERS

    /**
     * Creates a container renderable which will not be shown.  It exists for its children's reference.
     * @return the renderable being created.
     */
    private MaskChanRenderableData getNonRenderingRenderableData(DomainObject dobj, AlignmentBoardItem item) {
        RenderableBean containerDataBean = new RenderableBean();
        containerDataBean.setLabelFileNum(0);
        containerDataBean.setTranslatedNum(0); // Always zero for any sample.
        containerDataBean.setRgb(
                new byte[]{
                        NON_RENDER_INTENSITY, NON_RENDER_INTENSITY, NON_RENDER_INTENSITY, RenderMappingI.NON_RENDERING
                }
        );
        containerDataBean.setReference(item.getTarget());
        containerDataBean.setId(dobj.getId());
        containerDataBean.setName(dobj.getName());

        MaskChanRenderableData containerRenderable = new MaskChanRenderableData();
        containerRenderable.setBean(containerDataBean);
        containerRenderable.setCompartment(false);
        return containerRenderable;
    }

    /**
     * Create the common part of the renderable data.
     *
     * @param maskChanRenderableDatas to this collection will be added the new renderable data.
     * @param nextTranslatedNum will be the target render-method id for this data
     * @param isCompartment for special treatment of compartments.
     * @param item being wrapped.
     * @return number of correct files found for this "translated number".
     */
    private int getRenderableData(
            Collection<MaskChanRenderableData> maskChanRenderableDatas,
            int nextTranslatedNum,
            boolean isCompartment,
            String basePath,
            AlignmentBoardItem item) {
        DomainObject dobj = RenderUtils.getObjectForItem(item);
        RenderableBean renderableBean = createRenderableBean( nextTranslatedNum, isCompartment, item );
        MaskChanRenderableData nfRenderable = new MaskChanRenderableData();
        nfRenderable.setBean( renderableBean );
        nfRenderable.setCompartment( isCompartment );
        
        String maskPath = getMaskPath( dobj );
        if (maskPath != null  &&  basePath != null) {
            maskPath = basePath + FILE_SEP + maskPath;
        }
        nfRenderable.setMaskPath( maskPath );
        String channelPath = getChanPath( dobj );
        nfRenderable.setChannelPath( channelPath );

        int liveFileCount = getCorrectFilesFoundCount(
                "" + renderableBean.getId(), maskPath, channelPath
        );

        maskChanRenderableDatas.add( nfRenderable );
        return liveFileCount;
    }

    /**
     * Create a neuron-fragment-specific renderable data.
     *
     * @param maskChanRenderableDatas to this collection will be added the new
     * renderable data.
     * @param nextTranslatedNum will be the target render-method id for this
     * data
     * @param isCompartment for special treatment of compartments.
     * @param item being wrapped.
     * @return number of correct files found for this "translated number".
     */
    private int getNeuronFragmentRenderableData(
            Collection<MaskChanRenderableData> maskChanRenderableDatas,
            int nextTranslatedNum,
            boolean isCompartment,
            AlignmentBoardItem item) {
        
        DomainObject dobj = RenderUtils.getObjectForItem(item);
        String basePath = ((NeuronFragment)dobj).getFilepath();
        return getRenderableData(maskChanRenderableDatas, nextTranslatedNum, isCompartment, basePath, item);
    }

    private int getRenderableData(
        Collection<MaskChanRenderableData> maskChanRenderableDatas,
        int nextTranslatedNum,
        VolumeImage volumeImage,
        AlignmentBoardItem item
    ) {
        RenderableBean renderableBean = new RenderableBean();
        renderableBean.setInvertedY(false);
        renderableBean.setLabelFileNum(nextTranslatedNum);
        renderableBean.setTranslatedNum(nextTranslatedNum);
        renderableBean.setType("Reference");     //todo move this to EntityConstants
        renderableBean.setName(volumeImage.getName()); //???
        renderableBean.setId(item.getTarget().getTargetId());
        renderableBean.setReference(item.getTarget());
        setAppearance( false, item, renderableBean );
        MaskChanRenderableData data = new MaskChanRenderableData();
        data.setBean( renderableBean );
        data.setCompartment( true ); // For render characteristics

        String maskPath = volumeImage.getMask3dImageFilepath();
        String channelPath = volumeImage.getChan3dImageFilepath();
        if ( maskPath == null || channelPath == null ) {
            logger.warn("No mask and/or channel pat for reference {}:{}.", renderableBean.getName(), renderableBean.getId() );
        }
        data.setChannelPath( channelPath );
        data.setMaskPath( maskPath );

        maskChanRenderableDatas.add( data );
        int liveFileCount = getCorrectFilesFoundCount(
                "" + renderableBean.getId(), maskPath, channelPath
        );

        return liveFileCount;
    }

    /**
     * Checks if the mask paths make sense (non null), and if so, returns 1 for the count.
     */
    private int getCorrectFilesFoundCount(String id, String maskPath, String channelPath) {
        int rtnVal = 0;
        if ( maskPath == null  ||  channelPath == null ) {
            logger.warn( "{} has either no channel or no mask path.", id );
        }
        else {
            rtnVal ++;
        }

        return rtnVal;
    }

    private RenderableBean createRenderableBean( int translatedNum, boolean isCompartment, AlignmentBoardItem item ) {
        DomainObject dobj = RenderUtils.getObjectForItem(item);
        int maskIndex = getOriginalMaskNumber(dobj);
        logger.debug(
                "Creating Renderable Bean for: " + item.getTarget().getTargetClassName() + "/" + item.getTarget().getTargetId() + " original index=" + maskIndex +
                        " new index=" + translatedNum
        );

        RenderableBean renderableBean = new RenderableBean();
        renderableBean.setLabelFileNum(maskIndex);
        renderableBean.setTranslatedNum(translatedNum);
        renderableBean.setName(dobj.getName());
        renderableBean.setId(dobj.getId());
        renderableBean.setReference(item.getTarget());
        renderableBean.setType(item.getTarget().getTargetClassName());
        if ( isCompartment ) {
            renderableBean.setInvertedY( false );
        }
        else {
            renderableBean.setInvertedY( true );
        }

        // If a simplistic value has been set, change it.  Otherwise, would be something more specific.
//        if ( renderableBean.getType().equals( EntityConstants.TYPE_ALIGNED_ITEM ) ) {
//            renderableBean.setType(EntityConstants.TYPE_NEURON_FRAGMENT);
//        }
        setAppearance(isCompartment, item, renderableBean);

        return renderableBean;
    }
    
    private int getOriginalMaskNumber(DomainObject domainObj) {
        int rtnVal = -1;
        if (domainObj instanceof NeuronFragment) {
            NeuronFragment nf = (NeuronFragment)domainObj;
            rtnVal = nf.getNumber();
        }
        else if (domainObj instanceof Compartment) {
            Compartment compartment = (Compartment)domainObj;
            rtnVal = compartment.getNumber();
        }
        return rtnVal;
    }
    
    private String getMaskPath(DomainObject domainObj) {        
        return getTypedPath(domainObj, FileType.MaskFile);
    }
    
    private String getChanPath(DomainObject domainObj) {
        return getTypedPath(domainObj, FileType.ChanFile);
    }
    
    private String getTypedPath(DomainObject domainObj, FileType type) {
        String rtnVal = null;
        Map<FileType,String> files = null;
        if (domainObj instanceof NeuronFragment) {
             files = ((NeuronFragment)domainObj).getFiles();
        }
        else if (domainObj instanceof Compartment) {
             files = ((Compartment)domainObj).getFiles();            
        }
        if (files != null) {
            rtnVal = files.get(type);
        }
        return rtnVal;
    }
    
    private void setAppearance(boolean isCompartment, AlignmentBoardItem item, RenderableBean renderableBean) {
        // See to the appearance.
        Color renderColor = RenderUtils.getColorFromRGBStr(item.getColor());
        if ( renderColor == null ) {
            // Second chance at the render color, from the item parent.
            renderColor = getParentColor();
        }

        if ( renderColor == null ) {
            // If visible, leave RGB as null, and allow downstream automated-color to take place.
            // Otherwise, if not visible, ensure that the bean has a non-render setting.
            if ( ! item.isVisible() ) {
                byte[] rgb = new byte[ 4 ];
                rgb[ 0 ] = 0;
                rgb[ 1 ] = 0;
                rgb[ 2 ] = 0;
                rgb[ 3 ] = RenderMappingI.NON_RENDERING;
                renderableBean.setRgb(rgb);
            }
            else if ( RenderUtils.isPassthroughRendering(item)  ||  parentIsPassthrough() ) {
                byte[] rgb = new byte[ 4 ];
                setPassthroughRGB(rgb);
                renderableBean.setRgb(rgb);
            }
            else if ( isCompartment ) {
                Compartment compartment = (Compartment)RenderUtils.getObjectForItem(item);
                byte[] rgb = new byte[ 4 ];
                if ( RenderUtils.isPassthroughRendering(compartmentSetABItem) ) {
                    setPassthroughRGB( rgb );
                }
                else {
                    Color color = RenderUtils.getColorFromRGBStr(compartment.getColor());
                    int[] rawColor = new int[] { color.getRed(), color.getGreen(), color.getBlue() };
                    for ( int i = 0; i < 3; i++ ) {
                        rgb[ i ] = (byte)rawColor[ i ];
                    }
                    rgb[ 3 ] = RenderMappingI.COMPARTMENT_RENDERING;
                    renderableBean.setRgb( rgb );
                }
            }
        }
        else {
            logger.debug( "Render color is {} for {}.", renderColor, item.getTarget().getTargetId() );
            // A Neuron Color was set, but the neuron could still be "turned off" for render.
            byte[] rgb = new byte[ 4 ];
            setRgbFromColor(renderColor, rgb);
            byte renderMethod = RenderMappingI.FRAGMENT_RENDERING;
            if ( RenderUtils.isPassthroughRendering(item) ) {
                renderMethod = RenderMappingI.PASS_THROUGH_RENDERING;
            }
            if ( ! item.isVisible() ) {
                renderMethod = RenderMappingI.NON_RENDERING;
            }
            else if ( isCompartment ) {
                // Allow an override for all compartments, from parent.
                if ( parentIsPassthrough() ) {
                    setPassthroughRGB( rgb );
                }
                else if ( getParentColor() != null ) {
                    setRgbFromColor(getParentColor(), rgb);
                    renderMethod = RenderMappingI.COMPARTMENT_RENDERING;
                }
                else {
                    renderMethod = RenderMappingI.COMPARTMENT_RENDERING;
                }
            }
            rgb[ 3 ] = renderMethod;
            renderableBean.setRgb(rgb);
        }
    }

    private Color getParentColor() {
        Color renderColor = null;
        if ( currentSample != null ) {
            renderColor = getParentColor( sampleABItem );
        }
        else if ( currentCompartmentSet != null ) {
            renderColor = getParentColor( compartmentSetABItem );
        }
        return renderColor;
    }

    private void setRgbFromColor(Color renderColor, byte[] rgb) {
        rgb[ 0 ] = (byte)renderColor.getRed();
        rgb[ 1 ] = (byte)renderColor.getGreen();
        rgb[ 2 ] = (byte)renderColor.getBlue();
    }

    private void setPassthroughRGB(byte[] rgb) {
        rgb[ 0 ] = 0;
        rgb[ 1 ] = 0;
        rgb[ 2 ] = 0;
        rgb[ 3 ] = RenderMappingI.PASS_THROUGH_RENDERING;
    }

    private Color getParentColor( AlignmentBoardItem parent ) {
        Color renderColor = null;
        if ( parent != null  &&  parent.getColor() != null ) {
            renderColor = RenderUtils.getColorFromRGBStr(parent.getColor());
        }
        return renderColor;
    }

    private boolean parentIsPassthrough() {
        return (compartmentSetABItem != null  &&   RenderUtils.isPassthroughRendering(compartmentSetABItem))  ||
               (sampleABItem != null  &&  RenderUtils.isPassthroughRendering(sampleABItem));
    }
    
}
