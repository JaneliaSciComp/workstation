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
import java.util.Map;

/**
 * Created by murphys on 8/28/2015.
 */
public abstract class ScrollableColorRowPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ScrollableColorRowPanel.class);

    public static final String COLOR_CALLBACK = "COLOR_CALLBACK";

    public static int DEFAULT_WIDTH = 320;
    public static int DEFAULT_HEIGHT = 800;

    protected JPanel rowPanel = new JPanel();

    protected java.util.List<Component> components=new ArrayList<>();

    public ScrollableColorRowPanel() {
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(rowPanel);
        scrollPane.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        add(scrollPane);
    }

    public abstract void addEntry(final String name, Map<String,SyncedCallback> callbackMap) throws Exception;

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

    protected SyncedCallback getCallback(String key, Map<String, SyncedCallback> callbackMap) throws Exception {
        SyncedCallback syncedCallback=callbackMap.get(key);
        if (syncedCallback==null) {
            throw new Exception("Could not find SyncedCallback entry for key="+key);
        }
        return syncedCallback;
    }

}
