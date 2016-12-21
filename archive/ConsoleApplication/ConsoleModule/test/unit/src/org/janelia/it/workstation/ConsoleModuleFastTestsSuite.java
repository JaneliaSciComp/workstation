
package org.janelia.it.workstation;

import org.janelia.it.jacs.model.TestCategories;

import org.janelia.it.workstation.gui.opengl.TestPolygonalMesh;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Categories.class)
@Categories.IncludeCategory(TestCategories.FastTests.class)
@Suite.SuiteClasses({
        TestPolygonalMesh.class,
})
public class ConsoleModuleFastTestsSuite {}
