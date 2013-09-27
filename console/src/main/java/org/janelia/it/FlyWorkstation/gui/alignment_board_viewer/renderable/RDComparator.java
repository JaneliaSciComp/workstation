package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/26/13
 * Time: 11:15 PM
 *
 * Use this to compare two renderable datas.  This delegats to the RB comparator, and returns sort order associated
 * with the renderable beans contained in the renderable datas.
 */
public class RDComparator implements Comparator<MaskChanRenderableData> {
    private boolean ascending;

    public RDComparator() {
        ascending = true;
    }

    public RDComparator( boolean ascending ) {
        this.ascending = ascending;
    }

    /**
     * Comparator for sorting things that contain renderable beans.  Should return descending order.
     *
     * @param second one handed in as 1st param.
     * @param first one handed in as 2nd param.
     * @return Negative : right < left; positive: right > left; 0: same</>
     */
    @Override
    public int compare(MaskChanRenderableData first, MaskChanRenderableData second) {
        int rtnVal;
        if ( first == null  &&   second == null ) {
            rtnVal = 0;
        }
        else if ( first == null ) {
            rtnVal = 1;
        }
        else if ( second == null ) {
            rtnVal = -1;
        }
        else {
            RenderableBean firstBean = first.getBean();
            RenderableBean secondBean = second.getBean();

            rtnVal = new RBComparator().compare( firstBean, secondBean );
        }
        if ( ! ascending ) {
            rtnVal = -rtnVal;
        }
        return rtnVal;
    }

}

