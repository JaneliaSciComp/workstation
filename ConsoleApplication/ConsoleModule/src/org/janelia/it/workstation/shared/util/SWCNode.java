package org.janelia.it.workstation.shared.util;

import java.util.HashMap;
import java.util.Map;

/**
 * used by the SWCData class to hold each point in the neuron
 *
 * see http://research.mssm.edu/cnic/swc.html; node holds info
 * from one line in SWC file
 */
public class SWCNode {

    private static final Map<Integer,SegmentType> decodeToSegment = new HashMap<>();
    public static enum SegmentType {
        undefined(0), soma(1), axon(2), dendrite(3), apical_dendrite(4), fork_point(5), end_point(6), custom(7);

        private int decodeNum;
        public static SegmentType getSegmentType( String typeName ) {
            return SegmentType.valueOf(typeName.replaceAll(" ","_"));
        }
        
        private SegmentType( int decodeNum ) {
            this.decodeNum = decodeNum;
            decodeToSegment.put( decodeNum, this );
        }
       
        public int decode() {
            return decodeNum;
        }
        
        @Override
        public String toString() {
            return this.name().replaceAll("_"," ");
        }
    }
    
    private int index;
    private SegmentType segmentType;
    private double x, y, z;
    private double radius;
    private int parentIndex;

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
                decodeToSegment.get(Integer.parseInt(items[1])),
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
    public SWCNode(int index, SegmentType segmentType, double x, double y, double z,
        double radius, int parentIndex) {

        this.index = index;
        this.segmentType = segmentType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.parentIndex = parentIndex;

    }

    /**
     * simple validity checks; not returning a reason at this point
     */
    public boolean isValid() {
        // couple simple validity checks
        if (radius <= 0.0) {
            return false;
        }
        return true;
    }

    /**
     * returns a string (no newline) that is suitable for writing the node
     * into an SWC file
     */
    public String toSWCline () {
        return String.format("%d\t%d\t%f\t%f\t%f\t%f\t%d",
                index,
                segmentType.decode(),
                x, y, z,
                radius,
                parentIndex);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public SegmentType getSegmentType() {
        return segmentType;
    }

    public void setSegmentType(SegmentType segmentType) {
        this.segmentType = segmentType;
    }

    public String getSegmentTypeString() {
        return segmentType.toString();
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
