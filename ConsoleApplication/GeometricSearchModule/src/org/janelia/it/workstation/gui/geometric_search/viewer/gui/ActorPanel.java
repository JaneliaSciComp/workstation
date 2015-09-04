package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.*;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Created by murphys on 8/20/2015.
 */
public class ActorPanel extends JTabbedPane implements VoxelViewerEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ActorPanel.class);

    ScrollableColorRowPanel volumeRowPanel=new ScrollableColorRowPanel();
    ScrollableColorRowPanel meshRowPanel=new ScrollableColorRowPanel();
    ScrollableColorRowPanel elementRowPanel=new ScrollableColorRowPanel();

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
                addActor(volumeRowPanel, actor);
            } else if (actor instanceof MeshActor) {
                addActor(meshRowPanel, actor);
            } else if (actor instanceof SparseVolumeActor) {
                addActor(elementRowPanel, actor);
            }

        } else if (event instanceof ActorsClearAllEvent) {
            clear();
        }
    }

    private void addActor(ScrollableColorRowPanel rowPanel, Actor actor) {
        SyncedCallback colorSelectionCallback=createColorSelectionCallback(rowPanel, actor);
        SyncedCallback brightnessCallback=createBrightnessCallback(actor);
        SyncedCallback transparencyCallback=createTransparencyCallback(actor);

        rowPanel.addEntry(actor.getName(), colorSelectionCallback, brightnessCallback, transparencyCallback);
        Vector4 actorColor = actor.getColor();
        if (actorColor!=null) {
            float[] colorData = actorColor.toArray();
            if (colorData!=null) {
                int red=(int)(colorData[0] * 255);
                int green=(int)(colorData[1] * 255);
                int blue=(int)(colorData[2] * 255);
                rowPanel.setEntryStatusColor(actor.getName(), new Color(red, green, blue));
            }
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

    private SyncedCallback createBrightnessCallback(final Actor actor) {
        return new SyncedCallback() {
            @Override
            public void performAction(Object o) {
                Float brightness=(Float)o;
                actor.setBrightness(brightness);
                EventManager.sendEvent(this, new ActorModifiedEvent());
            }
        };
    }

    private SyncedCallback createTransparencyCallback(final Actor actor) {
        return new SyncedCallback() {
            @Override
            public void performAction(Object o) {
                Float transparency=(Float)o;
                actor.setTransparency(transparency);
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
