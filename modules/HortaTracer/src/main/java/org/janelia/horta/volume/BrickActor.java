package org.janelia.horta.volume;

import java.io.IOException;

import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.horta.volume.VolumeMipMaterial.VolumeState;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.gltools.texture.Texture3d;
import org.janelia.horta.BrainTileInfo;
import org.janelia.horta.actors.BrainTileMesh;

/**
 *
 * @author Christopher Bruns
 */
public class BrickActor extends MeshActor implements DepthSlabClipper {
    private final BrickInfo brainTile;
    private final BrickMaterial brickMaterial;

    public BrickActor(BrickInfo brainTile,
                      ImageColorModel brightnessModel,
                      VolumeState volumeState,
                      int colorChannel) throws IOException {
        super(new BrainTileMesh(brainTile),
                new BrickMaterial(brainTile, brightnessModel, volumeState, colorChannel),
                null);
        this.brainTile = brainTile;
        this.brickMaterial = (BrickMaterial)getMaterial();
    }

    // Constructor version that uses preloaded Texture3d
    public BrickActor(BrickInfo brainTile,
                      Texture3d texture3d,
                      ImageColorModel brightnessModel,
                      VolumeState volumeState) {
        super(new BrainTileMesh(brainTile),
                new BrickMaterial(texture3d, brightnessModel, volumeState),
                null);
        this.brainTile = brainTile;
        this.brickMaterial = (BrickMaterial)getMaterial();
    }
    
    public void setOpaqueDepthTexture(Texture2d depthTexture) {
        brickMaterial.setOpaqueDepthTexture(depthTexture);
    }

    public BrickInfo getBrainTile() {
        return brainTile;
    }

    public void setRelativeSlabThickness(float zNear, float zFar) {
        brickMaterial.setRelativeSlabThickness(zNear, zFar);
    }

    private static class BrickMaterial extends VolumeMipMaterial {

        private BrickMaterial(BrickInfo brainTile,
                              ImageColorModel imageColorModel,
                              VolumeState volumeState,
                              int colorChannel) throws IOException {
            super(safeLoadBrick(brainTile, colorChannel), imageColorModel);
            setVolumeState(volumeState);
        }

        private static Texture3d safeLoadBrick(BrickInfo brainTile, int colorChannel) throws IOException {
            brainTile.setColorChannelIndex(colorChannel);
            Texture3d brick = brainTile.loadBrick(10, null);
            if (brick == null) {
                throw new IOException("Load was interrupted");
            }
            return brick;
        }

        private BrickMaterial(
                Texture3d texture3d,
                ImageColorModel imageColorModel,
                VolumeState volumeState) {
            super(texture3d, imageColorModel);
            setVolumeState(volumeState);
        }
        
    }
}
