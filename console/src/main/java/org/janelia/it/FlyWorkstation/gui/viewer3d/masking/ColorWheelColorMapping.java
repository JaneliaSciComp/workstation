package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/5/13
 * Time: 3:04 PM
 *
 * Simplistic implementation of a render mapping.
 */
public class ColorWheelColorMapping implements RenderMappingI {

    private Collection<RenderableBean> renderableBeans;

    @Override
    public void setRenderables(Collection<RenderableBean> beans) {
        this.renderableBeans = beans;
    }

    public Map<Integer,byte[]> getMapping() {
        return makeMaskMappings( renderableBeans );
    }

    private Map<Integer,byte[]> makeMaskMappings( Collection<RenderableBean> renderableBeans ) {
        Map<Integer,byte[]> maskMappings = new HashMap<Integer,byte[]>();
        byte[][] colorWheel = {
                { (byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff },
                { (byte)0x00, (byte)0xff, (byte)0x00, (byte)0xff },
                { (byte)0xff, (byte)0x00, (byte)0x00, (byte)0xff },
                { (byte)0x00, (byte)0xff, (byte)0xff, (byte)0xff },
                { (byte)0xff, (byte)0x00, (byte)0xff, (byte)0xff },
                { (byte)0xff, (byte)0xff, (byte)0x00, (byte)0xff },
                { (byte)0x8f, (byte)0x00, (byte)0x00, (byte)0xff },
                { (byte)0x00, (byte)0x8f, (byte)0x00, (byte)0xff },
        };

        for ( RenderableBean renderableBean : renderableBeans ) {
            // Make the "back map" to the original fragment number.
            int translatedNum = renderableBean.getTranslatedNum();
            byte[] rgb = renderableBean.getRgb();

            if ( rgb == null ) {
                rgb = colorWheel[ translatedNum % colorWheel.length ];
                Entity entity = renderableBean.getRenderableEntity();
                if ( entity != null ) {
                    String entityTypeName = entity.getEntityType().getName();
                    if ( entityTypeName.equals(EntityConstants.TYPE_NEURON_FRAGMENT ) ) {
                        rgb[ 3 ] = RenderMappingI.FRAGMENT_RENDERING;
                    }
                    else {
                        rgb[ 3 ] = RenderMappingI.NON_RENDERING;
                    }
                }
                else {
                    rgb[ 3 ] = RenderMappingI.FRAGMENT_RENDERING;
                }
            }

            maskMappings.put( translatedNum, rgb );
        }

        return maskMappings;

    }
}
