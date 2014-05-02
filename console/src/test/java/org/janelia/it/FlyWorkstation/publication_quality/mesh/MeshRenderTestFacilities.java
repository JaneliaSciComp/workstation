package org.janelia.it.FlyWorkstation.publication_quality.mesh;

import org.janelia.it.jacs.compute.access.loader.renderable.MaskChanRenderableData;
import org.janelia.it.jacs.compute.access.loader.renderable.RenderableBean;

import java.util.ArrayList;
import java.util.List;

import static org.janelia.it.FlyWorkstation.gui.TestingConstants.*;

/**
 * Created by fosterl on 4/14/14.
 */
public class MeshRenderTestFacilities {
    public static final int COMPARTMENT_LABEL_FILE_NUM = 5;
    public static final int COMPARTMENT_TRANSLATED_NUM = 50;
    public static final Long COMPARTMENT_RENDERABLE_ID = 5555L;
    public static final int NEURON_LABEL_FILE_NUM = 7;
    public static final int NEURON_TRANSLATED_NUM = 70;
    public static final Long NEURON_RENDERABLE_ID = 7777L;

    /** This is a facility method for general testing. */
    public static List<MaskChanRenderableData> getCompartmentMaskChanRenderableDatas() {
        List<MaskChanRenderableData> beanList = new ArrayList<MaskChanRenderableData>();
        MaskChanRenderableData renderableData = new MaskChanRenderableData();
        RenderableBean renderableBean = new RenderableBean();
        renderableBean.setLabelFileNum(COMPARTMENT_LABEL_FILE_NUM);
        renderableBean.setTranslatedNum(COMPARTMENT_TRANSLATED_NUM);
        renderableBean.setAlignedItemId(COMPARTMENT_RENDERABLE_ID);
        renderableData.setBean( renderableBean );

        renderableData.setMaskPath(LOCAL_COMPARTMENT_MASK_FILE_PATH);
        renderableData.setChannelPath(LOCAL_COMPARTMENT_CHAN_FILE_PATH);
        beanList.add( renderableData );
        return beanList;
    }

    /** This is a facility method for general testing. */
    public static List<MaskChanRenderableData> getNeuronMaskChanRenderableDatas() {
        List<MaskChanRenderableData> beanList = new ArrayList<MaskChanRenderableData>();
        MaskChanRenderableData renderableData = new MaskChanRenderableData();
        RenderableBean renderableBean = new RenderableBean();
        renderableBean.setLabelFileNum(NEURON_LABEL_FILE_NUM);
        renderableBean.setTranslatedNum(NEURON_TRANSLATED_NUM);
        renderableBean.setAlignedItemId(NEURON_RENDERABLE_ID);
        renderableData.setBean( renderableBean );

        renderableData.setMaskPath(LOCAL_NEURON_MASK_FILE_PATH);
        renderableData.setChannelPath(LOCAL_NEURON_CHAN_FILE_PATH);
        beanList.add( renderableData );
        return beanList;
    }

}
