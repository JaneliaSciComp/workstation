package org.janelia.horta.actors;

import org.janelia.geometry3d.*;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.horta.blocks.BlockTileKey;
import org.janelia.horta.blocks.OmeZarrBlockTileSource;
import org.janelia.horta.volume.VolumeMipMaterial;
import org.janelia.workstation.controller.model.color.ImageColorModel;

import javax.media.opengl.GL3;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

public class OmeZarrVolumeActor extends BasicGL3Actor implements DepthSlabClipper {
    private static OmeZarrVolumeActor singletonInstance;

    // Singleton access
    static public OmeZarrVolumeActor getInstance() {
        if (singletonInstance == null)
            singletonInstance = new OmeZarrVolumeActor();
        return singletonInstance;
    }

    private Texture2d opaqueDepthTexture = null;
    private float zNearRelative = 0.50f;
    private float zFarRelative = 150.0f; // relative z clip planes
    private ImageColorModel brightnessModel;
    private VolumeMipMaterial.VolumeState volumeState;
    private OmeZarrBlockTileSource source;

    private Observer dynamicTilesObserver;
    private Vantage vantage;
    private boolean autoUpdateCache;

    private final List<OmeZarrVolumeChannelActor> channelActors;

    private OmeZarrVolumeActor() {
        super(null);

        channelActors = new ArrayList<>();
        channelActors.add(new OmeZarrVolumeChannelActor(this, 0));
        channelActors.add(new OmeZarrVolumeChannelActor(this, 1));
        channelActors.add(new OmeZarrVolumeChannelActor(this, 2));
    }

    public void addTransientBlock(BlockTileKey key) {
        for (OmeZarrVolumeChannelActor channelActor : channelActors) {
            channelActor.addTransientBlock(key);
        }
    }

    public void setAutoUpdate(boolean updateCache) {
        this.autoUpdateCache = updateCache;
        for (OmeZarrVolumeChannelActor channelActor : channelActors) {
            channelActor.setAutoUpdate(updateCache);
        }
    }

    public void setVolumeState(VolumeMipMaterial.VolumeState volumeState) {
        this.volumeState = volumeState;
        for (OmeZarrVolumeChannelActor channelActor : channelActors) {
            channelActor.setVolumeState(volumeState);
        }
    }

    public void setHortaVantage(Vantage vantage) {
        this.vantage = vantage;
        for (OmeZarrVolumeChannelActor channelActor : channelActors) {
            channelActor.setHortaVantage(vantage);
        }
    }

    public void addDynamicTileUpdateObservable(Observer observer) {
        this.dynamicTilesObserver = observer;
        for (OmeZarrVolumeChannelActor channelActor : channelActors) {
            channelActor.getDynamicTileUpdateObservable().addObserver(observer);
        }
    }

    public void setBrightnessModel(ImageColorModel brightnessModel) {
        this.brightnessModel = brightnessModel;
        for (OmeZarrVolumeChannelActor channelActor : channelActors) {
            channelActor.setBrightnessModel(brightnessModel);
        }
    }

    public void setOmeZarrTileSource(OmeZarrBlockTileSource source) {
        this.source = source;

        /*
        if (source != null) {
            int numChannels = source.getNumColorChannels();

            if (numChannels > channelActors.size()) {
                for (int idx = channelActors.size(); idx < numChannels; idx++) {
                    OmeZarrVolumeChannelActor actor = new OmeZarrVolumeChannelActor(this, idx);
                    actor.setOmeZarrTileSource(source);
                    actor.setBrightnessModel(brightnessModel);
                    actor.setAutoUpdate(autoUpdateCache);
                    actor.setHortaVantage(vantage);
                    actor.setVolumeState(volumeState);
                    actor.setOpaqueDepthTexture(opaqueDepthTexture);
                    actor.setRelativeSlabThickness(zNearRelative, zFarRelative);
                    actor.getDynamicTileUpdateObservable().addObserver(dynamicTilesObserver);
                    channelActors.add(actor);
                }
            } else if (numChannels < channelActors.size()){
                channelActors.subList(numChannels, channelActors.size()).clear();
            }
        }
        */

        for (OmeZarrVolumeChannelActor channelActor : channelActors) {
            channelActor.setOmeZarrTileSource(source);
        }
    }

    public VolumeMipMaterial.VolumeState getVolumeState() {
        return this.volumeState;
    }

    public void refreshBlocks(ConstVector3 focus) {
        for (OmeZarrVolumeChannelActor channelActor : channelActors) {
            channelActor.refreshBlocks(focus);
        }
    }

    public void clearAllBlocks() {
        for (OmeZarrVolumeChannelActor channelActor : channelActors) {
            channelActor.clearAllBlocks();
        }
    }

    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        for (OmeZarrVolumeChannelActor channelActor : channelActors) {
            channelActor.display(gl, camera, parentModelViewMatrix);
        }
    }

    @Override
    public void setOpaqueDepthTexture(Texture2d opaqueDepthTexture) {
        this.opaqueDepthTexture = opaqueDepthTexture;
        for (OmeZarrVolumeChannelActor channelActor : channelActors) {
            channelActor.setOpaqueDepthTexture(opaqueDepthTexture);
        }
    }

    @Override
    public void setRelativeSlabThickness(float zNear, float zFar) {
        this.zNearRelative = zNear;
        this.zFarRelative = zFar;
        for (OmeZarrVolumeChannelActor channelActor : channelActors) {
            channelActor.setRelativeSlabThickness(zNear, zFar);
        }
    }
}
