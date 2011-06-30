package org.janelia.it.FlyWorkstation.gui.application;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:10 PM
 * This panel is the main part of the splash screen.
 */
public class SplashPanel extends JPanel {
    JPanel panel1 = new JPanel();
    JPanel panel2 = new JPanel();

    JLabel logoImageControl = new JLabel(new ImageIcon("/Users/"+System.getenv("USER")+"/Dev/jacs/console/target/classes/org/janelia/it/FlyWorkstation/gui/application/jfrc-bg4.jpg"));

    public SplashPanel() {
        try {
            jbInit();
        } catch (Exception e) {
            try {
//                client.gui.framework.session_mgr.SessionMgr.getSessionMgr()
//                                                      .handleException(e);
            } catch (Exception ex1) {
                e.printStackTrace();
            }
        }

    }

    private void jbInit() throws Exception {
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
        panel1.setBackground(Color.white);
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
        panel2.setBackground(Color.white);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        panel2.add(Box.createVerticalGlue());
        panel2.add(logoImageControl);
        panel2.add(Box.createVerticalGlue());
        setBackground(Color.white);
        add(panel1);
        add(panel2);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = getSize();
        // Pad the splash for now
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
