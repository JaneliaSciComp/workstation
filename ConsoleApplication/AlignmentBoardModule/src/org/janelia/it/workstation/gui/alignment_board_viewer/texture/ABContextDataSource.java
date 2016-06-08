package org.janelia.it.workstation.gui.alignment_board_viewer.texture;

import org.janelia.it.workstation.gui.alignment_board.util.ABCompartment;
import org.janelia.it.workstation.gui.alignment_board.util.ABCompartmentSet;
import org.janelia.it.workstation.gui.alignment_board.util.ABItem;
import org.janelia.it.workstation.gui.alignment_board.util.ABNeuronFragment;
import org.janelia.it.workstation.gui.alignment_board.util.ABSample;
import org.janelia.it.workstation.gui.alignment_board.util.RenderUtils;
import org.janelia.it.workstation.gui.alignment_board.util.ABReferenceChannel;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.RenderableDataSourceI;
import org.janelia.it.workstation.gui.alignment_board_viewer.creation.DomainHelper;
import org.janelia.it.workstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.workstation.gui.alignment_board_viewer.CompatibilityChecker;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentContext;
import org.janelia.it.workstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;
//import org.janelia.it.workstation.model.domain.VolumeImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.*;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;

/**
 * Implements the data source against the context of the alignment board.  New read pass each call.
 */
public class ABContextDataSource implements RenderableDataSourceI {

    // This data was manually extracted from some Yoshi/standard-space-size data.  It is named as:
    //   prefix_1822248087368761442_9.mask
    // where '9' above is the label number, the usual mask/chan extensions apply, and the number shown
    // should match the location for the previously-known files fetched from the older pipeline.
    private static final byte NON_RENDER_INTENSITY = (byte) 0f;
    private final AlignmentBoardContext context;

    private ABSample currentSample; // NOTE: use of this precludes multi-threaded use of this data source!
    private ABCompartmentSet currentCompartmentSet;
    private AlignmentBoardItem sampleABItem;
    private AlignmentBoardItem compartmentSetABItem;
    private final DomainHelper domainHelper;

