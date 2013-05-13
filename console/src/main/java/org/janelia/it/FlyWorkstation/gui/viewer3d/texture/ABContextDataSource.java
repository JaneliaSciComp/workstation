package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.RenderMappingI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableDataSourceI;
import org.janelia.it.FlyWorkstation.model.domain.*;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;

/** Implements the data source, to avoid having to mock up an entire context just to test. */
public class ABContextDataSource implements RenderableDataSourceI {

    // This data was manually extracted from some Yoshi/standard-space-size data.  It is named as:
    //   prefix_1822248087368761442_9.mask
    // where '9' above is the label number, the usual mask/chan extensions apply, and the number shown
    // should match the location for the previously-known files fetched from the older pipeline.
    private static final String MASK_EXTENSION = ".mask";
    private static final byte COMPARTMENT_INTENSITY = (byte) 0f;
    private static final byte NON_RENDER_INTENSITY = (byte) 0f;
    private AlignmentBoardContext context;
    private String[] filenames;

    private Logger logger = LoggerFactory.getLogger( ABContextDataSource.class );
    public ABContextDataSource(String[] filenames) {
        this.filenames = filenames;
    }

    public ABContextDataSource( AlignmentBoardContext context ) {
        this.context = context;
    }

    @Override
    public String getName() {
        return ABContextDataSource.class.getSimpleName() + " Data Source";
    }

