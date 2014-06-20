package org.janelia.it.workstation.shared.util;

import java.util.HashMap;
import java.util.Map;

/**
 * used by the SWCReader class to hold each point in the neuron
 *
 * see http://research.mssm.edu/cnic/swc.html; node holds info
 * from one line in SWC file
 */
public class SWCNode {

    private int index;
    private int segmentType;
    private double x, y, z;
    private double radius;
    private int parentIndex;

    // these defs are from the above website; I'm tempted to build the inverse map,
    //  plus maybe an enum to hold constants mapping to the strings below, but I'll
    //  wait until I really need it
    public static final Map<Integer, String> SEGMENT_TYPES = new HashMap<Integer, String>();
    static {
        SEGMENT_TYPES.put(0, "undefined");
        SEGMENT_TYPES.put(1, "soma");
        SEGMENT_TYPES.put(2, "axon");
        SEGMENT_TYPES.put(3, "dendrite");
        SEGMENT_TYPES.put(4, "apical dendrite");
        SEGMENT_TYPES.put(5, "fork point");
        SEGMENT_TYPES.put(6, "end point");
        SEGMENT_TYPES.put(7, "custom");
    }

    /**
     * create a node from a line of a swc file; null if it fails
     */
    public static SWCNode parseLine(String line) {
        if (line == null) {
            return null;
        }

        String [] items = line.split("\\s+");
        if (items.length != 7) {
            return null;
        }

        return new SWCNode(
                Integer.parseInt(items[0]),
                Integer.parseInt(items[1]),
                Double.parseDouble(items[2]),
                Double.parseDouble(items[3]),
                Double.parseDouble(items[4]),
                Double.parseDouble(items[5]),
                Integer.parseInt(items[6])
        );
    }

    /**
     * @param index = index of node
     * @param segmentType = segment type; see types in code comments
     * @param x, y, z = location
     * @param radius = radius at node
     * @param parentIndex = index of parent node (-1 = no parent)
     */
    public SWCNode(int index, int segmentType, double x, double y, double z,
        double radius, int parentIndex) {

        this.index = index;
        this.segmentType = segmentType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.parentIndex = parentIndex;

    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getSegmentType() {
        return segmentType;
    }

    public void setSegmentType(int segmentType) {
        this.segmentType = segmentType;
    }

    public String getSegmentTypeString() {
        return SEGMENT_TYPES.get(segmentType);
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public int getParentIndex() {
        return parentIndex;
    }

    public void setParentIndex(int parentIndex) {
        this.parentIndex = parentIndex;
    }
}
