package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.texture;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableDataSourceI;
import org.janelia.it.FlyWorkstation.model.domain.*;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
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
    private final AlignmentBoardContext context;

    private AlignedItem currentSample; // NOTE: use of this precludes multi-threaded use of this data source!
    private AlignedItem currentCompartmentSet;

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
        Collection<MaskChanRenderableData> rtnVal = new ArrayList<MaskChanRenderableData>();

        int nextTranslatedNum = 1;

        int liveFileCount = 0;

        for ( AlignedItem alignedItem : context.getAlignedItems() ) {

            EntityWrapper itemEntity = alignedItem.getItemWrapper();
            if ( itemEntity instanceof Sample) {
                currentSample = alignedItem;
                currentCompartmentSet = null;
                Sample sample = (Sample)itemEntity;
                Entity internalEntity = sample.getInternalEntity();

                MaskChanRenderableData containerRenderable = getNonRenderingRenderableData(internalEntity);

                rtnVal.add( containerRenderable );

                Collection<AlignedItem> childItems = alignedItem.getAlignedItems();
                if ( childItems != null ) {
                    for ( AlignedItem childItem: childItems ) {
                        if ( childItem.getItemWrapper() instanceof Neuron) {
                            liveFileCount += getRenderableData( rtnVal, nextTranslatedNum++, false, childItem );
                        }
                        else if ( childItem.getItemWrapper() instanceof VolumeImage ) {
                            VolumeImage image = sample.getReference();
                            liveFileCount += getRenderableData( rtnVal, nextTranslatedNum++, image, childItem );
                        }
                    }
                }
            }
            else if ( itemEntity instanceof Neuron ) {
                liveFileCount += getRenderableData(rtnVal, nextTranslatedNum, false, alignedItem);
            }
            else if ( itemEntity instanceof CompartmentSet) {
                currentSample = null;
                currentCompartmentSet = alignedItem;

                CompartmentSet compartmentSet = (CompartmentSet)itemEntity;
                Entity internalEntity = compartmentSet.getInternalEntity();

                MaskChanRenderableData containerRenderable = getNonRenderingRenderableData(internalEntity);

                rtnVal.add( containerRenderable );

                Collection<AlignedItem> childItems = alignedItem.getAlignedItems();
                if ( childItems != null ) {
                    for ( AlignedItem item: childItems ) {
                        if ( item.getItemWrapper() instanceof Compartment ) {
                            liveFileCount += getRenderableData(rtnVal, nextTranslatedNum++, true, item);
                        }
                    }
                }

            }
            else if ( itemEntity instanceof Compartment ) {
                liveFileCount += getRenderableData(rtnVal, nextTranslatedNum, true, alignedItem);
            }
        }

        //  Prevent user from seeing "forever working" indicator when there is nothing to see.
        if ( liveFileCount == 0  &&  context.getAlignedItems().size() > 0 ) {
            String message = "No mask or channel file sets found.  Nothing to display.";
            logger.error( message );
            throw new RuntimeException( message );
        }

        return rtnVal;
    }

    //------------------------------------------------------------HELPERS

    /**
     * Creates a container renderable which will not be shown.  It exists for its children's reference.
     *
     * @param internalEntity entity being wrapped by this data.
     * @return the renderable being created.
     */
    private MaskChanRenderableData getNonRenderingRenderableData(Entity internalEntity) {
        RenderableBean containerDataBean = new RenderableBean();
        containerDataBean.setLabelFileNum(0);
        containerDataBean.setTranslatedNum(0); // Always zero for any sample.
        containerDataBean.setRgb(
                new byte[]{
                        NON_RENDER_INTENSITY, NON_RENDER_INTENSITY, NON_RENDER_INTENSITY, RenderMappingI.NON_RENDERING
                }
        );
        containerDataBean.setRenderableEntity(internalEntity);

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
            AlignedItem item) {
        RenderableBean renderableBean = createRenderableBean( nextTranslatedNum, isCompartment, item );
        MaskChanRenderableData nfRenderable = new MaskChanRenderableData();
        nfRenderable.setBean( renderableBean );
        nfRenderable.setCompartment( isCompartment );

        Masked3d masked = (Masked3d)item.getItemWrapper();
        String maskPath = getMaskPath(masked);
        nfRenderable.setMaskPath( maskPath );
        String channelPath = getChannelPath(masked);
        nfRenderable.setChannelPath( channelPath );

        int liveFileCount = getCorrectFilesFoundCount(
                "" + renderableBean.getRenderableEntity(), maskPath, channelPath
        );

        maskChanRenderableDatas.add( nfRenderable );
        return liveFileCount;
    }

    private int getRenderableData(
        Collection<MaskChanRenderableData> maskChanRenderableDatas,
        int nextTranslatedNum,
        VolumeImage volumeImage,
        AlignedItem item
    ) {
        RenderableBean renderableBean = new RenderableBean();
        renderableBean.setInvertedY(false);
        renderableBean.setLabelFileNum(nextTranslatedNum);
        renderableBean.setTranslatedNum(nextTranslatedNum);
        renderableBean.setType("Reference");     //todo move this to EntityConstants
        renderableBean.setRenderableEntity( volumeImage.getInternalEntity() );
        setAppearance( false, item, renderableBean );
        MaskChanRenderableData data = new MaskChanRenderableData();
        data.setBean( renderableBean );
        data.setCompartment( true ); // For render characteristics

        String maskPath = volumeImage.getMask3dImageFilepath();
        String channelPath = volumeImage.getChan3dImageFilepath();
        if ( maskPath == null || channelPath == null ) {
            logger.warn("No mask and/or channel pat for reference {}:{}.", renderableBean.getRenderableEntity().getName(), renderableBean.getRenderableEntity().getId() );
        }
        data.setChannelPath( channelPath );
        data.setMaskPath( maskPath );

        maskChanRenderableDatas.add( data );
        int liveFileCount = getCorrectFilesFoundCount(
                "" + renderableBean.getRenderableEntity(), maskPath, channelPath
        );

        return liveFileCount;
    }

    /**
     * This will implement the fetch of mask file name.
     *
     * @param masked has a mask file associated.
     * @return that mask file.
     */
    private String getMaskPath( Masked3d masked ) {
        return masked.getMask3dImageFilepath();
    }

    /**
     * This will implement the fetch of channel file name.
     *
     * @param masked has a channel file associated.
     * @return that channel file.
     */
    private String getChannelPath( Masked3d masked ) {
        return masked.getChan3dImageFilepath();
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

    private RenderableBean createRenderableBean( int translatedNum, boolean isCompartment, AlignedItem item ) {
        Entity internalEntity = item.getInternalEntity();
        MaskIndexed maskIndexed = (MaskIndexed)item.getItemWrapper();
        int maskIndex = maskIndexed.getMaskIndex();
        logger.debug(
                "Creating Renderable Bean for: " + item.getItemWrapper().getName() + " original index=" + maskIndex +
                        " new index=" + translatedNum
        );

        RenderableBean renderableBean = new RenderableBean();
        renderableBean.setLabelFileNum(maskIndex);
        renderableBean.setTranslatedNum(translatedNum);
        renderableBean.setRenderableEntity(internalEntity);
        if ( isCompartment ) {
            renderableBean.setInvertedY( false );
            renderableBean.setType(EntityConstants.TYPE_COMPARTMENT );
        }
        else {
            renderableBean.setInvertedY( true );
        }

        // If a simplistic value has been set, change it.  Otherwise, would be something more specific.
        if ( renderableBean.getType().equals( EntityConstants.TYPE_ALIGNED_ITEM ) ) {
            renderableBean.setType(EntityConstants.TYPE_NEURON_FRAGMENT);
        }
        setAppearance(isCompartment, item, renderableBean);


        return renderableBean;
    }

    private void setAppearance(boolean isCompartment, AlignedItem item, RenderableBean renderableBean) {
        // See to the appearance.
        Color renderColor = item.getColor();
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
            else if ( item.isPassthroughRendering()  ||  parentIsPassthrough() ) {
                byte[] rgb = new byte[ 4 ];
                setPassthroughRGB(rgb);
                renderableBean.setRgb(rgb);
            }
            else if ( isCompartment ) {
                Compartment compartment = (Compartment)item.getItemWrapper();
                byte[] rgb = new byte[ 4 ];
                if ( currentCompartmentSet.isPassthroughRendering() ) {
                    setPassthroughRGB( rgb );
                }
                else {
                    int[] rawColor = compartment.getColor();
                    for ( int i = 0; i < 3; i++ ) {
                        rgb[ i ] = (byte)rawColor[ i ];
                    }
                    rgb[ 3 ] = RenderMappingI.COMPARTMENT_RENDERING;
                    renderableBean.setRgb( rgb );
                }
            }
        }
        else {
            logger.debug( "Render color is {} for {}.", renderColor, item.getItemWrapper().getName() );
            // A Neuron Color was set, but the neuron could still be "turned off" for render.
            byte[] rgb = new byte[ 4 ];
            setRgbFromColor(renderColor, rgb);
            byte renderMethod = RenderMappingI.FRAGMENT_RENDERING;
            if ( item.isPassthroughRendering() ) {
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
            renderColor = getParentColor( currentSample );
        }
        else if ( currentCompartmentSet != null ) {
            renderColor = getParentColor( currentCompartmentSet );
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

    private Color getParentColor( AlignedItem parent ) {
        Color renderColor = null;
        if ( parent != null  &&  parent.getColor() != null ) {
            renderColor = parent.getColor();
        }
        return renderColor;
    }

    private boolean parentIsPassthrough() {
        return (currentCompartmentSet != null  &&   currentCompartmentSet.isPassthroughRendering())  ||
               (currentSample != null  &&  currentSample.isPassthroughRendering());
    }
}
