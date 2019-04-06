package org.janelia.it.workstation.gui.framework.console;

import java.awt.*;
import java.io.Serializable;

/**
 * This is a legacy class which is not longer used, but needs to stay in this package because it has been serialized into users' settings. 
 */
@Deprecated
public class BrowserPosition implements Serializable {
    private Dimension consoleSize;
    private Dimension screenSize;
    private Point consoleLocation;
    private int verticalDividerLocation, horizontalLeftDividerLocation, horizontalRightDividerLocation;

    void setVerticalDividerLocation(int location) {
        verticalDividerLocation = location;
    }

    void setHorizontalLeftDividerLocation(int location) {
        horizontalLeftDividerLocation = location;
    }

    void setHorizontalRightDividerLocation(int location) {
        horizontalRightDividerLocation = location;
    }

    int getVerticalDividerLocation() {
        return verticalDividerLocation;
    }

    int getHorizontalLeftDividerLocation() {
        return horizontalLeftDividerLocation;
    }

    int getHorizontalRightDividerLocation() {
        return horizontalRightDividerLocation;
    }

    Dimension getScreenSize() {
        return screenSize;
    }

    void setScreenSize(Dimension dimension) {
        screenSize = dimension;
    }

    Dimension getBrowserSize() {
        return consoleSize;
    }

    Point getBrowserLocation() {
        return consoleLocation;
    }

    void setBrowserLocation(Point location) {
        consoleLocation = location;
    }

    void setBrowserSize(Dimension dimension) {
        consoleSize = dimension;
    }
}
