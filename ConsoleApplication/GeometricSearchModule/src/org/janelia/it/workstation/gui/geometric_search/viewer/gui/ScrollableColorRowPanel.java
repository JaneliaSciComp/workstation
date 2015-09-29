package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.janelia.it.workstation.gui.geometric_search.viewer.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

    public static int DEFAULT_WIDTH = 410;
    public static int DEFAULT_HEIGHT = 800;

    protected JPanel rowPanel = new JPanel();

    protected java.util.List<Component> components=new ArrayList<>();

    public ScrollableColorRowPanel() {
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(rowPanel);
        scrollPane.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        add(scrollPane);
    }

    public abstract void addEntry(final String name, boolean isInitiallyVisible, Map<String,SyncedCallback> callbackMap) throws Exception;

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

    public void selectAllRows() {
        boolean changed=false;
        EventManager.setDisallowViewerRefresh(true);
        for (Component component : components) {
            if (component instanceof ColorSelectionRow) {
                ColorSelectionRow row = (ColorSelectionRow)component;
                JCheckBox visibleCheckBox = row.getVisibleCheckBox();
                if (!visibleCheckBox.isSelected()) {
                    visibleCheckBox.setSelected(true);
                    visibleCheckBox.getActionListeners()[0].actionPerformed(null);
                    changed=true;
                }
            }
        }
        EventManager.setDisallowViewerRefresh(false);
        if (changed) {
            EventManager.sendEvent(this, new ActorModifiedEvent());
        }
    }

    public void deselectAllRows() {
        boolean changed=false;
        EventManager.setDisallowViewerRefresh(true);
        for (Component component : components) {
            if (component instanceof ColorSelectionRow) {
                ColorSelectionRow row = (ColorSelectionRow)component;
                JCheckBox visibleCheckBox = row.getVisibleCheckBox();
                if (visibleCheckBox.isSelected()) {
                    visibleCheckBox.setSelected(false);
                    visibleCheckBox.getActionListeners()[0].actionPerformed(null);
                    changed=true;
                }
            }
        }
        EventManager.setDisallowViewerRefresh(false);
        if (changed) {
            EventManager.sendEvent(this, new ActorModifiedEvent());
        }
    }

    protected void setupActorStateCallbacks(final String actorName, final ColorSelectionRow c) {
        final ScrollableColorRowPanel actionSource = this;

        c.getVisibleCheckBox().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.info("checkbox set to=" + c.getVisibleCheckBox().isSelected());
                EventManager.sendEvent(actionSource, new ActorSetVisibleEvent(actorName, c.getVisibleCheckBox().isSelected()));
            }
        });

        c.getAllButton().addActionListener(getAOSActionListener(actionSource, c, actorName, ActorAOSEvent.ALL_TYPE, c.getAllButton()));
        c.getNoneButton().addActionListener(getAOSActionListener(actionSource, c, actorName, ActorAOSEvent.OFF_TYPE, c.getNoneButton()));
        c.getSoloButton().addActionListener(getAOSActionListener(actionSource, c, actorName, ActorAOSEvent.SOLO_TYPE, c.getSoloButton()));

    }

    private ActionListener getAOSActionListener(final ScrollableColorRowPanel actionSource, final ColorSelectionRow c, final String actorName, final String aosType,
                                                final ColorSelectionRow.GroupSelectionButton eventButton) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<ColorSelectionRow.GroupSelectionButton> updateButtons=new ArrayList<>();

                if (!c.getAllButton().equals(eventButton)) {
                    updateButtons.add(c.getAllButton());
                }
                if (!c.getNoneButton().equals(eventButton)) {
                    updateButtons.add(c.getNoneButton());
                }
                if (!c.getSoloButton().equals(eventButton)) {
                    updateButtons.add(c.getSoloButton());
                }

                if (eventButton.isSelected()) {
                    // Turn off
                    eventButton.setSelected(false);
                    EventManager.sendEvent(actionSource, new ActorAOSEvent(actorName, aosType, false));
                } else {
                    // Turn on
                    eventButton.setSelected(true);
                    for (ColorSelectionRow.GroupSelectionButton aosButton : updateButtons) {
                        if (aosButton.isSelected()) {
                            aosButton.setSelected(false);
                        }
                    }
                    EventManager.sendEvent(actionSource, new ActorAOSEvent(actorName, aosType, true));
                }
            }
        };
    }


}
