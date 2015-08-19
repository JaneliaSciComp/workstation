package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Created by murphys on 8/18/2015.
 */
public class ScrollableRowPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ScrollableRowPanel.class);

    JPanel rowPanel = new JPanel();

    java.util.List<Component> components=new ArrayList<>();

    public ScrollableRowPanel() {
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(rowPanel);
        scrollPane.setPreferredSize(new Dimension(250, 800));
        add(scrollPane);
    }

    public void addEntry(String name) {
        logger.info("Adding row for name="+name);
        JButton l = new JButton(name);
        l.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        l.setBorder(BorderFactory.createBevelBorder(1));
        components.add(l);
        rowPanel.add(l);
    }

    public void clear() {
        for (Component component : components) {
            remove(component);
        }
        components.clear();
    }

}
