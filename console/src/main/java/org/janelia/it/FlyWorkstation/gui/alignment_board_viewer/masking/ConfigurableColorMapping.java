package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

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

    private Map<Long,Integer> guidToRenderMethod;
    private Collection<RenderableBean> renderableBeans;
    private MultiMaskTracker multiMaskTracker;

    public ConfigurableColorMapping() {}
    public ConfigurableColorMapping( MultiMaskTracker multiMaskTracker ) {
        this.multiMaskTracker = multiMaskTracker;
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
        return makeMaskMappings( renderableBeans );
    }

    private Map<Integer,byte[]> makeMaskMappings( Collection<RenderableBean> renderableBeans ) {
        Map<Integer,byte[]> maskMappings = new HashMap<Integer,byte[]>();
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

        // 'Single mask' masks. They do not expand into anything.
        for ( RenderableBean renderableBean : renderableBeans ) {
            // Make the "back map" to the original fragment number.
            int translatedNum = renderableBean.getTranslatedNum();
            byte[] rgb = renderableBean.getRgb();

            if ( rgb == null ) {
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

        // Multi-masks: these are backed by lists of alternate masks, two or more each.  They will be
        // mapped to the rendering technique (and color) of the highest-priority, visible alternate mask
        // on their list.
        if ( multiMaskTracker != null ) {
            List<Integer> orderedMasks = prioritizeMasks();
            Map<Integer,MultiMaskTracker.MultiMaskBean> multiMaskMap = multiMaskTracker.getMultiMaskBeans();
            for ( Integer multiMask: multiMaskMap.keySet() ) {
                MultiMaskTracker.MultiMaskBean bean = multiMaskMap.get( multiMask );
                int leastPos = Integer.MAX_VALUE;
                Integer chosenAltMask = null;
                for ( Integer nextAltMask: bean.getAltMasks() ) {
                    byte[] rgb = maskMappings.get( nextAltMask );
                    if ( rgb != null  &&  rgb[ 3 ] != RenderMappingI.NON_RENDERING ) {
                        int pos = orderedMasks.indexOf( nextAltMask );
                        if ( pos < leastPos ) {
                            chosenAltMask = nextAltMask;
                        }
                    }
                }
                // If any visible one found above, map the multimask to that value.
                if ( chosenAltMask != null ) {
                    maskMappings.put( multiMask, maskMappings.get( chosenAltMask ) );
                }
            }
        }

        return maskMappings;

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

    private static class RBComparator implements Comparator<RenderableBean> {

        private Map<String,Integer> rankMapping;
        {
            rankMapping = new HashMap<String,Integer>();
            rankMapping.put( EntityConstants.TYPE_NEURON_FRAGMENT, 1 );
            rankMapping.put( EntityConstants.TYPE_COMPARTMENT, 3 );
            rankMapping.put( EntityConstants.TYPE_SAMPLE, 9 );
            rankMapping.put( EntityConstants.TYPE_COMPARTMENT_SET, 10 );
        }

        /**
         * Comparator for sorting renderable beans.  Should return descending order.
         *
         * @param second one handed in as 1st param.
         * @param first one handed in as 2nd param.
         * @return Negative : right < left; positive: right > left; 0: same</>
         */
        @Override
        public int compare(RenderableBean first, RenderableBean second) {
            int rtnVal = 0;
            if ( first == null  &&   second == null ) {
                return 0;
            }
            else if ( first == null ) {
                rtnVal = 1;
            }
            else if ( second == null ) {
                rtnVal = -1;
            }
            else if ( first.getType().equals(second.getType()) ) {
                // Must compare the contents.  Ranks among same-typed renderables are ordered by size.
                rtnVal = (int)(first.getVoxelCount() - second.getVoxelCount());
            }
            else {
                String firstTypeName = first.getType();
                int typeRankFirst = rankMapping.get(firstTypeName);
                String secondTypeName = second.getType();
                int typeRankSecond = rankMapping.get(secondTypeName);

                rtnVal = typeRankSecond - typeRankFirst;
            }
            return rtnVal;
        }

    }

    private static class InvertingComparator implements Comparator<RenderableBean> {
        private Comparator<RenderableBean> wrappedComparator;
        public InvertingComparator( Comparator<RenderableBean> wrappedComparator ) {
            this.wrappedComparator = wrappedComparator;
        }
        public int compare(RenderableBean first, RenderableBean second) {
            return wrappedComparator.compare( first, second ) * -1;
        }
    }
}
