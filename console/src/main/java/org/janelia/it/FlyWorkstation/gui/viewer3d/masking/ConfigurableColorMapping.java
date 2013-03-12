package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.renderable.RenderableBean;
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
    private Collection<RenderableBean> renderableBeans;

    @Override
    public void setRenderables( Collection<RenderableBean> renderables ) {
        this.renderableBeans = renderables;
    }

    /** This is used by the test-loop. It is specifically NOT an override. */
    public void setGuidToRenderMethod( Map<Long,Integer> guidToRenderMethod ) {
        this.guidToRenderMethod = guidToRenderMethod;
    }

    @Override
    public Map<Integer,byte[]> getMapping() {
        return makeMaskMappings( renderableBeans );
    }

    private Map<Integer,byte[]> makeMaskMappings( Collection<RenderableBean> renderableBeans ) {
        Map<Integer,byte[]> maskMappings = new HashMap<Integer,byte[]>();
        byte[][] colorWheel = {
                { (byte)0x00, (byte)0xff, (byte)0x00, (byte)0xff },          //G
                { (byte)0x00, (byte)0x00, (byte)0xff, (byte)0xff },          //B
                { (byte)0xff, (byte)0x00, (byte)0x00, (byte)0xff },          //R
                { (byte)0x00, (byte)0xff, (byte)0xff, (byte)0xff },          //G+B
                { (byte)0xff, (byte)0x00, (byte)0xff, (byte)0xff },          //R+B
                { (byte)0xff, (byte)0xff, (byte)0x00, (byte)0xff },          //R+G
                { (byte)0x8f, (byte)0x00, (byte)0x00, (byte)0xff },          //Dk R
                { (byte)0x00, (byte)0x8f, (byte)0x00, (byte)0xff },          //Dk G
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
                    rgb[ 3 ] = RenderMappingI.PASS_THROUGH_RENDERING;
                }
            }
            else {
                // No-op if non-shader rendering.  Do not add this to the mapping at all.
                if ( rgb[ 3 ] == RenderMappingI.NO_SHADER_USE ) {
                    continue;
                }
            }

            // Placing this here, to benefit from null-catch of RGB array above.
            if ( renderableBean.getRenderableEntity() != null && guidToRenderMethod != null ) {
                Long entityId = renderableBean.getRenderableEntity().getId();
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
