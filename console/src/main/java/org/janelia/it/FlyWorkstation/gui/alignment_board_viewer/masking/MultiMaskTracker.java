package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static final int MAX_MASK_DEPTH = 8;

    private Map<Integer,MultiMaskBean> maskIdToBean;
    private Map<String,MultiMaskBean> altMasksToBean;

    private List<Integer> dumpedList;
    private Set<Integer> retiredMasks;
    private Logger logger;

    private int nextMaskNum;
    private boolean masksExhausted = false;

    public MultiMaskTracker() {
        logger = LoggerFactory.getLogger( MultiMaskTracker.class );
        maskIdToBean = new HashMap<Integer, MultiMaskBean>();
        altMasksToBean = new HashMap<String, MultiMaskBean>();
        retiredMasks = new HashSet<Integer>();
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
        retiredMasks.clear();
        if ( dumpedList != null )
            dumpedList.clear();
        masksExhausted = false;
    }

    /**
     * Looks up or creates a replacement mask for the one 'discovered' in the growing volume.
     *
     * @param discoveredMask given as a renderable mask to be set in the target cell.
     * @param oldVolumeMask found to have been set earlier, at the target cell of the volume.
     * @return existing old volume mask, if the discovered mask is already one of its 'constituents'. Otherwise,
     *        some newly-created volume mask, which inherits the old mask's constituents and adds the discovered one.
     *        CAUTION: may return -1 if too many masks were sought.
     */
    public synchronized Integer getMask(int discoveredMask, int oldVolumeMask) {
        Integer rtnVal;

        // Check if this exists in the current bean list.
        MultiMaskBean oldBean = maskIdToBean.get( oldVolumeMask );
        String fullInvertedKey;
        List<Integer> altMasks = null;
        if ( oldBean != null ) {
            altMasks = oldBean.getAltMasks();
            if ( altMasks.contains( discoveredMask ) ) {
                fullInvertedKey = oldBean.getInvertedKey();
                System.out.println("Unlikely scenario: found multimask " + oldVolumeMask + ", which also contains newly-adding submask " + discoveredMask);
            }
            else {
                // Generate a key for finding any existing combo of old + new.
                fullInvertedKey = oldBean.getExtendedInvertedKey( discoveredMask );
                // Decrement number of voxels referencing the old mask, because now we reference a new one.
                oldBean.decrementVoxelCount();
                if ( oldBean.getVoxelCount() == 0 ) {
                    retiredMasks.add( oldBean.getMultiMaskNum() );
                    maskIdToBean.remove( oldBean.getMultiMaskNum() );
                }
            }
        }
        else {
            // Whatever mask had been set in the volume was NOT a multi-mask. But a multi-mask convering the
            // combo of new+old may exist. Key will find that.
            if ( discoveredMask < oldVolumeMask ) {
                fullInvertedKey = new StringBuilder()
                        .append( discoveredMask )
                        .append(' ')
                        .append( oldVolumeMask )
                        .append( ' ' )
                        .toString();
                        //String.format( DUAL_MASK_HEX_FORMAT, discoveredMask, oldVolumeMask );
            }
            else {
                fullInvertedKey = new StringBuilder()
                        .append( oldVolumeMask )
                        .append(' ')
                        .append(discoveredMask)
                        .append( ' ' )
                        .toString();
                        //String.format( DUAL_MASK_HEX_FORMAT, oldVolumeMask, discoveredMask );
            }
        }

        // Need to see if there is a bean covering old alt masks plus this new one.
        rtnVal = getExtendedMultiMask(discoveredMask, oldVolumeMask, fullInvertedKey, altMasks);
        return rtnVal;
    }

    /** Expose the collection created here, for actual use. */
    public Map<Integer,MultiMaskBean> getMultiMaskBeans() {
        return this.maskIdToBean;
    }

    /** convenience helper: simpler interface for caller. */
    public MultiMaskBean getMultiMaskBean( Integer multiMaskId ) {
        return getMultiMaskBeans().get(multiMaskId);
    }

    /** Assumed that if the mask id is not in the mapping, must be a single-mask.  Otherwise rtn #-of-sub-masks. */
    public int getMaskExpansionCount( Integer maskId ) {
        MultiMaskBean maskBean = maskIdToBean.get( maskId );
        return maskBean == null ? 1 : maskBean.getAltMasks().size();
    }

    /** This is the "panic button" to press when things are going wrong, to help debug the problem. */
    public void dumpMaskContents( Integer originalMask ) {
        if ( dumpedList == null ) {
            dumpedList = new ArrayList<Integer>();
        }
        if ( ! dumpedList.contains( originalMask ) ) {
            dumpedList.add(originalMask);
        }

    }

    /** Call this at the end, to drop resources, etc. */
    public void writeOutstandingDump() {
        if ( dumpedList == null ) {
            return;
        }
        StringBuilder totalDump = new StringBuilder("Dumping Mask Contents\n");
        totalDump.append("Mask List: ");
        for ( Integer maskId: dumpedList ) {
            totalDump.append( maskId ).append(",");
        }
        totalDump.setLength( totalDump.length() - 1 );    // Trim trailing comma
        totalDump.append("\n");
        for ( Integer maskId: maskIdToBean.keySet() ) {
            StringBuilder maskContents = new StringBuilder();

            // Dump all multimasks containing any targeted submask.
            List<Integer> altMasks = maskIdToBean.get(maskId).getAltMasks();
            boolean toDump = false;
            for ( Integer member: dumpedList ) {
                if ( altMasks.contains( member ) ) {
                    toDump = true;
                    break;
                }
            }
            if ( toDump ) {
                for ( Integer subMask: altMasks) {
                    maskContents.append( subMask ).append( ' ' );
                }
                String maskContentStr = maskContents.toString().trim();
                totalDump.append( "Mask " ).append( maskId ).append(" contains these submasks [" ).append( maskContentStr ).append( "]").append("\n");
            }
        }

        logger.info( totalDump.toString() );
        totalDump.setLength( 0 );
        totalDump.append( "Dumping Inverted Mask Contents\n" );
        Set<String> altMasks = this.altMasksToBean.keySet();
        for ( String invertedKey: altMasks ) {
            boolean toDump = false;
            String[] altMaskArr = invertedKey.split(" ");
            List<String> altMaskList = Arrays.asList( altMaskArr );
            for ( Integer member: dumpedList ) {
                if ( altMaskList.contains( member.toString() ) ) {
                    toDump = true;
                    break;
                }
            }
            if ( toDump ) {
                totalDump.append( "Alt-Mask-Set ").append( invertedKey ).append( " refers to multimask " ).append( altMasksToBean.get( invertedKey ).getMultiMaskNum() ).append( "\n" );
            }
        }
        logger.info( totalDump.toString() );
        dumpedList.clear();

    }

    /**
     * Given a correct key for finding a multi-mask that covers all of the (one or more) sub-masks from the old volume
     * mask, plus the newly discovered mask that also occupies that voxel, see if a bean/mask already exists that
     * covers this (N+1) case.  If not, create it.
     *
     * @param discoveredMask supplied by a renderable, for a previously-occupied voxel. (+1)
     * @param oldVolumeMask was occupying the voxel before this renderable encountered there.
     * @param fullInvertedKey a search key covering all sub-masks involved.
     * @param altMasks list of submasks against the oldVolumeMask. (N)
     * @return a new multi-mask covering N+1.
     */
    private synchronized Integer getExtendedMultiMask(
            int discoveredMask, int oldVolumeMask, String fullInvertedKey, List<Integer> altMasks
    ) {
        Integer rtnVal;
        MultiMaskBean extendedMultiMaskBean = altMasksToBean.get( fullInvertedKey );
        if ( extendedMultiMaskBean != null ) {
            // This combination already exists.  Use this bean's mask.
            rtnVal = extendedMultiMaskBean.getMultiMaskNum();
            extendedMultiMaskBean.incrementVoxelCount();
        }
        else {
            rtnVal = createIncrementedMultimask(discoveredMask, oldVolumeMask, altMasks);

        }
        return rtnVal;
    }

    /**
     * Helper to make a new multimask whose alternate (or sub) masks include all those in the cell's previously
     * occupying multimask, plus the newly-discovered single mask now also shown to occupy that cell.  The
     * 'old volume mask' may have been a singly-assigned mask or a multimask.
     *
     * @param discoveredMask identifier for some new renderable that claims occupancy of current cell.
     * @param oldVolumeMask identifier for old mask.  Does not include the discovered mask in its (possible) alts.
     * @param altMasks base list of masks covered by the multimask created here.
     * @return mask number of new multi-mask, or -1, to indicate no more are available.
     */
    private Integer createIncrementedMultimask(int discoveredMask, int oldVolumeMask, List<Integer> altMasks) {
        Integer rtnVal;// Need a new one.  C'tor sets voxel count to 1.
        MultiMaskBean newBean = new MultiMaskBean();
        if ( nextMaskNum >= 65536 ) {
            Iterator<Integer> retiredMaskIterator = retiredMasks.iterator();
            if ( retiredMaskIterator.hasNext() ) {
                Integer reusedMask = retiredMasks.iterator().next();
                logger.debug("Re-using mask {}.", reusedMask);
                newBean.setMultiMaskNum(reusedMask);
                retiredMasks.remove(newBean.getMultiMaskNum()); // Back in circulation.
            }
            else {
                if ( ! masksExhausted ) {
                    // OUT of masks.
                    masksExhausted = true;
                    logger.warn("Completely out of masks.  Even with re-use, have exhausted all 2-byte values.");
                }
                return -1;
            }
        }
        else {
            newBean.setMultiMaskNum( nextMaskNum );
        }

        // Any mask in the list referenced by the old mask, should be referenced by the newly-created mask.
        //  In addition, the newly-created mask's list must also contain the new mask that the calling
        //  process claims to belong in this slot.
        if ( altMasks != null ) {
            // Old vol mask was a multi mask, and all its sub's are added to the new bean's subs list.
            newBean.addAll( altMasks );
        }
        else {
            // Old vol mask was a single mask, and is added directly to the subs list.
            newBean.addAltMask( oldVolumeMask );
        }

        // The new bean's alternates must include this new single mask.
        newBean.addAltMask( discoveredMask );

        maskIdToBean.put( nextMaskNum, newBean );
        altMasksToBean.put( newBean.getInvertedKey(), newBean );
        if ( nextMaskNum < 65536 ) {
            nextMaskNum ++;
        }

        rtnVal = newBean.getMultiMaskNum();
        return rtnVal;
    }

    /** All info needed around a multi-mask. */
    public static class MultiMaskBean {
        // This mask ID will be pushed to the GPU.
        private Integer multiMaskNum;
        private List<Integer> altMasks = new ArrayList<Integer>();
        private int voxelCount = 1; // On construction, this instance variable will indicate that 1 voxel is masked.

        public String getInvertedKey() {
            List<Integer> sortedAltMasks = sortAltMasks();
            StringBuilder rtnVal = new StringBuilder();
            for ( Integer altMask: sortedAltMasks ) {
                // String hexKey = String.format(MASK_HEX_FORMAT, altMask );
                rtnVal.append( altMask ).append( ' ' );
            }
            return rtnVal.toString();
        }

        public String getExtendedInvertedKey( Integer newAltMask ) {
            List<Integer> sortedAltMasks = new ArrayList<Integer>( altMasks );
            sortedAltMasks.add( newAltMask );
            Collections.sort( sortedAltMasks );

            StringBuilder rtnVal = new StringBuilder();
            for ( Integer altMask: sortedAltMasks ) {
                //String nextKey = String.format(MASK_HEX_FORMAT, altMask );
                rtnVal.append( altMask ).append( ' ' );
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
            if ( ! altMasks.contains( altMask ) )
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

        /** Returns which priority among all sub-masks, this one is. */
        public int getMaskOffset( Integer maskNum ) {
            return this.getAltMasks().indexOf( maskNum );
        }

        private List<Integer> sortAltMasks() {
            List<Integer> sortedAltMasks = new ArrayList<Integer>();
            sortedAltMasks.addAll( altMasks );
            Collections.sort(sortedAltMasks);
            return sortedAltMasks;
        }

    }
}
