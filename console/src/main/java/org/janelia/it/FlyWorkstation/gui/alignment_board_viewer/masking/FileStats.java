package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/12/13
 * Time: 3:39 PM
 *
 * Populate this with file statistics from beans encountered.
 */
public class FileStats {
    private Map<Long,double[]> channelAverageMap;
    private int maxDepthExceededCount = 0;
    private boolean masksExhausted = false;

    public FileStats() {
        channelAverageMap = new HashMap<Long,double[]>();
    }

    /** Record an average-channel value array by id.  */
    public synchronized void recordChannelAverages( Long id, double[] channelAverages ) {
        double[] oldChannelAverages = channelAverageMap.get(id);
        if ( oldChannelAverages !=  null ) {
            for ( int i = 0; i < channelAverages.length; i++ ) {
                channelAverages[ i ] += oldChannelAverages[ i ];
            }
        }
        channelAverageMap.put( id, channelAverages );
    }

    /** Return the averages for all channels associated with this identifier. */
    public synchronized double[] getChannelAverages( Long id ) {
        double[] averages = channelAverageMap.get( id );
        double[] rtnVal = averages;
        if ( averages != null ) {
            rtnVal = new double[ 3 ];
            for ( int i = 0; i < 3; i++ ) {
                if ( averages.length > i ) {
                    rtnVal[ i ] = averages[ i ];
                }
            }
        }
        return rtnVal;
    }

    public synchronized void clear() {
        channelAverageMap.clear();
    }

    public int getMaxDepthExceededCount() {
        return maxDepthExceededCount;
    }

    public void setMaxDepthExceededCount( int maxDepthExceededCount ) {
        this.maxDepthExceededCount = maxDepthExceededCount;
    }

    public boolean isMasksExhausted() {
        return masksExhausted;
    }

    public void setMasksExhausted(boolean masksExhausted) {
        this.masksExhausted = masksExhausted;
    }

    public int getItemCount() { return channelAverageMap.size(); }
}
