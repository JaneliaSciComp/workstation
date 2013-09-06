package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable;

import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/5/13
 * Time: 4:46 PM
 *
 * Use this to compare two renderable beans.
 */
public class RBComparator implements Comparator<RenderableBean> {

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
        int rtnVal;
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

