package org.janelia.it.workstation.gui.large_volume_viewer;

import java.awt.Point;
import java.text.DecimalFormat;
import org.janelia.it.workstation.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.action.BasicMouseMode;

/**
 * Consistently print the coords in palatable format.
 *
 * @author fosterl
 */
public class MicronCoordsFormatter {
    private BasicMouseMode pointComputer;
    public MicronCoordsFormatter(BasicMouseMode pointComputer) {
        this.pointComputer = pointComputer;
    }
    public String formatForPresentation(Point mouseLocation) {
        Vec3 xyz = pointComputer.worldFromPixel(mouseLocation);
        DecimalFormat fmt = new DecimalFormat("0.0");
        String msg = "["
                + fmt.format(xyz.getX())
                + ", " + fmt.format(xyz.getY())
                + ", " + fmt.format(xyz.getZ())
                + "] \u00B5m"; // micrometers. Maybe I should use pixels (also?)?
        return msg;
    }
}
