/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.janelia.it.jacs.shared.mesh_loader.Triangle;
import org.janelia.it.jacs.shared.mesh_loader.TriangleSource;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoBean;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoKey;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.ViewMatrixSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Can build up mesh enclosures over lines.  Lines are defined as pairs
 * of endpoints.
 * 
 * @author fosterl
 */
public class LineEnclosureFactory implements TriangleSource {
    
    private static Logger logger = LoggerFactory.getLogger(LineEnclosureFactory.class);
    private static final int X = 0, Y = 1, Z = 2;
    private static final int YZ = 0;
    private static final int ZX = 1;
    private static final int XY = 2;
    
    private static final double[] PROTOTYPE_NORMAL = {0, 0, -1};
    private List<VertexInfoBean> vertices = new ArrayList<>();
    private List<Triangle> triangles = new ArrayList<>();
    private ViewMatrixSupport matrixUtils = new ViewMatrixSupport();
    
    private int endPolygonSides = -1;
    private double endPolygonRadius; 
    
    private double[][] prototypeEndPolygon;
    private List<double[][]> endCapPolygonsHolder = new ArrayList<>();
    
    public LineEnclosureFactory(int endPolygonSides, double endPolygonRadius) {
        this.endPolygonSides = endPolygonSides;
        this.endPolygonRadius = endPolygonRadius;
        this.prototypeEndPolygon = createPrototypeEndPolygon();
    }

    /**
     * Makes a set of triangles (geometry) to make a "pipe" around
     * the pair of starting/ending coords.  May make overhangs for "elbow
     * convenience."
     * 
     * @param startingCoords one end.
     * @param endingCoords other end.
     */
    public void addEnclosure( double[] startingCoords, double[] endingCoords ) {
        if ( startingCoords.length != 3 ) {
            throw new IllegalArgumentException("3-D starting coords only.");
        }
        if (endingCoords.length != 3) {
            throw new IllegalArgumentException("3-D ending coords only.");
        }
        
        //List<double[][]> discardedEndCaps = makeEndPolygons(startingCoords, endingCoords);
        List<double[][]> endCaps = makeEndPolygonsNoTrig( startingCoords, endingCoords );
        addVertices(endCaps.get(0));
        addVertices(endCaps.get(1));
        Triangle t = new Triangle();
    }

    //--------------------------------------------IMPLEMENT TriangleSource
    @Override
    public List<VertexInfoBean> getVertices() {
        return vertices;
    }

    @Override
    public List<Triangle> getTriangleList() {
        return triangles;
    }

    //--------------------------------------------HELPERS
    protected void addVertices(double[][] poly) {
        for (int i = 0; i < poly.length; i++) {
            VertexInfoBean bean = new VertexInfoBean();
            VertexInfoKey key = new VertexInfoKey();
            key.setPosition(poly[i]);
            bean.setKey(key);
            vertices.add(bean);
            logger.info("Adding vertex {},{},{}", key.getPosition()[X], key.getPosition()[Y], key.getPosition()[Z]);
        }
    }

    private List<double[][]> makeEndPolygons( double[] startCoords, double[] endCoords ) {
        
        endCapPolygonsHolder.clear();
        
        // Get the three angles: about X, about Y, about Z.
        double[] lineUnitVector = normalize(getLineDelta(startCoords, endCoords));
        
        double aboutX = lineUnitVector[Z] == 0 ? 0 : Math.atan(lineUnitVector[Y] / lineUnitVector[Z]);
        double aboutY = lineUnitVector[Z] == 0 ? 0 : Math.atan(lineUnitVector[X] / lineUnitVector[Z]);
        double aboutZ = lineUnitVector[X] == 0 ? 0 : Math.atan(lineUnitVector[Y] / lineUnitVector[X]);
        
        // Now that we have our angles, we make the transform.
        Matrix transform = matrixUtils.getTransform3D(
                aboutX, aboutY, aboutZ,
                startCoords[X], startCoords[Y], startCoords[Z]);        
        endCapPolygonsHolder.add(producePolygon(transform));
        
        transform.set(0, 3, endCoords[X]);
        transform.set(1, 3, endCoords[Y]);
        transform.set(2, 3, endCoords[Z]);
        endCapPolygonsHolder.add(producePolygon(transform));
        
        return endCapPolygonsHolder;
    }
    
