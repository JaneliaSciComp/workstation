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
    private static final int MAX_NAME_CHARS=10;
    private static final int ROW_WIDTH=385;
    private static final int ROW_HEIGHT=45;

    JCheckBox visibleCheckBox;
    JPanel groupManagementPanel;
    JPanel groupSelectionPanel;
    JLabel nameLabel;
    GroupSelectionButton allButton;
    GroupSelectionButton noneButton;
    GroupSelectionButton soloButton;
    ColorPanel colorStatusPanel;
    ColorSelectionPanel colorSelectionPanel;
    SyncedCallback colorSelectionCallback;

    private static class GroupSelectionButton extends JButton {

        public GroupSelectionButton(String name) {
            super(name);
            this.setFont(new Font("Arial", Font.BOLD, 9));
        }
    }


    public ColorSelectionRow(String name) {

        try {

            setName(name);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setPreferredSize(new Dimension(ROW_WIDTH, ROW_HEIGHT));
            setMaximumSize(new Dimension(ROW_WIDTH, ROW_HEIGHT));

            visibleCheckBox = new JCheckBox();
            visibleCheckBox.setSelected(true);

            groupManagementPanel = new JPanel();
            groupManagementPanel.setLayout(new BoxLayout(groupManagementPanel, BoxLayout.Y_AXIS));

            groupSelectionPanel = new JPanel();
            groupSelectionPanel.setLayout(new BoxLayout(groupSelectionPanel, BoxLayout.X_AXIS));

            String normalizedName = getNormalizedName(name);
            nameLabel = new JLabel(normalizedName, JLabel.LEFT);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 10));
            JPanel nameBox=new JPanel();
            nameBox.setLayout(new BoxLayout(nameBox, BoxLayout.X_AXIS));
            nameBox.add(nameLabel);

            allButton = new GroupSelectionButton("A");
            noneButton = new GroupSelectionButton("-");
            soloButton = new GroupSelectionButton("S");

            groupSelectionPanel.add(allButton);
            groupSelectionPanel.add(noneButton);
            groupSelectionPanel.add(soloButton);

            groupManagementPanel.add(nameBox);
            groupManagementPanel.add(groupSelectionPanel);

            colorStatusPanel = new ColorPanel(COLOR_STATUS_WIDTH, COLOR_STATUS_HEIGHT, new Color(0, 0, 0));
            colorSelectionPanel = new ColorSelectionPanel();
            colorSelectionPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Point panelPoint = e.getPoint();
                    Point imgPoint = colorSelectionPanel.toImageContext(panelPoint);
                    Color selectedColor = colorSelectionPanel.getColorFromClickCoordinate(imgPoint);
                    if (colorSelectionCallback != null) {
                        colorSelectionCallback.performAction(selectedColor);
                    }
                }
            });

            add(visibleCheckBox);
            add(colorStatusPanel);
            add(groupManagementPanel);
            add(colorSelectionPanel);

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }

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

    public String getNormalizedName(String name) {
        int initialLength=name.length();
        if (initialLength>MAX_NAME_CHARS) {
            return name.substring(0, MAX_NAME_CHARS);
        } else if (initialLength<MAX_NAME_CHARS) {
            String pad="";
            for (int i=0;i<(MAX_NAME_CHARS-initialLength);i++){
                pad += " ";
            }
            return name + pad;
        }
        return name;
    }

}
