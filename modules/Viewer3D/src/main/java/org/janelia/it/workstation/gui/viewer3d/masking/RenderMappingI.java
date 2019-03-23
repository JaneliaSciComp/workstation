package org.janelia.it.workstation.gui.viewer3d.masking;

import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/5/13
 * Time: 3:03 PM
 *
 * Supplies a color mapping, given a list of
 */
public interface RenderMappingI {
    public static final String PASSTHROUGH_RENDER_ATTRIBUTE = "Passthrough";

    public static final byte FRAGMENT_RENDERING = 1;
    public static final byte NON_RENDERING = 0;
    public static final byte COMPARTMENT_RENDERING = 2;
    public static final byte SOLID_COMPARTMENT_RENDERING = 3;
    public static final byte PASS_THROUGH_RENDERING = 4;
    public static final byte NO_SHADER_USE = 5;

    void setRenderables( Collection<RenderableBean> beans );
    Map<Integer,byte[]> getMapping();
}
