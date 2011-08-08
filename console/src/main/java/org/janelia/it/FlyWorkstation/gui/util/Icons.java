package org.janelia.it.FlyWorkstation.gui.util;

import org.janelia.it.FlyWorkstation.gui.framework.console.MissingIcon;
import org.janelia.it.FlyWorkstation.shared.util.Utils;

import javax.swing.*;
import java.io.FileNotFoundException;

public class Icons {

    public static Icon loadingIcon;
    public static Icon missingIcon = new MissingIcon();
    public static Icon expandAllIcon;
    public static Icon collapseAllIcon;

    static {
        try {
            loadingIcon = Utils.getClasspathImage("spinner.gif");
            expandAllIcon = Utils.getClasspathImage("expand_all.png");
            collapseAllIcon = Utils.getClasspathImage("collapse_all.png");
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
