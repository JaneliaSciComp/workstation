package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.janelia.it.workstation.gui.geometric_search.viewer.event.ActorSetVisibleEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.EventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * Created by murphys on 9/9/2015.
 */
public class MeshScrollableRowPanel extends ScrollableColorRowPanel {

    private static final Logger logger = LoggerFactory.getLogger(MeshScrollableRowPanel.class);

    public static final String EDGEFALLOFF_CALLBACK = "EDGEFALLOFF_CALLBACK";
    public static final String INTENSITY_CALLBACK = "INTENSITY_CALLBACK";
    public static final String AMBIENT_CALLBACK = "AMBIENT_CALLBACK";

    public void addEntry(final String name, boolean isInitiallyVisible, Map<String,SyncedCallback> callbackMap) throws Exception {

        SyncedCallback colorCallback = getCallback(COLOR_CALLBACK, callbackMap);
        SyncedCallback edgefalloffCallback = getCallback(EDGEFALLOFF_CALLBACK, callbackMap);
        SyncedCallback intensityCallback = getCallback(INTENSITY_CALLBACK, callbackMap);
        SyncedCallback ambientCallback = getCallback(AMBIENT_CALLBACK, callbackMap);

        logger.info("Adding row for name="+name);
        final MeshColorSelectionRow l = new MeshColorSelectionRow(name, this);
        l.setColorSelectionCallback(colorCallback);
        l.setEdgefalloffCallback(edgefalloffCallback);
        l.setIntensityCallback(intensityCallback);
        l.setAmbientCallback(ambientCallback);
        l.setName(name);
        l.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        l.setBorder(BorderFactory.createBevelBorder(1));
        l.getVisibleCheckBox().setSelected(isInitiallyVisible);
        final ScrollableColorRowPanel actionSource=this;

        l.getVisibleCheckBox().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.info("checkbox set to="+l.getVisibleCheckBox().isSelected());
                EventManager.sendEvent(actionSource, new ActorSetVisibleEvent(name, l.getVisibleCheckBox().isSelected()));
            }
        });

        components.add(l);
        rowPanel.add(l);
    }

}