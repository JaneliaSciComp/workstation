
package org.janelia.it.FlyWorkstation;

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
        LocalFileCacheTest.class,
        WebDavClientTest.class,
        WebDavUploaderTest.class
})
public class ConsoleModuleFastTestsSuite {}