    private List<double[][]> makeEndPolygonsNoTrig(double[] startCoords, double[] endCoords) {

        endCapPolygonsHolder.clear();

        // Establish the positioning matrix.
        double[] lineDelta = getLineDelta(endCoords, startCoords);
        double[] lengthsInPlanes = getLengthsInPlanes( lineDelta );
        
        Matrix transformMatrix = matrixUtils.getTransform3D(
                lineDelta[Y] / lengthsInPlanes[YZ], lineDelta[Z] / lengthsInPlanes[YZ],
                lineDelta[Z] / lengthsInPlanes[ZX], lineDelta[X] / lengthsInPlanes[ZX],
                lineDelta[Y] / lengthsInPlanes[XY], lineDelta[X] / lengthsInPlanes[XY],
                startCoords[X], startCoords[Y], startCoords[Z]);
        
        endCapPolygonsHolder.add( producePolygon( transformMatrix ) );
        transformMatrix.set(0, 3, endCoords[X]);
        transformMatrix.set(1, 3, endCoords[Y]);
        transformMatrix.set(2, 3, endCoords[Z]);
        endCapPolygonsHolder.add( producePolygon( transformMatrix ) );
        return endCapPolygonsHolder;
    }
    
    private double[] getLengthsInPlanes( double[] lineDelta ) {
        return new double[] {
            Math.sqrt(lineDelta[Y] * lineDelta[Y] + lineDelta[Z] * lineDelta[Z]),
            Math.sqrt(lineDelta[Z] * lineDelta[Z] + lineDelta[X] * lineDelta[X]),
            Math.sqrt(lineDelta[Y] * lineDelta[Y] + lineDelta[X] * lineDelta[X]),
        };
    }

//    private double[][] makeEndPolygonForPoint( double[] coords, double[] deltasOverLength ) {
//        return makeEndPolygon(deltasOverLength[X], deltasOverLength[Y], deltasOverLength[Z], coords);        
//    }

//    protected double[][] makeEndPolygon(
//            Matrix transformMatrix,
//            double[] coords) {
//
//        double[][] polygon = new double[endPolygonSides][3];
//        // Clone the prototype.
//        for (int i = 0; i < endPolygonSides; i++) {
//            System.arraycopy(prototypeEndPolygon[i], 0, polygon[i], 0, 3);
//        }
//
//        for ( int i = 0; i < polygon.length; i++ ) {
//            polygon[i] = matrixUtils.transform( transformMatrix, polygon[i] );
//        }
//        return polygon;
//    }
    
    private double[] normalize( double[] distance ) {
        double magnitude = getMagnitude( distance );
        distance[0] /= magnitude;
        distance[1] /= magnitude;
        distance[2] /= magnitude;
        return distance;
    }

    public double getMagnitude(double[] distance) {
        double magnitude = Math.sqrt( distance[0] * distance[0] + distance[1] * distance[1] + distance[2] * distance[2] );
        return magnitude;
    }
    
    private double[] getLineDelta( double[] startCoords, double[] endCoords ) {
        double[] delta = new double[ startCoords.length ];
        for ( int i = 0; i < startCoords.length; i++ ) {
            delta[ i ] = startCoords[ i ] - endCoords[ i ];
        }
        return delta;
    }
    
//    private double deltaSlashLen( double lineDelta, double lineDelta1, double lineDelta2 ) {
//        return lineDelta / Math.sqrt( lineDelta1 * lineDelta1 + lineDelta2 * lineDelta2 );
//    }
    
    protected double[][] producePolygon(Matrix transform) {
        //dumpTransform(transform);
        // Clone the prototype.
        double[][] polygon = new double[endPolygonSides][];
        for (int i = 0; i < endPolygonSides; i++) {
            polygon[i] = Arrays.copyOf(prototypeEndPolygon[i], 3);
        }

        for (int i = 0; i < polygon.length; i++) {
            polygon[i] = matrixUtils.transform(transform, polygon[i]);
        }
        return polygon;
    }

    private double[][] createPrototypeEndPolygon() {
        double[][] prototypeEndPolygon = new double[endPolygonSides][];
        prototypeEndPolygon[0] = new double[]{-endPolygonRadius, 0f, 0f};
        double fullcircle = Math.PI * 2.0;
        double thetaIncrement = fullcircle / endPolygonSides;
        double theta = Math.PI;  // Position of first polygon point.
        for (int i = 1; i < endPolygonSides; i++) {
            theta += thetaIncrement;
            theta = theta % fullcircle;
            float x = (float) (Math.cos(theta) * endPolygonRadius);
            float y = (float) (Math.sin(theta) * endPolygonRadius);
            //System.out.println(String.format(
            //   "Theta=%f, x=%f, y=%f, cos=%f, sin=%f.  Iteration=%d\n", 
            //   theta, x, y, Math.cos(theta), Math.sin(theta), i));
            prototypeEndPolygon[i] = new double[]{x, y, 0f};
        }

        return prototypeEndPolygon;
    }

    @SuppressWarnings("unused")
    protected void dumpTransform(Matrix transform) {
        System.out.println("----TRANSFORM----");
        for (int i = 0; i < transform.getRowDimension(); i++) {
            for (int j = 0; j < transform.getColumnDimension(); j++) {
                System.out.print(" " + transform.get(i, j));
            }
            System.out.println();
        }
    }

}
