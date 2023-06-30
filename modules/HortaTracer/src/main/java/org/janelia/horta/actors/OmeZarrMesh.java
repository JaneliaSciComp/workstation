package org.janelia.horta.actors;

import Jama.Matrix;
import org.janelia.geometry3d.*;
import org.janelia.horta.blocks.OmeZarrBlockTileKey;

public class OmeZarrMesh extends MeshGeometry implements VolumeTextureMesh {
    private final OmeZarrBlockTileKey data;
    private Matrix4 transformWorldToTexCoord;

    public OmeZarrMesh(OmeZarrBlockTileKey data) {
        this.data = data;
        // Enumerate 8 corners of tile
        // NOTE: These are actual corners, not axis-aligned bounding box
        int cornerCount = 0;
        for (ConstVector3 corner : data.getCornerLocations()) {
            // spatial coordinates
            Vertex v = addVertex(corner);

            // texture coordinates
            // X texture coordinates alternate
            float tx = (float) cornerCount % 2;
            float ty = (float) (cornerCount / 2) % 2;
            float tz = (float) (cornerCount / 4) % 2;
            v.setAttribute("texCoord", new Vector3(tx, ty, tz));

            cornerCount += 1;
        }

        addFace(new int[]{0, 2, 3, 1}); // rear
        addFace(new int[]{4, 5, 7, 6}); // front
        addFace(new int[]{1, 3, 7, 5}); // right
        addFace(new int[]{0, 4, 6, 2}); // left
        addFace(new int[]{2, 6, 7, 3}); // top
        addFace(new int[]{0, 1, 5, 4}); // bottom

        notifyObservers();
    }

    @Override
    public Matrix4 getTransformWorldToTexCoord() {
        if (transformWorldToTexCoord == null) {
            Matrix m = data.getStageCoordToTexCoord();
            transformWorldToTexCoord = new Matrix4(m);
        }

        return transformWorldToTexCoord;
    }

    @Override
    public float getMinResolution() {
        return (float) data.getResolutionMicrometers();
    }
}
