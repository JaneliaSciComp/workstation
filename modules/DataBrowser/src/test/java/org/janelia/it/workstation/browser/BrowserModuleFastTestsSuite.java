
package org.janelia.it.workstation.browser;


import org.janelia.it.workstation.browser.filecache.AgentStorageClientTest;
import org.janelia.it.workstation.browser.filecache.LocalFileCacheTest;
import org.janelia.it.workstation.browser.filecache.MasterStorageClientTest;
import org.janelia.it.workstation.browser.filecache.RemoteFileCacheLoaderTest;
import org.janelia.it.workstation.browser.filecache.WebDavUploaderTest;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Categories.IncludeCategory(TestCategories.FastTests.class)
@Suite.SuiteClasses({
        RemoteFileCacheLoaderTest.class,
        LocalFileCacheTest.class,
        AgentStorageClientTest.class,
        MasterStorageClientTest.class,
        WebDavUploaderTest.class
})
public class BrowserModuleFastTestsSuite {}
