
package org.janelia.it.workstation;


import org.janelia.it.workstation.gui.opengl.TestPolygonalMesh;
import org.janelia.it.workstation.gui.slice_viewer.TestTileFormat;
import org.janelia.it.workstation.shared.util.filecache.CachedFileTest;
import org.janelia.it.workstation.shared.util.filecache.LocalFileCacheTest;
import org.janelia.it.workstation.shared.util.filecache.WebDavClientTest;
import org.janelia.it.workstation.shared.util.filecache.WebDavUploaderTest;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Categories.IncludeCategory(TestCategories.FastTests.class)
@Suite.SuiteClasses({
        CachedFileTest.class,
        LocalFileCacheTest.class,
        TestPolygonalMesh.class,
        TestTileFormat.class,
        WebDavClientTest.class,
        WebDavUploaderTest.class
})
public class ConsoleModuleFastTestsSuite {}