    private final Logger logger = LoggerFactory.getLogger( ABContextDataSource.class );
    public ABContextDataSource( AlignmentBoardContext context ) {
        this.context = context;
        this.domainHelper = new DomainHelper();
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

        for ( AlignmentBoardItem alignmentBoardItem : context.getAlignmentBoardItems() ) {
            ABItem dobj = domainHelper.getObjectForItem(alignmentBoardItem);
            if (dobj instanceof ABSample) {
                currentSample = (ABSample)dobj;
                sampleABItem = alignmentBoardItem;
                currentCompartmentSet = null;

                MaskChanRenderableData containerRenderable = getNonRenderingRenderableData(dobj, alignmentBoardItem);

                rtnVal.add( containerRenderable );

                Collection<AlignmentBoardItem> childItems = alignmentBoardItem.getChildren();
                if ( childItems != null ) {
                    for ( AlignmentBoardItem childItem: childItems ) {
                        ABItem childDobj = domainHelper.getObjectForItem(childItem);
                        if ( childDobj instanceof ABNeuronFragment) {
                            liveFileCount += getNeuronFragmentRenderableData( rtnVal, nextTranslatedNum++, false, childItem );
                        }
                        else if ( childDobj instanceof ABReferenceChannel) {
                            liveFileCount += getReferenceChannelRenderableData( rtnVal, nextTranslatedNum++, childItem );
                        }
                    }
                }
            }
            else if ( dobj instanceof ABNeuronFragment) {
                liveFileCount += getNeuronFragmentRenderableData(rtnVal, nextTranslatedNum, false, alignmentBoardItem);
            }
            else if ( dobj instanceof ABCompartmentSet) {
                currentSample = null;
                currentCompartmentSet = (ABCompartmentSet)dobj;
                compartmentSetABItem = alignmentBoardItem;

                MaskChanRenderableData containerRenderable = getNonRenderingRenderableData(dobj, alignmentBoardItem);

                rtnVal.add( containerRenderable );

                AlignmentContext targetAlignmentContext = context.getAlignmentContext();
                AlignmentContext compartmentAlignmentContext = new AlignmentContext(); 
                compartmentAlignmentContext.setAlignmentSpace(currentCompartmentSet.getAlignmentSpace());
                compartmentAlignmentContext.setImageSize(currentCompartmentSet.getImageSize());
                compartmentAlignmentContext.setOpticalResolution(currentCompartmentSet.getOpticalResolution());

                Collection<AlignmentBoardItem> childItems = alignmentBoardItem.getChildren();
                if ( childItems != null ) {
                    CompatibilityChecker checker = new CompatibilityChecker();
                    for ( AlignmentBoardItem item: childItems ) {
                        ABItem childDobj = domainHelper.getObjectForItem(item);
                        if ( childDobj instanceof ABCompartment) {
                            if ( checker.isEqual(targetAlignmentContext, compartmentAlignmentContext ) ) {
                                liveFileCount += getRenderableData(rtnVal, nextTranslatedNum++, true, item);
                            }
                        }
                    }
                }

            }
            else if ( dobj instanceof ABCompartment) {
                liveFileCount += getRenderableData(rtnVal, nextTranslatedNum, true, alignmentBoardItem);
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
    private MaskChanRenderableData getNonRenderingRenderableData(ABItem dobj, AlignmentBoardItem item) {
        RenderableBean containerDataBean = new RenderableBean();
        containerDataBean.setLabelFileNum(0);
        containerDataBean.setTranslatedNum(0); // Always zero for any sample.
        containerDataBean.setRgb(
                new byte[]{
                        NON_RENDER_INTENSITY, NON_RENDER_INTENSITY, NON_RENDER_INTENSITY, RenderMappingI.NON_RENDERING
                }
        );

        ABItem abItem = domainHelper.getObjectForItem(item);
        containerDataBean.setType(abItem.getType());
        containerDataBean.setItem(abItem);
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
            AlignmentBoardItem item) {
        ABItem dobj = domainHelper.getObjectForItem(item);
        RenderableBean renderableBean = createRenderableBean( nextTranslatedNum, isCompartment, item );
        MaskChanRenderableData nfRenderable = new MaskChanRenderableData();
        nfRenderable.setBean( renderableBean );
        nfRenderable.setCompartment( isCompartment );
        
        String maskPath = dobj.getMaskPath();
        String channelPath = dobj.getChanPath();
        if (isCompartment) {
            if (currentCompartmentSet != null) {
                channelPath = currentCompartmentSet.getChanPath() + File.separator + channelPath;
                maskPath = currentCompartmentSet.getMaskPath() + File.separator + maskPath;
            }

        }

        nfRenderable.setMaskPath( maskPath );
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
        
        return getRenderableData(maskChanRenderableDatas, nextTranslatedNum, isCompartment, item);
    }

    private int getReferenceChannelRenderableData(
        Collection<MaskChanRenderableData> maskChanRenderableDatas,
        int nextTranslatedNum,
        AlignmentBoardItem item
    ) {
        RenderableBean renderableBean = new RenderableBean();
        renderableBean.setInvertedY(false);
        renderableBean.setLabelFileNum(nextTranslatedNum);
        renderableBean.setTranslatedNum(nextTranslatedNum);
        renderableBean.setName(ABReferenceChannel.REF_CHANNEL_TYPE_NAME);
        renderableBean.setType(ABReferenceChannel.REF_CHANNEL_TYPE_NAME);
        setAppearance( false, item, renderableBean );
        MaskChanRenderableData data = new MaskChanRenderableData();
        data.setBean( renderableBean );
        data.setCompartment( true ); // For render characteristics

        Sample referencedSample = (Sample)DomainMgr.getDomainMgr().getModel().getDomainObject(item.getTarget().getObjectRef());
        String maskPath = domainHelper.getRefChannelPath(referencedSample, context.getAlignmentContext());
        String channelPath = domainHelper.getRefMaskPath(referencedSample, context.getAlignmentContext());
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
        ABItem abItem = domainHelper.getObjectForItem(item);
        int maskIndex = getOriginalMaskNumber(abItem);
        logger.debug(
                "Creating Renderable Bean for: " + item.getTarget() + " original index=" + maskIndex +
                        " new index=" + translatedNum
        );

        RenderableBean renderableBean = new RenderableBean();
        renderableBean.setLabelFileNum(maskIndex);
        renderableBean.setTranslatedNum(translatedNum);
        renderableBean.setName(abItem.getName());
        renderableBean.setId(abItem.getId());
        renderableBean.setType(abItem.getType());
        renderableBean.setItem(abItem);
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
    
    private int getOriginalMaskNumber(ABItem domainObj) {
        int rtnVal = -1;
        if (domainObj instanceof ABNeuronFragment) {
            ABNeuronFragment nf = (ABNeuronFragment)domainObj;
            rtnVal = nf.getNumber();
        }
        else if (domainObj instanceof ABCompartment) {
            ABCompartment compartment = (ABCompartment)domainObj;
            rtnVal = compartment.getNumber();
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
                ABCompartment compartment = (ABCompartment) domainHelper.getObjectForItem(item);
                byte[] rgb = new byte[ 4 ];
                if ( RenderUtils.isPassthroughRendering(compartmentSetABItem) ) {
                    setPassthroughRGB( rgb );
                }
                else {
                    Color color = RenderUtils.getColorFromRGBStr(compartment.getDefaultColor());
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
            logger.debug( "Render color is {} for {}.", renderColor, item.getTarget() );
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
