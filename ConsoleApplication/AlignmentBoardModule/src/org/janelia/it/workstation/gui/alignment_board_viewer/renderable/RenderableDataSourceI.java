package org.janelia.it.workstation.gui.alignment_board_viewer.renderable;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/29/13
 * Time: 2:30 PM
 *
 * Implement this to provide Mask/Chan renderable datas.
 */
public interface RenderableDataSourceI {
    String getName();
    Collection<MaskChanRenderableData> getRenderableDatas();
}
