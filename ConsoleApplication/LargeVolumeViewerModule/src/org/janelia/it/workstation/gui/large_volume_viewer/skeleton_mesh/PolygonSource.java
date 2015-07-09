/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

/**
 *
 * @author fosterl
 */
public class PolygonSource {
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;
    
    private int endPolygonSides;
    private double endPolygonRadius;
    public PolygonSource(int numSides, double radius) {
        this.endPolygonSides = numSides;
        this.endPolygonRadius = radius;
    }
    /**
     * Here is where the prototype polygons--those whose positions are modified
     * by angle transforms--are initially created.
     *
     * @param axis tells along which axis this prototype will align.
     * @return array of endPolygonSides, each of which is a coord triple.
     */
    public double[][] createAxisAlignedPrototypeEndPolygon(int axis) {
        double[][] prototypeEndPolygon = new double[endPolygonSides][];
        double fullcircle = Math.PI * 2.0;
        double thetaIncrement = fullcircle / endPolygonSides;
        if (axis == Z || axis == Y) {
            // Negation of increment is required, to force the vertices
            // to be laid out in clockwise fashion.  When end-caps are
            // created, their "outside widings" are counter clockwise,
            // which requires the overall polygon to be clockwise.
            thetaIncrement = -thetaIncrement;
            fullcircle = -fullcircle;
        }
        double theta = Math.PI;  // Position of first polygon point.
        for (int i = 0; i < endPolygonSides; i++) {
            theta += thetaIncrement;
            theta = theta % fullcircle;
            float[] calculatedCoords = new float[2];
            calculatedCoords[0] = (float) (Math.cos(theta) * endPolygonRadius);
            calculatedCoords[1] = (float) (Math.sin(theta) * endPolygonRadius);
            //System.out.println(String.format(
            //   "Theta=%f, x=%f, y=%f, cos=%f, sin=%f.  Iteration=%d\n", 
            //   theta, x, y, Math.cos(theta), Math.sin(theta), i));
            prototypeEndPolygon[i] = new double[3];
            int calcNum = 0;
            for (int coord = 0; coord < 3; coord++) {
                prototypeEndPolygon[i][coord] = (coord == axis) ? 0 : calculatedCoords[ calcNum++];
            }
        }
        //dumpPolygon("Prototype: axis=" + axis, prototypeEndPolygon);
        return prototypeEndPolygon;
    }

    public double[][] createZAxisAlignedPrototypeEndPolygon() {
        return createAxisAlignedPrototypeEndPolygon(Z);
    }


}
