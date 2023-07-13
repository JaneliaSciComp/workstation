package org.janelia.horta.actors;

import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector4;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.gltools.texture.Texture3d;
import org.janelia.gltools.MeshActor;
import org.janelia.horta.blocks.BlockTileResolution;
import org.janelia.horta.blocks.OmeZarrBlockTileKey;
import org.janelia.horta.blocks.OmeZarrBlockTileSource;
import org.janelia.horta.volume.VolumeMipMaterial;
import org.janelia.workstation.controller.model.color.ImageColorModel;

import javax.media.opengl.GL3;
import java.io.IOException;

public class OmeZarrVolumeMeshActor extends MeshActor implements SortableBlockActor, DepthSlabClipper {
    private final OmeZarrVolumeMeshActor.MeshMaterial meshMaterial;

    private final Vector4 homogeneousCentroid;

    private BlockTileResolution resolution;

    public OmeZarrVolumeMeshActor(OmeZarrBlockTileSource source, OmeZarrBlockTileKey tile, VolumeMipMaterial.VolumeState volumeState, int colorChannel) throws IOException {
        super(new OmeZarrMesh(tile), new OmeZarrVolumeMeshActor.MeshMaterial(source, tile, source.getColorModel(), volumeState, colorChannel), null);

        this.meshMaterial = (OmeZarrVolumeMeshActor.MeshMaterial)getMaterial();

        ConstVector3 centroid = tile.getCentroid();

        this.homogeneousCentroid= new Vector4(centroid.getX(), centroid.getY(), centroid.getZ(), 1.0f);
    }

    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix)
    {
        super.display(gl, camera, parentModelViewMatrix); // display child objects
    }

    public void setOpaqueDepthTexture(Texture2d depthTexture) {
        meshMaterial.setOpaqueDepthTexture(depthTexture);
    }

    public void setRelativeSlabThickness(float zNear, float zFar) {
        meshMaterial.setRelativeSlabThickness(zNear, zFar);
    }

    @Override
    public Vector4 getHomogeneousCentroid() {
        return homogeneousCentroid;
    }

    @Override
    public BlockTileResolution getResolution() {
        return null;
    }

    private static class MeshMaterial extends VolumeMipMaterial {
        private MeshMaterial(OmeZarrBlockTileSource source, OmeZarrBlockTileKey tile, ImageColorModel imageColorModel, VolumeState volumeState, int colorChannel) throws IOException {
            super(safeLoadData(source, tile, colorChannel), imageColorModel);
            setVolumeState(volumeState);
        }

        private static Texture3d safeLoadData(OmeZarrBlockTileSource source, OmeZarrBlockTileKey tile, int colorChannel) throws IOException {
            Texture3d brick = source.loadBrick(tile, colorChannel);
            if (brick == null) {
                throw new IOException("Load was interrupted");
            }
            return brick;
        }
    }
}
