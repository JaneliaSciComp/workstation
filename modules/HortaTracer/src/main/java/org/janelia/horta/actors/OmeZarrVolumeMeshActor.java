package org.janelia.horta.actors;

import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vector4;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.gltools.texture.Texture3d;
import org.janelia.gltools.MeshActor;
import org.janelia.horta.blocks.BlockTileResolution;
import org.janelia.horta.blocks.OmeZarrBlockResolution;
import org.janelia.horta.blocks.OmeZarrBlockTileKey;
import org.janelia.horta.blocks.OmeZarrBlockTileSource;
import org.janelia.horta.volume.VolumeMipMaterial;
import org.janelia.workstation.controller.model.color.ImageColorModel;

import javax.media.opengl.GL3;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

public class OmeZarrVolumeMeshActor extends MeshActor implements SortableBlockActor, SortableBlockActorSource, DepthSlabClipper {
    private final OmeZarrVolumeMeshActor.MeshMaterial meshMaterial;

    private final Vector4 homogeneousCentroid;

    private BlockTileResolution resolution;
    
    private final List<SortableBlockActor> listOfThis;
    
    private final Vector3 bbox_min;
    private final Vector3 bbox_max;
    
    private double distance;

    public OmeZarrVolumeMeshActor(OmeZarrBlockTileSource source, OmeZarrBlockTileKey tile, VolumeMipMaterial.VolumeState volumeState, int colorChannel) throws IOException {
        super(new OmeZarrMesh(tile), new OmeZarrVolumeMeshActor.MeshMaterial(source, tile, source.getColorModel(), volumeState, colorChannel), null);

        this.meshMaterial = (OmeZarrVolumeMeshActor.MeshMaterial)getMaterial();

        ConstVector3 centroid = tile.getCentroid();

        this.homogeneousCentroid= new Vector4(centroid.getX(), centroid.getY(), centroid.getZ(), 1.0f);
        
        resolution = new OmeZarrBlockResolution(tile.getKeyDepth(), tile.getShape(), tile.getResolutionMicrometers(), 0);
        
        bbox_min = tile.getOrigin();
        Vector3 ext = tile.getExtents();
        bbox_max = new Vector3(bbox_min.get(0) + ext.get(0), bbox_min.get(1) + ext.get(1), bbox_min.get(2) + ext.get(2));
        
        listOfThis = new ArrayList<>();
        listOfThis.add(this);
    }
    
    public void setDistance(double d) {
    	distance = d;
    }
    public double getDistance() {
    	return distance;
    }
    public Vector3 getBBoxMin() {
    	return bbox_min;
    }
    public Vector3 getBBoxMax() {
    	return bbox_max;
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
    public Collection<SortableBlockActor> getSortableBlockActors() {
        return listOfThis;
    }

    @Override
    public BlockTileResolution getResolution() {
        return resolution;
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
