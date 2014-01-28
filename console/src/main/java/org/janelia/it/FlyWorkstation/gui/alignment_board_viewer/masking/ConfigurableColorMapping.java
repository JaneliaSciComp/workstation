package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.InvertingComparator;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RBComparator;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;
import org.janelia.it.jacs.model.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/5/13
 * Time: 3:04 PM
 *
 * Overridable implementation of a render mapping.
 */
public class ConfigurableColorMapping implements RenderMappingI {

    private static final int RENDER_METHOD_BITS = 3;
    private static final int POSITION_BITS = 3;
    private static final int BYTE_INTENSITY_INTERP = 1 << POSITION_BITS + RENDER_METHOD_BITS;
    private static final int NIBBLE_INTENSITY_INTERP = 2 << POSITION_BITS + RENDER_METHOD_BITS;

    private Map<Long,Integer> guidToRenderMethod;
    private Collection<RenderableBean> renderableBeans;
    private MultiMaskTracker multiMaskTracker;
    private FileStats fileStats;
    private int maxDepthExceededCount = 0;
    private Logger logger = LoggerFactory.getLogger(ConfigurableColorMapping.class);

    public ConfigurableColorMapping() {}
    public ConfigurableColorMapping( MultiMaskTracker multiMaskTracker, FileStats fileStats ) {
        this.multiMaskTracker = multiMaskTracker;
        this.fileStats = fileStats;
    }

    @Override
    public void setRenderables( Collection<RenderableBean> renderables ) {
        this.renderableBeans = renderables;
    }

    /** This is used by the test-loop. It is specifically NOT an override. */
    public void setGuidToRenderMethod( Map<Long,Integer> guidToRenderMethod ) {
        this.guidToRenderMethod = guidToRenderMethod;
    }

    @Override
    public Map<Integer,byte[]> getMapping() {
        Map<Integer, byte[]> masMap = makeMaskMappings();
        if ( maxDepthExceededCount > 0 ) {
            logger.warn(
                    "Exceeded max depth for multimask rendering {} times.  Max depth is {}.",
                    maxDepthExceededCount,
                    MultiMaskTracker.MAX_MASK_DEPTH
            );

            // Give the user some kind of warning of what just happened.
            fileStats.setMaxDepthExceededCount( maxDepthExceededCount );
        }
        maxDepthExceededCount = 0; // Clear for re-use.
        return masMap;
    }

    /**
     * Buildup a map of masks (or intercompatible renderable ids), to their rendering info.
     */
    private Map<Integer,byte[]> makeMaskMappings() {
        Map<Integer,byte[]> maskMappings = new HashMap<Integer,byte[]>();

        mapSingleMasks(maskMappings);
        mapMultiMasks(maskMappings);

        return maskMappings;

    }

    /**
     * Multi-masks: these are backed by lists of alternate masks, two or more each.  They will be
     * mapped to the rendering technique (and color) of the highest-priority, visible alternate mask
     * on their list.  Also, position and position-interpretation will be added in some bits of the
     * render method byte.
     *
     * @param maskMappings add mappings to this.
     */
    private void mapMultiMasks(Map<Integer, byte[]> maskMappings) {
        if ( multiMaskTracker != null ) {
            List<Integer> orderedMasks = prioritizeMasks();
            Map<Integer,MultiMaskTracker.MultiMaskBean> multiMaskMap = multiMaskTracker.getMultiMaskBeans();
            for ( Integer multiMask: multiMaskMap.keySet() ) {
                MultiMaskTracker.MultiMaskBean bean = multiMaskMap.get( multiMask );
                int leastPos = Integer.MAX_VALUE;
                Integer chosenAltMask = null;
                for ( int nextAltMask: bean.getAltMasks() ) {
                    byte[] rgb = maskMappings.get( nextAltMask );
                    if ( rgb != null  &&  rgb[ 3 ] != RenderMappingI.NON_RENDERING ) {
                        int pos = orderedMasks.indexOf( nextAltMask );
                        if ( pos < leastPos ) {
                            chosenAltMask = nextAltMask;
                            leastPos = pos;
                        }
                    }
                }

                // If any visible one found above, map the multimask to that value.
                if ( chosenAltMask != null ) {
                    // Get the raw render info from the chosen alternative mask.
                    byte[] rgb = new byte[ 4 ];
                    System.arraycopy( maskMappings.get( chosenAltMask ), 0, rgb, 0, 4 );

                    // Add info to the "render method" byte.
                    // Using the offset of the chosen mask, within the whole alternates list.
                    int maskOffset = bean.getMaskOffset(chosenAltMask);
                    int intensityOffset = maskOffset << RENDER_METHOD_BITS;
                    if ( maskOffset < 0 ) {
                        logger.warn(
                                "Invalid negative alternative mask offset {}, for mask {}.",
                                intensityOffset, chosenAltMask
                        );
                        rgb[ 3 ] = RenderMappingI.NON_RENDERING;
                    }
                    else if ( maskOffset <= MultiMaskTracker.MAX_MASK_DEPTH ) {
                        int intensityOffsetInterp;

                        // Using the number of alternates to signal to shader how to treat the mask offset number.
                        if ( bean.getAltMaskCount() <= 4 ) {
                            intensityOffsetInterp = BYTE_INTENSITY_INTERP;
                        }
                        else {
                            intensityOffsetInterp = NIBBLE_INTENSITY_INTERP;
                        }
                        rgb[ 3 ] |= intensityOffset | intensityOffsetInterp;
                    }
                    else {
                        rgb[ 3 ] = RenderMappingI.NON_RENDERING;
                        maxDepthExceededCount ++;

                        // Here, nothing to add to the mapping.  Max depth exceeded.
                    }
                    maskMappings.put( multiMask, rgb );

                }
            }

        }
    }

