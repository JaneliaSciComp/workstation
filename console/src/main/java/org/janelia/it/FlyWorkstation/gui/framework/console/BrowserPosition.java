package org.janelia.it.FlyWorkstation.gui.framework.console;

import java.awt.*;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 2/8/11
 * Time: 12:40 PM
 * Class to keep track of Console real-estate
 */
public class BrowserPosition implements Serializable {
    private Dimension consoleSize;
    private Dimension screenSize;
    private Point consoleLocation;
    private int verticalDividerLocation, horizontalDividerLocation;

    void setVerticalDividerLocation(int location) {
        verticalDividerLocation = location;
    }

    void setHorizontalDividerLocation(int location) {
        horizontalDividerLocation = location;
    }

    int getVerticalDividerLocation() {
        return verticalDividerLocation;
    }

    int getHorizontalDividerLocation() {
        return horizontalDividerLocation;
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
