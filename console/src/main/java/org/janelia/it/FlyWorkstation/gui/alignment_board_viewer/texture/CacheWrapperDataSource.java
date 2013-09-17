package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.texture;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.MaskChanRenderableData;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RenderableDataSourceI;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 8/26/13
 * Time: 11:30 AM
 *
 * This is a means of creating simplistic entities/wrappers for presentation.  Test data only.
 */
public class CacheWrapperDataSource implements RenderableDataSourceI {

    private RenderableDataSourceI wrappedDataSource;
    private Collection<MaskChanRenderableData> cachedContents;

    /** Use this as decorator around wrapped source. */
    public CacheWrapperDataSource( RenderableDataSourceI wrappedDataSource ) {
        this.wrappedDataSource = wrappedDataSource;
    }

    //--------------------------------------------IMPLEMENTS RenderableDataSourceI
    @Override
    public String getName() {
        return wrappedDataSource.getName();
    }

    @Override
    public Collection<MaskChanRenderableData> getRenderableDatas() {
        this.cachedContents = wrappedDataSource.getRenderableDatas();
        return cachedContents;
    }
    //--------------------------------------------END IMPL RenderableDataSourceI

    public RenderableDataSourceI getSimpleDataSource() {
        return new RenderableDataSourceI() {
            @Override
            public String getName() {
                return "Cached data";
            }

            @Override
            public Collection<MaskChanRenderableData> getRenderableDatas() {
                return cachedContents;
            }
        };
    }
}
