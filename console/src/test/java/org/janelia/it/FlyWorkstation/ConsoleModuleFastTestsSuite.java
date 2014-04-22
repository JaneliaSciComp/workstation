
package org.janelia.it.FlyWorkstation;

import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.ChannelReadTest;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.MaskReadTest;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.channel_split.NBitChannelSplitStrategyTest;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.gui_elements.GpuSamplerTest;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.ConfigurableColorMappingTest;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.masking.MultiMaskTrackerTest;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RBComparatorTest;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable.RDComparatorTest;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder.DownSamplerTest;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder.VeryLargeVolumeTest;
import org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_export.FilteringAcceptorDecoratorTest;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.UserSettingSerializerStringTest;
import org.janelia.it.FlyWorkstation.gui.opengl.TestPolygonalMesh;
import org.janelia.it.FlyWorkstation.gui.slice_viewer.TestTileFormat;
import org.janelia.it.FlyWorkstation.shared.util.filecache.CachedFileTest;
import org.janelia.it.FlyWorkstation.shared.util.filecache.LocalFileCacheTest;
import org.janelia.it.FlyWorkstation.shared.util.filecache.WebDavClientTest;
import org.janelia.it.FlyWorkstation.shared.util.filecache.WebDavUploaderTest;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Categories.IncludeCategory(TestCategories.FastTests.class)
@Suite.SuiteClasses({
        CachedFileTest.class,
        ChannelReadTest.class,
        ConfigurableColorMappingTest.class,
        DownSamplerTest.class,
        FilteringAcceptorDecoratorTest.class,
        GpuSamplerTest.class,
        LocalFileCacheTest.class,
        MaskReadTest.class,
        MultiMaskTrackerTest.class,
        NBitChannelSplitStrategyTest.class,
        RBComparatorTest.class,
        RDComparatorTest.class,
        TestPolygonalMesh.class,
        TestTileFormat.class,
        UserSettingSerializerStringTest.class,
        VeryLargeVolumeTest.class,
        WebDavClientTest.class,
        WebDavUploaderTest.class
})
public class ConsoleModuleFastTestsSuite {}
