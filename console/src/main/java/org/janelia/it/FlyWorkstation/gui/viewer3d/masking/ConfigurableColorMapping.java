package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.RenderableBean;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/5/13
 * Time: 3:04 PM
 *
 * Overridable implementation of a render mapping.
 */
public class ConfigurableColorMapping implements RenderMappingI {

    private Map<Long,Integer> guidToRenderMethod;

    public void setGuidToRenderMethod( Map<Long,Integer> guidToRenderMethod ) {
        this.guidToRenderMethod = guidToRenderMethod;
    }

    public Map<Integer,byte[]> getMapping( Collection<RenderableBean> renderableBeans ) {
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
                Entity entity = renderableBean.getEntity();
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
                    rgb[ 3 ] = RenderMappingI.PASS_THROUGH_RENDERING;
                }
            }

            // Placing this here, to benefit from null-catch of RGB array above.
            if ( renderableBean.getEntity() != null && guidToRenderMethod != null ) {
                Long entityId = renderableBean.getEntity().getId();
                Integer renderMethodNum = guidToRenderMethod.get( entityId );
                if ( renderMethodNum != null ) {
                    rgb[ 3 ] = renderMethodNum.byteValue();
                }
            }

            maskMappings.put( translatedNum, rgb );
        }

        return maskMappings;

    }
}
