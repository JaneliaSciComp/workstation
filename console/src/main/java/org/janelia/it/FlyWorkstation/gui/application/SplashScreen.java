package org.janelia.it.FlyWorkstation.gui.application;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:08 PM
 * This class acts as the general splash screen for the app
 */
public final class SplashScreen extends JWindow {
    protected void processWindowEvent(final WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            dispose();
        }

        super.processWindowEvent(e);
    }

    private final JPanel outerPanel = new JPanel();
    private final JLabel statusLabel = new JLabel("");

    public SplashScreen() {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        jbInit();
        pack();
    }

    public void setStatusText(final String text) {
        statusLabel.setText(text);
    }

    private void jbInit() {
        getContentPane().setBackground(Color.white);
        outerPanel.setLayout(new BorderLayout(10, 0));


        //new BoxLayout(outerPanel,BoxLayout.Y_AXIS));
        outerPanel.add(new SplashPanel(), BorderLayout.NORTH);

        //    outerPanel.setBackground(Color.white);
        final JPanel rightsPanel = new JPanel();
        rightsPanel.setLayout(new BoxLayout(rightsPanel, BoxLayout.Y_AXIS));
        rightsPanel.setBackground(Color.white);
        rightsPanel.add(new JLabel("Fly Workstation"));
        rightsPanel.add(new JLabel("Howard Hughes Medical Institute"));
        // Tack on Status text field to report on application initialization
        rightsPanel.add(statusLabel);

        final JPanel outerRightsPanel = new JPanel();
        outerRightsPanel.setLayout(
                new BoxLayout(outerRightsPanel, BoxLayout.X_AXIS));
        outerRightsPanel.setBackground(Color.white);
        outerRightsPanel.add(rightsPanel);
        outerPanel.add(outerRightsPanel, BorderLayout.SOUTH);

        outerPanel.setBorder(
                new BevelBorder(BevelBorder.RAISED, Color.lightGray,
                        Color.darkGray));
        getContentPane().add(outerPanel);
        pack();

        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final Dimension frameSize = getSize();
        frameSize.setSize(frameSize.getWidth()+200, frameSize.getHeight()+50);

        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }

        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }

        setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);
    }
}
