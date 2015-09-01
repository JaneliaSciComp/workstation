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
public class ScrollableColorRowPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ScrollableColorRowPanel.class);

    protected JPanel rowPanel = new JPanel();

    protected java.util.List<Component> components=new ArrayList<>();

    public ScrollableColorRowPanel() {
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(rowPanel);
        scrollPane.setPreferredSize(new Dimension(250, 800));
        add(scrollPane);
    }

    public void addEntry(final String name, SyncedCallback colorCallback) {
        logger.info("Adding row for name="+name);
        final ColorSelectionRow l = new ColorSelectionRow(name);
        l.setColorSelectionCallback(colorCallback);
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

    public void setEntryStatusColor(String name, Color color) {
        ColorSelectionRow row=getRowByName(name);
        if (row!=null) {
            row.setColorStatus(color);
        }
    }

    private ColorSelectionRow getRowByName(String name) {
        for (Component component : components) {
            if (component instanceof ColorSelectionRow) {
                ColorSelectionRow row = (ColorSelectionRow)component;
                if (row.getName().equals(name)) {
                    return row;
                }
            }
        }
        return null;
    }

    public void clear() {

        logger.info("clear() start");

        for (Component component : components) {
            logger.info("removing component="+component.getName());
            rowPanel.remove(component);
        }
        components.clear();
    }

}
