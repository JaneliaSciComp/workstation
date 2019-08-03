
package org.janelia.workstation.core;


import org.janelia.workstation.core.filecache.AgentStorageClientTest;
import org.janelia.workstation.core.filecache.MasterStorageClientTest;
import org.janelia.workstation.core.filecache.WebDavUploaderTest;
import org.janelia.it.jacs.model.TestCategories;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Categories.IncludeCategory(TestCategories.FastTests.class)
@Suite.SuiteClasses({
        AgentStorageClientTest.class,
        MasterStorageClientTest.class,
        WebDavUploaderTest.class
})
public class BrowserModuleFastTestsSuite {}
