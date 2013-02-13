package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.RenderableBean;

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
public interface ColorMappingI {
    Map<Integer,byte[]> getMapping( Collection<RenderableBean> fragments );
}
