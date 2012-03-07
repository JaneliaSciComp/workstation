package org.janelia.it.FlyWorkstation.gui.application;

import org.janelia.it.FlyWorkstation.shared.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:10 PM
 * This panel is the main part of the splash screen.
 */
public class SplashPanel extends JPanel {
    public SplashPanel() {
        try {
            jbInit();
        }
        catch (Exception e) {
            try {
//                client.gui.framework.session_mgr.SessionMgr.getSessionMgr()
//                                                      .handleException(e);
            }
            catch (Exception ex1) {
                e.printStackTrace();
            }
        }

    }

    private void jbInit() throws Exception {
        setBackground(Color.white);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        try {
            ImageIcon bkgdImageIcon = Utils.getClasspathImage("flylight_logo_modified101110.png");
            graphics.drawImage(bkgdImageIcon.getImage(), (this.getWidth() - bkgdImageIcon.getIconWidth()) / 2, (this.getHeight() - bkgdImageIcon.getIconHeight()) / 2, null);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
