package org.janelia.it.workstation.gui.opengl;

import org.janelia.it.jacs.model.TestCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

@Category(TestCategories.FastTests.class)
public class TestPolygonalMesh {

    @Test
    public void testLoadGourdObjFile() throws Exception {
        String objName = "/org/janelia/it/workstation/gui/opengl/demo/gourd.obj";
        InputStream gourdStream = this.getClass().getResourceAsStream(objName);
        if (gourdStream == null)
            fail("Failed to open OBJ file resource "+objName);
        org.janelia.it.workstation.gui.opengl.PolygonalMesh testMesh = new org.janelia.it.workstation.gui.opengl.PolygonalMesh();
        assertEquals(testMesh.getVertexes().size(), 0);        
        assertEquals(testMesh.getFaces().size(), 0);        
        try {
            testMesh.loadFromObjFile(gourdStream);
        } catch (IOException e) {
            throw new RuntimeException("failed to read " + objName, e);
        }
        // Total number of vertices
        assertEquals(testMesh.getVertexes().size(), 326);
        // Total number of faces
        assertEquals(testMesh.getFaces().size(), 648);
        // Three vertices in first face
        assertEquals(testMesh.getFaces().get(0).vertexIndexes.size(), 3);
    }

}
