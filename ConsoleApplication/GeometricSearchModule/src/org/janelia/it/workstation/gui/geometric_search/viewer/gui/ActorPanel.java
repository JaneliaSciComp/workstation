package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.*;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by murphys on 8/20/2015.
 */
public class ActorPanel extends JTabbedPane implements VoxelViewerEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ActorPanel.class);

    ScrollableColorRowPanel volumeRowPanel=new VolumeScrollableRowPanel();
    ScrollableColorRowPanel meshRowPanel=new MeshScrollableRowPanel();
    ScrollableColorRowPanel elementRowPanel=new VolumeScrollableRowPanel();

    public ActorPanel() {
        addTab("Element", elementRowPanel);
        addTab("Mesh", meshRowPanel);
        addTab("Volume", volumeRowPanel);
    }

    public ScrollableColorRowPanel getVolumeRowPanel() { return volumeRowPanel; }
    public ScrollableColorRowPanel getMeshRowPanel() { return meshRowPanel; }
    public ScrollableColorRowPanel getElementRowPanel() { return elementRowPanel; }

    @Override
    public void processEvent(VoxelViewerEvent event) {
        logger.info("processEvent() event type="+event.getClass().getName());
        if (event instanceof ActorAddedEvent) {
            ActorAddedEvent actorAddedEvent=(ActorAddedEvent)event;
            final Actor actor=actorAddedEvent.getActor();

            if (actor instanceof DenseVolumeActor) {
                DenseVolumeActor denseVolumeActor=(DenseVolumeActor)actor;
                addVolumeActor(volumeRowPanel, denseVolumeActor);
            } else if (actor instanceof MeshActor) {
                MeshActor meshActor=(MeshActor)actor;
                addMeshActor(meshRowPanel, meshActor);
            } else if (actor instanceof SparseVolumeActor) {
                SparseVolumeActor sparseVolumeActor=(SparseVolumeActor)actor;
                addVolumeActor(elementRowPanel, sparseVolumeActor);
            }

        } else if (event instanceof ActorsClearAllEvent) {
            clear();
        }
    }

    private void addVolumeActor(ScrollableColorRowPanel rowPanel, DenseVolumeActor actor) {
        SyncedCallback colorSelectionCallback=createColorSelectionCallback(rowPanel, actor);
        SyncedCallback brightnessCallback=createBrightnessCallback(actor);
        SyncedCallback transparencyCallback=createTransparencyCallback(actor);

        Map<String, SyncedCallback> callbackMap=new HashMap<>();
        callbackMap.put(ScrollableColorRowPanel.COLOR_CALLBACK, colorSelectionCallback);
        callbackMap.put(VolumeScrollableRowPanel.BRIGHTNESS_CALLBACK, brightnessCallback);
        callbackMap.put(VolumeScrollableRowPanel.TRANSPARENCY_CALLBACK, transparencyCallback);

        try {
            rowPanel.addEntry(actor.getName(), callbackMap);
            Vector4 actorColor = actor.getColor();
            if (actorColor != null) {
                float[] colorData = actorColor.toArray();
                if (colorData != null) {
                    int red = (int) (colorData[0] * 255);
                    int green = (int) (colorData[1] * 255);
                    int blue = (int) (colorData[2] * 255);
                    rowPanel.setEntryStatusColor(actor.getName(), new Color(red, green, blue));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
    }

    private void addMeshActor(ScrollableColorRowPanel rowPanel, MeshActor actor) {
        SyncedCallback colorSelectionCallback=createColorSelectionCallback(rowPanel, actor);
        SyncedCallback edgefalloffCallback=createEdgeFalloffCallback(actor);
        SyncedCallback intensityCallback=createIntensityCallback(actor);
        SyncedCallback ambientCallback=createAmbientCallback(actor);

        Map<String, SyncedCallback> callbackMap=new HashMap<>();
        callbackMap.put(ScrollableColorRowPanel.COLOR_CALLBACK, colorSelectionCallback);
        callbackMap.put(MeshScrollableRowPanel.EDGEFALLOFF_CALLBACK, edgefalloffCallback);
        callbackMap.put(MeshScrollableRowPanel.INTENSITY_CALLBACK, intensityCallback);
        callbackMap.put(MeshScrollableRowPanel.AMBIENT_CALLBACK, ambientCallback);

        try {
            rowPanel.addEntry(actor.getName(), callbackMap);
            Vector4 actorColor = actor.getColor();
            if (actorColor != null) {
                float[] colorData = actorColor.toArray();
                if (colorData != null) {
                    int red = (int) (colorData[0] * 255);
                    int green = (int) (colorData[1] * 255);
                    int blue = (int) (colorData[2] * 255);
                    rowPanel.setEntryStatusColor(actor.getName(), new Color(red, green, blue));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
    }

    private SyncedCallback createColorSelectionCallback(final ScrollableColorRowPanel rowPanel, final Actor actor) {
        return new SyncedCallback() {
            @Override
            public void performAction(Object o) {
                Color selectedColor=(Color)o;
                Vector4 colorVector=new Vector4(0,0,0,1f);
                selectedColor.getRGBColorComponents(colorVector.toArray());
                actor.setColor(colorVector);
                rowPanel.setEntryStatusColor(actor.getName(), selectedColor);
                EventManager.sendEvent(this, new ActorModifiedEvent());
            }
        };
    }

    private SyncedCallback createBrightnessCallback(final DenseVolumeActor actor) {
        return new SyncedCallback() {
            @Override
            public void performAction(Object o) {
                Float brightness=(Float)o;
                actor.setBrightness(brightness);
                EventManager.sendEvent(this, new ActorModifiedEvent());
            }
        };
    }

    private SyncedCallback createTransparencyCallback(final DenseVolumeActor actor) {
        return new SyncedCallback() {
            @Override
            public void performAction(Object o) {
                Float transparency=(Float)o;
                actor.setTransparency(transparency);
                EventManager.sendEvent(this, new ActorModifiedEvent());
            }
        };
    }

    private SyncedCallback createEdgeFalloffCallback(final MeshActor actor) {
        return new SyncedCallback() {
            @Override
            public void performAction(Object o) {
                Float edgeFalloff=(Float)o;
                actor.setEdgeFalloff(edgeFalloff);
                EventManager.sendEvent(this, new ActorModifiedEvent());
            }
        };
    }

    private SyncedCallback createIntensityCallback(final MeshActor actor) {
        return new SyncedCallback() {
            @Override
            public void performAction(Object o) {
                Float intensity=(Float)o;
                actor.setIntensity(intensity);
                EventManager.sendEvent(this, new ActorModifiedEvent());
            }
        };
    }

    private SyncedCallback createAmbientCallback(final MeshActor actor) {
        return new SyncedCallback() {
            @Override
            public void performAction(Object o) {
                Float ambience=(Float)o;
                actor.setAmbience(ambience);
                EventManager.sendEvent(this, new ActorModifiedEvent());
            }
        };
    }


    private void clear() {
        volumeRowPanel.clear();
        meshRowPanel.clear();
        elementRowPanel.clear();
    }

}
