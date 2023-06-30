package org.janelia.horta.actors;

import org.janelia.geometry3d.*;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.horta.blocks.*;
import org.janelia.horta.volume.VolumeMipMaterial;
import org.janelia.workstation.controller.model.color.ImageColorModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL3;
import javax.media.opengl.GL4;
import java.util.ArrayList;
import java.util.List;

public class OmeZarrVolumeActor extends BasicGL3Actor implements DepthSlabClipper {
    private static final Logger LOG = LoggerFactory.getLogger(OmeZarrVolumeActor.class);

    private static OmeZarrVolumeActor singletonInstance;

    // Singleton access
    static public OmeZarrVolumeActor getInstance() {
        if (singletonInstance == null)
            singletonInstance = new OmeZarrVolumeActor();
        return singletonInstance;
    }

    private Texture2d opaqueDepthTexture = null;
    private float zNearRelative = 0.10f;
    private float zFarRelative = 100.0f; // relative z clip planes
    private ImageColorModel brightnessModel;

    private VolumeMipMaterial.VolumeState volumeState = new VolumeMipMaterial.VolumeState();

    private OmeZarrBlockTileSource source;
    private final OmeZarrTileCache dynamicTiles = new OmeZarrTileCache(null);
    private BlockChooser<OmeZarrBlockTileKey, OmeZarrBlockTileSource> chooser;
    private BlockDisplayUpdater<OmeZarrBlockTileKey, OmeZarrBlockTileSource> blockDisplayUpdater;

    private OmeZarrVolumeActor() {
        super(null);
        chooser = new OmeZarrBlockChooser();
        blockDisplayUpdater = new BlockDisplayUpdater<>(chooser);
        initBlockStrategy(chooser);
        blockDisplayUpdater.getDisplayChangeObservable().addObserver((o, arg) -> dynamicTiles.updateDesiredTiles(blockDisplayUpdater.getDesiredBlocks()));
        dynamicTiles.getDisplayChangeObservable().addObserver(((o, arg) -> {
            List<Object3d> list = new ArrayList<>();
            getChildren().forEach(c -> {
                if (!dynamicTiles.getDisplayedActors().contains(c)) {
                    list.add(c);
                }
            });
            getChildren().removeAll(list);
            dynamicTiles.getDisplayedActors().forEach(a -> {
                if (!getChildren().contains(a)) {
                    addPersistentBlock(a);
                }
            });
        }));
    }

    private void initBlockStrategy(BlockChooser<OmeZarrBlockTileKey, OmeZarrBlockTileSource> chooser) {
        dynamicTiles.setBlockStrategy(chooser);
        blockDisplayUpdater.setBlockChooser(chooser);
    }

    public void setAutoUpdate(boolean updateCache) {
        blockDisplayUpdater.setAutoUpdate(updateCache);
    }

    public VolumeMipMaterial.VolumeState getVolumeState() {
        return volumeState;
    }

    public Object3d addPersistentBlock(Object3d child) {
        return addChild(child);
    }

    @Override
    public Object3d addChild(Object3d child) {
        if (child instanceof DepthSlabClipper) {
            ((DepthSlabClipper) child).setOpaqueDepthTexture(opaqueDepthTexture);
            ((DepthSlabClipper) child).setRelativeSlabThickness(zNearRelative, zFarRelative);
        }
        return super.addChild(child);
    }

    public void addTransientBlock(BlockTileKey key) {
        dynamicTiles.addDesiredTile((OmeZarrBlockTileKey) key);
    }

    public void setVolumeState(VolumeMipMaterial.VolumeState volumeState) {
        this.volumeState = volumeState;
    }

    public void setBrightnessModel(ImageColorModel brightnessModel) {
        this.brightnessModel = brightnessModel;
    }

    public void clearAllBlocks() {
        dynamicTiles.clearAllTiles();
        getChildren().clear();
    }

    public void setHortaVantage(Vantage vantage) {
        blockDisplayUpdater.setVantage(vantage);
    }

    public void setOmeZarrTileSource(OmeZarrBlockTileSource source) {
        dynamicTiles.setSource(source);
        blockDisplayUpdater.setBlockTileSource(source);
        this.source = source;
    }

    public ObservableInterface getDynamicTileUpdateObservable() {
        return dynamicTiles.getDisplayChangeObservable();
    }

    @Override
    public void setOpaqueDepthTexture(Texture2d opaqueDepthTexture) {
        this.opaqueDepthTexture = opaqueDepthTexture;

        getChildren().forEach(c -> {
            if (c instanceof DepthSlabClipper) {
                ((DepthSlabClipper) c).setOpaqueDepthTexture(opaqueDepthTexture);
            }
        });
    }

    @Override
    public void setRelativeSlabThickness(float zNear, float zFar) {
        zNearRelative = zNear;
        zFarRelative = zFar;

        getChildren().forEach(c -> {
            if (c instanceof DepthSlabClipper) {
                ((DepthSlabClipper) c).setRelativeSlabThickness(zNear, zFar);
            }
        });
    }

    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        if (!isVisible())
            return;
        if (!isInitialized) init(gl);
        if (getChildren() == null)
            return;

        Object3d[] children = getChildren().toArray(new Object3d[0]);
        if (children.length < 1)
            return;

        gl.glEnable(GL3.GL_BLEND);
        // Max intensity
        ((GL4) gl).glBlendEquationi(0, GL4.GL_MAX); // RGBA color target
        // Always use Maximum for blending secondary render target
        ((GL4) gl).glBlendEquationi(1, GL4.GL_MAX); // core intensity/depth target

        Matrix4 modelViewMatrix = parentModelViewMatrix;
        if (modelViewMatrix == null)
            modelViewMatrix = camera.getViewMatrix();
        Matrix4 localMatrix = getTransformInParent();
        if (localMatrix != null)
            modelViewMatrix = new Matrix4(modelViewMatrix).multiply(localMatrix);
        for (Object3d child : children) {
            if (child instanceof GL3Actor) {
                ((GL3Actor) child).display(gl, camera, modelViewMatrix);
            }
        }
    }
}