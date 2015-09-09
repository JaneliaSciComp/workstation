package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

/**
 * Created by murphys on 8/28/2015.
 */
public class ColorSelectionRow extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ColorSelectionRow.class);

    private static final int COLOR_STATUS_WIDTH=20;
    private static final int COLOR_STATUS_HEIGHT=20;

    JCheckBox visibleCheckBox;
    JLabel nameLabel;
    ColorPanel colorStatusPanel;
    ColorSelectionPanel colorSelectionPanel;
    SyncedCallback colorSelectionCallback;


    public ColorSelectionRow(String name) {
        setName(name);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setPreferredSize(new Dimension(320, 45));
        setMaximumSize(new Dimension(320, 45));
        visibleCheckBox=new JCheckBox();
        visibleCheckBox.setSelected(true);
        nameLabel=new JLabel(name);
        colorStatusPanel=new ColorPanel(COLOR_STATUS_WIDTH, COLOR_STATUS_HEIGHT, new Color(0, 0, 0));

        colorSelectionPanel=new ColorSelectionPanel();
        colorSelectionPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point panelPoint = e.getPoint();
                Point imgPoint = colorSelectionPanel.toImageContext(panelPoint);
                Color selectedColor=colorSelectionPanel.getColorFromClickCoordinate(imgPoint);
                if (colorSelectionCallback!=null) {
                    colorSelectionCallback.performAction(selectedColor);
                }
            }
        });

        add(visibleCheckBox);
        add(colorStatusPanel);
        add(nameLabel);
        add(colorSelectionPanel);

    }

    public JCheckBox getVisibleCheckBox() {
        return visibleCheckBox;
    }

    public void setVisibleCheckBox(JCheckBox visibleCheckBox) {
        this.visibleCheckBox = visibleCheckBox;
    }

    public void setColorStatus(final Color color) {
        colorStatusPanel.setColor(color);
    }

    public void setColorSelectionCallback(SyncedCallback callback) {
        this.colorSelectionCallback=callback;
    }

}