    @Override
    public Collection<MaskChanRenderableData> getRenderableDatas() {
        logger.info( "Getting renderable datas." );
        if ( filenames != null ) {
            return getRenderableDatasForFileList();
        }
        Collection<MaskChanRenderableData> rtnVal = new ArrayList<MaskChanRenderableData>();

        int nextTranslatedNum = 50000; //1;

        int liveFileCount = 0;

        for ( AlignedItem alignedItem : context.getAlignedItems() ) {

            EntityWrapper itemEntity = alignedItem.getItemWrapper();
            if ( itemEntity instanceof Sample) {
                Sample sample = (Sample)itemEntity;
                Entity internalEntity = sample.getInternalEntity();

                MaskChanRenderableData containerRenderable = getNonRenderingRenderableData(internalEntity);

                rtnVal.add( containerRenderable );

                Collection<AlignedItem> childItems = alignedItem.getAlignedItems();
                if ( childItems != null ) {
                    for ( AlignedItem item: childItems ) {
                        if ( item.getItemWrapper() instanceof Neuron) {
                            liveFileCount += getRenderableData(rtnVal, nextTranslatedNum++, false, item);
                        }
                    }
                }
            }
            else if ( itemEntity instanceof Neuron ) {
                liveFileCount += getRenderableData(rtnVal, nextTranslatedNum, false, alignedItem);
            }
            else if ( itemEntity instanceof CompartmentSet) {
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

    /*
    @deprecated
     */
    public Collection<MaskChanRenderableData> getRenderableDatasForFileList() {

        logger.info( "Getting renderable datas." );
        Collection<MaskChanRenderableData> rtnVal = new ArrayList<MaskChanRenderableData>();

        // Establish the renderable for the "signal".
        int nextTranslatedNum = 0;
        RenderableBean signalBean = new RenderableBean();
        signalBean.setLabelFileNum(0);
        signalBean.setTranslatedNum(nextTranslatedNum++);
        signalBean.setRgb(
                new byte[]{
                        NON_RENDER_INTENSITY, NON_RENDER_INTENSITY, NON_RENDER_INTENSITY, RenderMappingI.NON_RENDERING
                }
        );

        MaskChanRenderableData signalData = new MaskChanRenderableData();
        signalData.setBean(signalBean);
        signalData.setCompartment(false);

        // Must divie-up the inputs between compartments and Fragments.

        // Establish the compartment renderables.
        int nextArgument = 0;
        for ( int i = 0; i < filenames.length; i ++ ) {
            if ( ! filenames[ i ].toLowerCase().contains( "/compartment_" ) ) {
                break;
            }
            String fn = filenames[ i ];
            if ( fn.endsWith( MASK_EXTENSION ) ) {
                RenderableBean renderableBean = new RenderableBean();

                // File name should tell its own index number.
                int underPos = fn.lastIndexOf( '_' );
                String beanNumStr = fn.substring( underPos + 1, fn.indexOf( MASK_EXTENSION ) );
                int beanNum = Integer.parseInt( beanNumStr );
                renderableBean.setLabelFileNum(beanNum);
                renderableBean.setTranslatedNum(nextTranslatedNum);
                renderableBean.setRgb(
                        new byte[]{0, 0, 0, RenderMappingI.COMPARTMENT_RENDERING}
                );

                MaskChanRenderableData data = new MaskChanRenderableData();
                data.setBean( renderableBean );
                data.setCompartment( true );
                data.setMaskPath( fn );
                rtnVal.add(data);

                nextTranslatedNum ++;
            }
            nextArgument ++;
        }

        // Establish the fragment renderables.
        //  TEMP: just using the aligned items for coloring, not data.
        Collection<AlignedItem> neuronFragments = new ArrayList<AlignedItem>();
        for ( AlignedItem item: context.getAlignedItems() ) {
            for ( AlignedItem childItem: item.getAlignedItems() ) {
                if ( childItem.isVisible() )
                    neuronFragments.add( childItem );
            }
        }

        Iterator<AlignedItem> alignedItemIterator = neuronFragments.iterator();

        for ( int i = nextArgument; i < filenames.length; i += 2 ) {
            RenderableBean renderableBean = new RenderableBean();

            String fn = filenames[ i ];
            if ( fn.endsWith( MASK_EXTENSION ) ) {
                // File name should tell its own index number.
                int underPos = fn.lastIndexOf( '_' );
                String beanNumStr = fn.substring( underPos + 1, fn.indexOf( MASK_EXTENSION ) );
                int beanNum = Integer.parseInt( beanNumStr );

                renderableBean.setTranslatedNum( nextTranslatedNum );
                renderableBean.setLabelFileNum( beanNum );
                byte[] renderMethod = {
                        (byte) 255, (byte) 255, (byte) 255,
                        RenderMappingI.FRAGMENT_RENDERING
                };
                if ( alignedItemIterator.hasNext()) {
                    Color renderColor = null;
                    while ( renderColor == null  &&  alignedItemIterator.hasNext() ) {
                        AlignedItem item = alignedItemIterator.next();
                        renderColor = item.getColor();
                    }

                    if ( renderColor != null ) {
                        renderMethod[ 0 ] = (byte)renderColor.getRed();
                        renderMethod[ 1 ] = (byte)renderColor.getGreen();
                        renderMethod[ 2 ] = (byte)renderColor.getBlue();

                    }
                }

                renderableBean.setRgb(
                        renderMethod
                );

                MaskChanRenderableData data = new MaskChanRenderableData();
                data.setBean( renderableBean );
                data.setCompartment( false );
                data.setMaskPath( filenames[ i ] );
                data.setChannelPath( filenames[ i + 1 ] );
                rtnVal.add(data);

                nextTranslatedNum ++;
            }
            else {
                throw new IllegalArgumentException(
                        "Wrong file order.  Expecting .mask at position " + i + " in command-line arguments."
                );
            }
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
     * @return
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
        renderableBean.setInvertedY( ! isCompartment );

        // See to the appearance.
        Color renderColor = item.getColor();
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
            else if ( isCompartment ) {
                Compartment compartment = (Compartment)item.getItemWrapper();
                byte[] rgb = new byte[ 4 ];
                int[] rawColor = compartment.getColor();
                for ( int i = 0; i < 3; i++ ) {
                    rgb[ i ] = (byte)rawColor[ i ];
                }
                rgb[ 3 ] = RenderMappingI.COMPARTMENT_RENDERING;
                renderableBean.setRgb( rgb );
            }
        }
        else {
            logger.debug( "Render color is {} for {}.", renderColor, item.getItemWrapper().getName() );
            // A Neuron Color was set, but the neuron could still be "turned off" for render.
            byte[] rgb = new byte[ 4 ];
            rgb[ 0 ] = (byte)renderColor.getRed();
            rgb[ 1 ] = (byte)renderColor.getGreen();
            rgb[ 2 ] = (byte)renderColor.getBlue();
            byte renderMethod = RenderMappingI.FRAGMENT_RENDERING;
            if ( ! item.isVisible() ) {
                renderMethod = RenderMappingI.NON_RENDERING;
            }
            else if ( isCompartment ) {
                renderMethod = RenderMappingI.COMPARTMENT_RENDERING;
            }
            rgb[ 3 ] = renderMethod;
            renderableBean.setRgb(rgb);
        }

        return renderableBean;
    }


}
