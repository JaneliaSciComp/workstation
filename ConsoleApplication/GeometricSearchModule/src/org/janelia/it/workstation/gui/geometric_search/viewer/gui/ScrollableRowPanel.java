package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by murphys on 8/18/2015.
 */
public class ScrollableRowPanel extends JPanel {

    JPanel rowPanel = new JPanel();

    public ScrollableRowPanel() {
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(rowPanel);
        scrollPane.setPreferredSize(new Dimension(250, 800));
        add(scrollPane);
    }

    public void addEntry(String name) {
        JButton l = new JButton(name);
        l.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        l.setBorder(BorderFactory.createBevelBorder(1));
        rowPanel.add(l);
    }

}
