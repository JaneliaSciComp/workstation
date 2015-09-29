package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerRenderer;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.BackgroundColorChangeEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.BlendMethodChangeEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.EventManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by murphys on 9/4/2015.
 */
public class ViewerControlPanel extends JPanel {

    ColorSelectionPanel colorSelectionPanel=new ColorSelectionPanel();
    Knob brightnessKnob;
    Color lastSelectedColor=new Color(255,255,255);
    JButton mixButton;
    JButton mipButton;

    public ViewerControlPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        colorSelectionPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point panelPoint = e.getPoint();
                Point imgPoint = colorSelectionPanel.toImageContext(panelPoint);
                lastSelectedColor = colorSelectionPanel.getColorFromClickCoordinate(imgPoint);
                sendBrightnessAdjustedBackgroundColorChangeEvent();
            }
        });
        add(colorSelectionPanel);

        final ViewerControlPanel thisViewerControlPanel=this;

        mixButton=new JButton("mix");
        final JButton thisMixButton=mixButton;
        mipButton=new JButton("mip");
        final JButton thisMipButton=mipButton;

        mixButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setBlendMethod(VoxelViewerRenderer.BLEND_METHOD_MIX);
            }
        });
        add(mixButton);


        mipButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setBlendMethod(VoxelViewerRenderer.BLEND_METHOD_MIP);
            }
        });
        add(mipButton);


        brightnessKnob=new Knob("B",0.0, 3.0, 1.0, true);
        brightnessKnob.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                sendBrightnessAdjustedBackgroundColorChangeEvent();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
            }
        });
        add(brightnessKnob);
    }

    private void sendBrightnessAdjustedBackgroundColorChangeEvent() {
        int red=(int)(lastSelectedColor.getRed()*brightnessKnob.getValue());
        int green=(int)(lastSelectedColor.getGreen()*brightnessKnob.getValue());
        int blue=(int)(lastSelectedColor.getBlue()*brightnessKnob.getValue());
        if (red>255) {
            red=255;
        }
        if (green>255) {
            green=255;
        }
        if (blue>255) {
            blue=255;
        }
        Color newBackgroundColor=new Color(red, green, blue);
        EventManager.sendEvent(this, new BackgroundColorChangeEvent(newBackgroundColor));
    }

    public void setBlendMethod(int blendMethod) {

        if (blendMethod==VoxelViewerRenderer.BLEND_METHOD_MIX) {
            mipButton.setForeground(new Color(150, 150, 150));
            mixButton.setForeground(new Color(255, 255, 0));
            EventManager.sendEvent(this, new BlendMethodChangeEvent(VoxelViewerRenderer.BLEND_METHOD_MIX));
        } else if (blendMethod==VoxelViewerRenderer.BLEND_METHOD_MIP) {
            mixButton.setForeground(new Color(150, 150, 150));
            mipButton.setForeground(new Color(255, 255, 0));
            EventManager.sendEvent(this, new BlendMethodChangeEvent(VoxelViewerRenderer.BLEND_METHOD_MIP));
        }

    }

}
