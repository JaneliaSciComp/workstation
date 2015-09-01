package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.Actor;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.ActorModel;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * Created by murphys on 8/20/2015.
 */
public class ActorPanel extends ScrollableColorRowPanel implements VoxelViewerEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ActorPanel.class);

    @Override
    public void processEvent(VoxelViewerEvent event) {
        logger.info("processEvent() event type="+event.getClass().getName());
        if (event instanceof ActorAddedEvent) {
            ActorAddedEvent actorAddedEvent=(ActorAddedEvent)event;
            final Actor actor=actorAddedEvent.getActor();
            addEntry(actor.getName(), new SyncedCallback() {
                @Override
                public void performAction(Object o) {
                    Color selectedColor=(Color)o;
                    Vector4 colorVector=new Vector4(0,0,0,1f);
                    selectedColor.getRGBColorComponents(colorVector.toArray());
                    actor.setColor(colorVector);
                    setEntryStatusColor(actor.getName(), selectedColor);
                    EventManager.sendEvent(this, new ActorModifiedEvent());
                }
            });
            Vector4 actorColor = actor.getColor();
            if (actorColor!=null) {
                float[] colorData = actorColor.toArray();
                if (colorData!=null) {
                    int red=(int)(colorData[0] * 255);
                    int green=(int)(colorData[1] * 255);
                    int blue=(int)(colorData[2] * 255);
                    setEntryStatusColor(actor.getName(), new Color(red, green, blue));
                }
            }
        } else if (event instanceof ActorsClearAllEvent) {
            clear();
        }
    }

}
