package org.janelia.it.workstation.gui.large_volume_viewer;

import java.awt.Point;

import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.workstation.gui.large_volume_viewer.action.BasicMouseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consistently print the coords in palatable format.
 *
 * @author fosterl
 */
public class MicronCoordsFormatter {
    private final static String MICRON_FORMAT = "[%2.1f, %2.1f, %2.1f] \u00B5m";
    private final static String DOUBLE_TUPLE_FORMAT = "%2.1f,%2.1f,%2.1f";
    private final static String INT_TUPLE_FORMAT = "%d,%d,%d";
    
    private Logger logger = LoggerFactory.getLogger(MicronCoordsFormatter.class);
    
    private BasicMouseMode pointComputer;
    public MicronCoordsFormatter(BasicMouseMode pointComputer) {
        this.pointComputer = pointComputer;
    }
    
    public String formatForPresentation(Point mouseLocation) {
        Vec3 xyz = pointComputer.worldFromPixel(mouseLocation);
        String msg = String.format(MICRON_FORMAT, xyz.getX(), xyz.getY(), xyz.getZ());
        return msg;
    }
    
    /** Given a message formatted with the MICRON_FORMAT, pry out its numbers. */
    public double[] unformat(String msg) {

        double[] rtnVal = new double[] { -1, -1, -1 };
        try {
            String[] commaGroups = msg.split(",");
            rtnVal[0] = Double.parseDouble(commaGroups[0].substring(1));
            rtnVal[1] = Double.parseDouble(commaGroups[1].trim());
            commaGroups[2] = commaGroups[2].trim();
            int pos = commaGroups[2].indexOf("]");
            rtnVal[2] = Double.parseDouble(commaGroups[2].substring(0, pos));
        } catch (Exception ex) {
            logger.error("Failed to unformat {}.", msg);
            ex.printStackTrace();
        }
        return rtnVal;
    }
    
    public double[] messageToTuple( String message ) {
        return unformat( message );
    }
    
    /** Turns 3D 'mouse location' into comma-separated, float values. */
    public String formatAsTuple( Point mouseLocation ) {
        Vec3 xyz = pointComputer.worldFromPixel(mouseLocation);
        return String.format(DOUBLE_TUPLE_FORMAT, xyz.getX(), xyz.getY(), xyz.getZ());
    }
    
    public String formatAsTuple( double x, double y, double z ) {
        return String.format(DOUBLE_TUPLE_FORMAT, x, y, z);
    }

    public String formatAsTuple(int x, int y, int z) {
        return String.format(INT_TUPLE_FORMAT, x, y, z);
    }
}
