package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

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
 * Created by murphys on 8/18/2015.
 */
public class ScrollableRowPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ScrollableRowPanel.class);

    protected JPanel rowPanel = new JPanel();

    protected java.util.List<Component> components=new ArrayList<>();

    public ScrollableRowPanel() {
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(rowPanel);
        scrollPane.setPreferredSize(new Dimension(250, 800));
        add(scrollPane);
    }

    public void addEntry(final String name) {
        logger.info("Adding row for name="+name);
        final JButton l = new JButton(name);
        l.setName(name);
        l.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        l.setBorder(BorderFactory.createBevelBorder(1));
        final ScrollableRowPanel actionSource=this;
        l.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EventManager.sendEvent(actionSource, new RowSelectedEvent(l));
            }
        });
        components.add(l);
        rowPanel.add(l);
    }

    public void clear() {

        logger.info("clear() start");

        for (Component component : components) {
            logger.info("removing component="+component.getName());
            rowPanel.remove(component);
        }
        components.clear();
    }

    public void setSelectedRowByName(String name) {
        for (Component component : components) {
            if (component instanceof JButton) {
                JButton jButton = (JButton)component;
                if (jButton.getName().equals(name)) {
                    jButton.setForeground(new Color(200, 0, 0));
                } else {
                    jButton.setForeground(new Color(100, 100, 100));
                }
            }
        }
    }


}
