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
    public FileStats() {
        channelAverageMap = new HashMap<Long,double[]>();
    }

    /** Record an average-channel value array by id.  */
    public void recordChannelAverages( Long id, double[] channelAverages ) {
        channelAverageMap.put( id, channelAverages );
    }

    /** Return the averages for all channels associated with this identifier. */
    public double[] getChannelAverages( Long id ) {
        return channelAverageMap.get( id );
    }

    public void clear() {
        channelAverageMap.clear();
    }
}
