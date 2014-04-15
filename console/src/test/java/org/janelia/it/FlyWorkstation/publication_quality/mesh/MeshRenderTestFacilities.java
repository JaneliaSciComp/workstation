package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableBean;

import java.util.ArrayList;
import java.util.List;

import static org.janelia.it.FlyWorkstation.gui.TestingConstants.LOCAL_CHAN_FILE_PATH;
import static org.janelia.it.FlyWorkstation.gui.TestingConstants.LOCAL_MASK_FILE_PATH;

/**
 * Created by fosterl on 4/14/14.
 */
public class MeshRenderTestFacilities {
    public static final int LABEL_FILE_NUM = 5;
    public static final int TRANSLATED_NUM = 50;
    public static final Long RENDERABLE_ID = 5555L;

    /** This is a facility method for general testing. */
    public static List<MaskChanRenderableData> getMaskChanRenderableDatas() {
        List<MaskChanRenderableData> beanList = new ArrayList<MaskChanRenderableData>();
        MaskChanRenderableData renderableData = new MaskChanRenderableData();
        RenderableBean renderableBean = new RenderableBean();
        renderableBean.setLabelFileNum( LABEL_FILE_NUM );
        renderableBean.setTranslatedNum( TRANSLATED_NUM );
        renderableBean.setAlignedItemId( RENDERABLE_ID );
        renderableData.setBean( renderableBean );

        renderableData.setMaskPath( LOCAL_MASK_FILE_PATH );
        renderableData.setChannelPath( LOCAL_CHAN_FILE_PATH );
        beanList.add( renderableData );
        return beanList;
    }

}
