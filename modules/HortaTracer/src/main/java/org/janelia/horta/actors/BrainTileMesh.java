package org.janelia.horta.actors;

import Jama.Matrix;
import org.janelia.horta.volume.BrickInfo;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vertex;
import org.janelia.geometry3d.VolumeTextureMesh;

/**
 * Specialized Mesh for mouse brain tile rendering
 * @author Christopher Bruns
 */
public class BrainTileMesh extends MeshGeometry
implements VolumeTextureMesh
{
    private final BrickInfo brickInfo;
    private Matrix4 transformWorldToTexCoord;
    
    public BrainTileMesh(BrickInfo brickInfo) {
        this.brickInfo = brickInfo;
        // Enumerate 8 corners of tile
        // NOTE: These are actual corners, not axis-aligned bounding box
        int cornerCount = 0;
        for ( ConstVector3 corner : brickInfo.getCornerLocations() ) {
            // spatial coordinates
            Vertex v = addVertex(corner);
            
            // texture coordinates
            // X texture coordinates alternate
            float tx = (float)cornerCount % 2;
            float ty = (float)(cornerCount/2) % 2;
            float tz = (float)(cornerCount/4) % 2;
            v.setAttribute("texCoord", new Vector3(tx, ty, tz));
            
            cornerCount += 1;
        }

        addFace(new int[] {0, 2, 3, 1}); // rear
        addFace(new int[] {4, 5, 7, 6}); // front
        addFace(new int[] {1, 3, 7, 5}); // right
        addFace(new int[] {0, 4, 6, 2}); // left
        addFace(new int[] {2, 6, 7, 3}); // top
        addFace(new int[] {0, 1, 5, 4}); // bottom
        notifyObservers();
    }
    
    @Override
    public Matrix4 getTransformWorldToTexCoord() {
        if (transformWorldToTexCoord == null) {
            Matrix m = brickInfo.getTexCoord_X_stageUm();
            transformWorldToTexCoord = new Matrix4(m);
            // System.out.println("transformWorldToTexCoord = "+transformWorldToTexCoord);
        }
        return transformWorldToTexCoord;
    }

    @Override
    public float getMinResolution() {
        // convert nanometers to micrometers
        return (float)brickInfo.getResolutionMicrometers();
    }
    
}
