package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 8/28/13
 * Time: 4:56 PM
 *
 * This class will keep track of "multi-masks", or mask identifiers that apply to multiple (listed, in order)
 * renderables.
 */
public class MultiMaskTracker {

    public static final String MASK_HEX_FORMAT = "%04x";
    private Map<Integer,MultiMaskBean> maskIdToBean;
    private Map<String,MultiMaskBean> altMasksToBean;

    private int nextMaskNum;

    public MultiMaskTracker() {
        maskIdToBean = new HashMap<Integer, MultiMaskBean>();
        altMasksToBean = new HashMap<String, MultiMaskBean>();
    }

    public void setFirstMaskNum( int firstMaskNum ) {
        this.nextMaskNum = firstMaskNum;
    }

    /**
     * Throw away all accumulated information based on earlier calls to
     * @see #getMask()
     */
    public void clear() {
        maskIdToBean.clear();
        altMasksToBean.clear();
    }

    /**
     * Looks up or creates a replacement mask for the one 'discovered' in the growing volume.
     *
     * @param discoveredMask given as a renderable mask to be set in the target cell.
     * @param oldVolumeMask found to have been set earlier, at the target cell of the volume.
     * @return existing old volume mask, if the discovered mask is already one of its 'constituents'. Otherwise,
     *        some newly-created volume mask, which inverits the old mask's constituents and adds the discovered one.
     */
    public Integer getMask(int discoveredMask, int oldVolumeMask) {
        Integer rtnVal = oldVolumeMask;

        // Check if this exists in the current bean list.
        MultiMaskBean oldBean = maskIdToBean.get( oldVolumeMask );
        String fullInvertedKey;
        List<Integer> altMasks = null;
        if ( oldBean != null ) {
            // Decrement number of voxels referencing the old mask, because now we reference a new one.
            oldBean.decrementVoxelCount();
            altMasks = oldBean.getAltMasks();
            // Generate a key for finding any existing combo of old + new.
            fullInvertedKey = oldBean.getExtendedInvertedKey( discoveredMask );
        }
        else {
            // Whatever mask had been set in the volume was NOT a multi-mask. But a multi-mask convering the
            // combo of new+old may exist. Key will find that.
            if ( discoveredMask < oldVolumeMask ) {
                fullInvertedKey = String.format( MASK_HEX_FORMAT, discoveredMask ) + String.format( MASK_HEX_FORMAT, oldVolumeMask );
            }
            else {
                fullInvertedKey = String.format( MASK_HEX_FORMAT, oldVolumeMask ) + String.format( MASK_HEX_FORMAT, discoveredMask );
            }
        }

        // Need to see if there is a bean covering old alt masks plus this new one.
        MultiMaskBean extendedMultiMaskBean = altMasksToBean.get( fullInvertedKey );
        if ( extendedMultiMaskBean != null ) {
            // This combination already exists.  Use this bean's mask.
            rtnVal = extendedMultiMaskBean.getMultiMaskNum();
            extendedMultiMaskBean.incrementVoxelCount();
        }
        else {
            // Need a new one.  C'tor sets count to 1.
            MultiMaskBean newBean = new MultiMaskBean();
            newBean.setMultiMaskNum( nextMaskNum );
            // Any mask in the list referenced by the old mask, should be referenced by the newly-created mask.
            //  In addition, the newly-created mask's list must also contain the new mask that the calling
            //  process was _going_ to put in this slot.
            if ( altMasks != null ) {
                newBean.addAll( altMasks );
            }
            else {
                newBean.addAltMask( oldVolumeMask );
            }
            newBean.addAltMask( discoveredMask );

            maskIdToBean.put( nextMaskNum, newBean );
            altMasksToBean.put( newBean.getInvertedKey(), newBean );
            nextMaskNum ++;
        }
//        else {
//            MultiMaskBean newBean = new MultiMaskBean();
//            newBean.setMultiMaskNum( nextMaskNum );
//            newBean.addAltMask( oldVolumeMask );
//            newBean.addAltMask( discoveredMask );
//            maskIdToBean.put( nextMaskNum, newBean );
//            altMasksToBean.put( newBean.getInvertedKey(), newBean );
//            nextMaskNum ++;
//        }
        return rtnVal;
    }

    /** Expose the collection created here, for actual use. */
    public Map<Integer,MultiMaskBean> getMultiMaskBeans() {
        return this.maskIdToBean;
    }

    /** All info needed around a multi-mask. */
    public static class MultiMaskBean {
        // This mask ID will be pushed to the GPU.
        private Integer multiMaskNum;
        private List<Integer> altMasks = new ArrayList<Integer>();
        private int voxelCount = 1; // On construction, this instance variable will indicate that 1 voxel is masked.

        public String getInvertedKey() {
            Collections.sort( altMasks );
            StringBuilder rtnVal = new StringBuilder();
            for ( Integer altMask: altMasks ) {
                String hexKey = String.format(MASK_HEX_FORMAT, altMask );
                rtnVal.append( hexKey );
            }
            return rtnVal.toString();
        }

        public String getExtendedInvertedKey( Integer newAltMask ) {
            Collections.sort( altMasks );
            StringBuilder rtnVal = new StringBuilder();
            String newAltHex = String.format( MASK_HEX_FORMAT, newAltMask );
            for ( Integer altMask: altMasks ) {
                String hexKey = String.format( MASK_HEX_FORMAT, altMask );
                if ( altMask > newAltMask ) {
                    rtnVal.append( newAltHex );
                    newAltHex = null;
                }
                rtnVal.append( hexKey );
            }
            if ( newAltHex != null ) {
                rtnVal.append( newAltHex );
            }

            return rtnVal.toString();
        }

        public Integer getMultiMaskNum() {
            return multiMaskNum;
        }

        public void setMultiMaskNum(int multiMaskNum) {
            this.multiMaskNum = multiMaskNum;
        }

        public List<Integer> getAltMasks() {
            return altMasks;
        }

        public void addAltMask( Integer altMask ) {
            altMasks.add( altMask );
        }

        public void addAll( List<Integer> altMasks ) {
            this.altMasks.addAll( altMasks );
        }

        public int getVoxelCount() {
            return voxelCount;
        }

        public void incrementVoxelCount() {
            voxelCount ++;
        }

        public void decrementVoxelCount() {
            voxelCount --;
        }

    }
}
