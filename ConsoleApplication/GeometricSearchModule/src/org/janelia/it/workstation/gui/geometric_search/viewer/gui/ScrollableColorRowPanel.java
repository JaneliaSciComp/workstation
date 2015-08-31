package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.janelia.it.workstation.gui.geometric_search.viewer.event.ActorSetVisibleEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.EventManager;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.RowSelectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Created by murphys on 8/28/2015.
 */
public class ScrollableColorRowPanel extends ScrollableRowPanel {

    private static final Logger logger = LoggerFactory.getLogger(ScrollableColorRowPanel.class);

    public ScrollableColorRowPanel() {
        super();
    }

    public void addEntry(final String name) {
        logger.info("Adding row for name="+name);
        final ColorSelectionRow l = new ColorSelectionRow(name);
        l.setName(name);
        l.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        l.setBorder(BorderFactory.createBevelBorder(1));
        final ScrollableRowPanel actionSource=this;

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

    public void setSelectedRowByName(String name) {
        for (Component component : components) {
            if (component instanceof ColorSelectionRow) {
                ColorSelectionRow row = (ColorSelectionRow)component;
                if (row.getName().equals(name)) {
                    row.setForeground(new Color(200, 0, 0));
                } else {
                    row.setForeground(new Color(100, 100, 100));
                }
            }
        }
    }
}
