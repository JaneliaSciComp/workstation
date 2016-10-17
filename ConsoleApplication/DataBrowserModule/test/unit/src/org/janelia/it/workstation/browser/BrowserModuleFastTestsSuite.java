
package org.janelia.it.workstation.browser;


import org.janelia.it.workstation.browser.filecache.CachedFileTest;
import org.janelia.it.workstation.browser.filecache.LocalFileCacheTest;
import org.janelia.it.workstation.browser.filecache.WebDavClientTest;
import org.janelia.it.workstation.browser.filecache.WebDavUploaderTest;
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
public class BrowserModuleFastTestsSuite {}
