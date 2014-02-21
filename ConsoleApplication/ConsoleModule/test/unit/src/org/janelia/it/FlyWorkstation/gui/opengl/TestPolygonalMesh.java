package org.janelia.it.FlyWorkstation.gui.opengl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class TestPolygonalMesh {

    @Test
    public void testLoadGourdObjFile() {
        String objName = "gourd.obj";
        InputStream gourdStream = this.getClass().getResourceAsStream(objName);
        if (gourdStream == null)
            fail("Failed to open OBJ file resource "+objName);
        PolygonalMesh testMesh = new PolygonalMesh();
        assertEquals(testMesh.getVertexes().size(), 0);        
        assertEquals(testMesh.getFaces().size(), 0);        
        try {
            testMesh.loadFromObjFile(gourdStream);
        } catch (IOException e) {
            fail("Error reading obj file "+e.getStackTrace());
        }
        // Total number of vertices
        assertEquals(testMesh.getVertexes().size(), 326);
        // Total number of faces
        assertEquals(testMesh.getFaces().size(), 648);
        // Three vertices in first face
        assertEquals(testMesh.getFaces().get(0).vertexIndexes.size(), 3);
    }

}
