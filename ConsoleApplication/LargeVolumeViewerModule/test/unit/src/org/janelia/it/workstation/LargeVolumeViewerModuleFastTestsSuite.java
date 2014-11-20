
package org.janelia.it.workstation;


import org.janelia.it.workstation.gui.large_volume_viewer.TestTileFormat;
import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.workstation.gui.passive_3d.filter.MatrixFilter3DTest;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Categories.IncludeCategory(TestCategories.FastTests.class)
@Suite.SuiteClasses({
        TestTileFormat.class,
        MatrixFilter3DTest.class
})
public class LargeVolumeViewerModuleFastTestsSuite {}
