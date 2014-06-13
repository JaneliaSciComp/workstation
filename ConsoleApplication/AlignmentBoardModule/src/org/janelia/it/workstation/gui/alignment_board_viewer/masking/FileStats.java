package org.janelia.it.workstation.gui.alignment_board_viewer.masking;

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

    @SuppressWarnings("unused")
    public void dumpChannelAverages() {
        double[] maxima = new double[3];
        for (Long key: channelAverageMap.keySet()) {
            double[] avg = channelAverageMap.get(key);
            if ( avg.length > 2 ) {
                System.out.println("Average for ID " + key + ": ["+avg[0]+","+avg[1]+","+avg[2]+"]");
                accumulateMaxima(avg, maxima, 0);
                accumulateMaxima(avg, maxima, 1);
                accumulateMaxima(avg,maxima,2);
            }
            else if (avg.length > 1) {
                System.out.println("Average for ID " + key + ": ["+avg[0]+","+avg[1]+"]");
                accumulateMaxima(avg, maxima, 0);
                accumulateMaxima(avg, maxima, 1);
            }
            else {
                System.out.println("Average for ID " + key + ": ["+avg[0]+"]");
                accumulateMaxima(avg, maxima, 0);
            }
        }
        System.out.println("Max avg red = " + maxima[0] + ", max avg green = " + maxima[1] + ", max avg blue = " + maxima[2]);
    }
    
    private void accumulateMaxima( double[] avg, double[] maxima, int inx ) {
        if ( avg[inx] > maxima[inx] ) {
            maxima[inx] = avg[inx];
        }
    }
}
