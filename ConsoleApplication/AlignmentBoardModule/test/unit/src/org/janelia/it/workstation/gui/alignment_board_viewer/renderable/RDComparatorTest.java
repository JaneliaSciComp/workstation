package org.janelia.it.workstation.gui.alignment_board_viewer.renderable;

import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.junit.Test;
import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Check that the sort follows its required collating sequence.
 * Created by fosterl on 1/29/14.
 */
@Category(TestCategories.FastTests.class)
public class RDComparatorTest {
    private static final String CHANNEL_PATH = "/MaskResources/Compartment/maskChannelFormatWithTemplate/compartment_57.chan";
    private static final String MASK_PATH = "/MaskResources/Compartment/maskChannelFormatWithTemplate/compartment_57.mask";

    @Test
    public void testCompare() throws Exception {
        // Need to make the list.
        List<RenderableBean> enclosedBeanList = new RenderableBeanCollection().createCollection();

        List<MaskChanRenderableData> dataList = new ArrayList<MaskChanRenderableData>();
        for ( RenderableBean bean: enclosedBeanList ) {
            MaskChanRenderableData data = new MaskChanRenderableData();
            data.setBean(bean);
            data.setChannelPath(CHANNEL_PATH);
            data.setMaskPath(MASK_PATH);
            data.setCompartment( bean.getType().equals(EntityConstants.TYPE_COMPARTMENT ) );

            dataList.add( data );
        }

        Collections.sort( dataList, new RDComparator( false ) );

        // The tough part: see if it is sorted.  We preserve the order found, but make a new "un-enclosed" collection.
        // The sort order is meant to follow that of the enclosed value.
        List<RenderableBean> unwrappedBeanList = new ArrayList<RenderableBean>();
        for ( MaskChanRenderableData data: dataList ) {
            unwrappedBeanList.add( data.getBean() );
        }

        RBComparatorTest wrappedBeanTest = new RBComparatorTest();
        wrappedBeanTest.checkCollationConformation( unwrappedBeanList );
    }
}
