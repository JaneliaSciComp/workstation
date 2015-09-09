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
public class VolumeScrollableRowPanel extends ScrollableColorRowPanel {

    private static final Logger logger = LoggerFactory.getLogger(VolumeScrollableRowPanel.class);

    public static final String BRIGHTNESS_CALLBACK = "BRIGHTNESS_CALLBACK";
    public static final String TRANSPARENCY_CALLBACK = "TRANSPARENCY_CALLBACK";

    public void addEntry(final String name, Map<String,SyncedCallback> callbackMap) throws Exception {

        SyncedCallback colorCallback = getCallback(COLOR_CALLBACK, callbackMap);
        SyncedCallback brightnessCallback = getCallback(BRIGHTNESS_CALLBACK, callbackMap);
        SyncedCallback transparencyCallback = getCallback(TRANSPARENCY_CALLBACK, callbackMap);

        logger.info("Adding row for name="+name);
        final VolumeColorSelectionRow l = new VolumeColorSelectionRow(name);
        l.setColorSelectionCallback(colorCallback);
        l.setBrightnessCallback(brightnessCallback);
        l.setTransparencyCallback(transparencyCallback);
        l.setName(name);
        l.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        l.setBorder(BorderFactory.createBevelBorder(1));
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
