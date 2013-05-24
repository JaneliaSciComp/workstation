package org.janelia.it.FlyWorkstation.gui.viewer3d.renderable;

import org.janelia.it.FlyWorkstation.gui.viewer3d.texture.AlignmentBoardDataBuilder;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 3/29/13
 * Time: 2:33 PM
 *
 * Uses an alignment board context to provide Mask/Chan renderable datas.
 */
public class ABContextRenderableDataSource implements RenderableDataSourceI {
    private AlignmentBoardContext context;

    public ABContextRenderableDataSource( AlignmentBoardContext context ) {
        this.context = context;
    }

    @Override
    public String getName() {
        return context.getName();
    }

    @Override
    public Collection<MaskChanRenderableData> getRenderableDatas() {
        Collection<MaskChanRenderableData> renderableDatas =
                new AlignmentBoardDataBuilder()
                        .setAlignmentBoardContext( context )
                        .getRenderableDatas();

        return renderableDatas;
    }
}
