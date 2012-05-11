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
	
	private boolean showSplashImage = true;
	
    public SplashPanel() {
        setBackground(Color.white);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        try {
        	if (showSplashImage) {
	            ImageIcon bkgdImageIcon = Utils.getClasspathImage("flylight_transparent_no_shadow.png");
	            graphics.drawImage(bkgdImageIcon.getImage(), (this.getWidth() - bkgdImageIcon.getIconWidth()) / 2, (this.getHeight() - bkgdImageIcon.getIconHeight()) / 2, null);
        	}
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

	public boolean isShowSplashImage() {
		return showSplashImage;
	}

	public void setShowSplashImage(boolean showSplashImage) {
		this.showSplashImage = showSplashImage;
	}
}
