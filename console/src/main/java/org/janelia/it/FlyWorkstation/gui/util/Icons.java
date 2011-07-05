package org.janelia.it.FlyWorkstation.gui.util;

import java.io.FileNotFoundException;

import javax.swing.Icon;

import org.janelia.it.FlyWorkstation.gui.framework.console.MissingIcon;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

public class Icons {
	
	public static Icon loadingIcon;
	public static Icon missingIcon = new MissingIcon();
	
	static {
        try {
        	loadingIcon = Utils.getClasspathImage("spinner.gif");
        }
        catch (FileNotFoundException e) {
        	e.printStackTrace();
        }
	}
}