    /**
     * 'Single mask' masks. They do not expand into anything.
     *
     * @param maskMappings the output mapping between the mask and its render method/color.
     */
    private void mapSingleMasks( Map<Integer, byte[]> maskMappings ) {
        byte[][] colorWheel = {
                { (byte)0x00, (byte)0xff, (byte)0x00, (byte)0xff },          //G
                { (byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff },          //B
                { (byte)0xff, (byte)0x00, (byte)0x00, (byte)0xff },          //R
                { (byte)0x00, (byte)0xff, (byte)0xff, (byte)0xff },          //G+B
                { (byte)0xff, (byte)0x00, (byte)0xff, (byte)0xff },          //R+B
                { (byte)0xff, (byte)0xff, (byte)0x00, (byte)0xff },          //R+G
                { (byte)0x8f, (byte)0x00, (byte)0x00, (byte)0xff },          //Dk R
                { (byte)0x00, (byte)0x8f, (byte)0x00, (byte)0xff },          //Dk G
        };

        // Each renderable bean is represented by a single mask and color.
        for ( RenderableBean renderableBean : renderableBeans ) {
            // Make the "back map" to the original fragment number.
            int translatedNum = renderableBean.getTranslatedNum();
            byte[] rgb = renderableBean.getRgb();

            if ( rgb == null ) {
                // May be able to use averages collected during read.
                rgb = setRgbFromAverageColor( renderableBean );
            }

            boolean rgbValsZero = true;
            if ( rgb != null ) {
                for ( int i = 0; i < 3; i++ ) {
                    if ( rgb[ i ] != 0 )
                        rgbValsZero = false;
                }
                if ( rgb[ 3 ] == RenderMappingI.NON_RENDERING ) {
                    rgbValsZero = false;
                }
            }

            if ( rgb == null  ||  rgbValsZero ) {
                rgb = colorWheel[ translatedNum % colorWheel.length ];
                Entity entity = renderableBean.getRenderableEntity();
                if ( entity != null ) {
                    rgb[ 3 ] = RenderMappingI.FRAGMENT_RENDERING;
                }
                else {
                    rgb[ 3 ] = RenderMappingI.PASS_THROUGH_RENDERING;
                }
            }
            else {
                // No-op if non-shader rendering.  Do not add this to the mapping at all.
                if ( rgb[ 3 ] == RenderMappingI.NO_SHADER_USE ) {
                    continue;
                }
            }

            // Placing this here, to benefit from null-catch of RGB array above.
            if ( renderableBean.getRenderableEntity() != null && guidToRenderMethod != null ) {
                Long entityId = renderableBean.getRenderableEntity().getId();
                Integer renderMethodNum = guidToRenderMethod.get( entityId );
                if ( renderMethodNum != null ) {
                    rgb[ 3 ] = renderMethodNum.byteValue();
                }
            }
            maskMappings.put( translatedNum, rgb );
        }
    }

    private List<Integer> prioritizeMasks() {
        // Priority rules:
        //  First Neurons, then reference channels, and finally compartments.
        //  Within each type, rank by decreasing voxel count.
        List<RenderableBean> sortedBeans = new ArrayList<RenderableBean>();
        sortedBeans.addAll( renderableBeans );
        Collections.sort( sortedBeans, new InvertingComparator( new RBComparator() ) );
        /*
            Debug
        System.out.println("Check Sort Order from mapping class.");
        for ( RenderableBean bean: sortedBeans ) {
            System.out.println("SORTED:: " + bean.getRenderableEntity().getName() + "  " + bean.getType() + "  " + bean.getVoxelCount() );
        }
            */

        // Remarshall into a list of ids.  They will now be in priority order.
        List<Integer> prioritizedMasks = new ArrayList<Integer>();
        for ( RenderableBean bean: sortedBeans ) {
            prioritizedMasks.add( bean.getTranslatedNum() );
        }
        return prioritizedMasks;
    }

    /**
     * Add defaulted (non-user-chosen) values to color rendering, iff user has not picked any.
     *
     * @param bean set on here.
     */
    private byte[] setRgbFromAverageColor(RenderableBean bean) {
        byte[] rtnVal = null;
        // Taking average voxels into account.
        if ( bean.getRenderableEntity() != null  &&  fileStats != null ) {
            double[] colorAverages = fileStats.getChannelAverages( bean.getRenderableEntity().getId() );
            if ( colorAverages != null ) {
                rtnVal = new byte[ 4 ];
                for ( int i = 0; i < colorAverages.length; i++ ) {
                    rtnVal[ i ] = (byte)(256.0 * colorAverages[ i ]);
                }
                rtnVal[ 3 ] = RenderMappingI.FRAGMENT_RENDERING;
            }
        }
        return rtnVal;
    }

}
