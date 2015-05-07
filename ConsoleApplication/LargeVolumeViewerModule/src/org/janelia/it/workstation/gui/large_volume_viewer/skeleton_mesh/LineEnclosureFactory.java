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
import org.janelia.it.workstation.geom.Rotation3d;
import org.janelia.it.workstation.geom.UnitVec3;
import org.janelia.it.workstation.geom.Vec3;
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
    
    private static final double[] PROTOTYPE_NORMAL = {0, 0, -1};
    private List<VertexInfoBean> vertices = new ArrayList<>();
    private List<Triangle> triangles = new ArrayList<>();
    private ViewMatrixSupport matrixUtils = new ViewMatrixSupport();
    
    private int endPolygonSides = -1;
    private double endPolygonRadius; 
    
    private double[][] prototypeEndPolygon;
    
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
        
        //double[] deltasOverLength = computeDeltasOverLength( startingCoords, endingCoords );
        List<double[][]> endCaps = makeEndPolygons( startingCoords, endingCoords );
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

    private double[][] createPrototypeEndPolygon() {
        double[][] prototypeEndPolygon = new double[ endPolygonSides ][];
        prototypeEndPolygon[0] = new double[] { -endPolygonRadius, 0f, 0f };
        double fullcircle = Math.PI * 2.0;
        double thetaIncrement = fullcircle / endPolygonSides;
        double theta = Math.PI;  // Position of first polygon point.
        for ( int i = 1; i < endPolygonSides; i++ ) {
            theta += thetaIncrement;
            theta = theta % fullcircle;
            float x = (float)(Math.cos(theta) * endPolygonRadius);
            float y = (float)(Math.sin(theta) * endPolygonRadius);
            //System.out.println(String.format(
            //   "Theta=%f, x=%f, y=%f, cos=%f, sin=%f.  Iteration=%d\n", 
            //   theta, x, y, Math.cos(theta), Math.sin(theta), i));
            prototypeEndPolygon[i] = new double[] { x, y, 0f };
        }
        
        return prototypeEndPolygon;
    }
    
    private double[] computeDeltasOverLength( double[] startCoords, double[] endCoords ) {
        double[] rtnVal = new double[ 3 ];
        double[] lineDelta = getLineDelta(startCoords, endCoords);
        
        double accum = 0.0;
        for (int i = 0; i < 3; i++) {
            accum += lineDelta[i] * lineDelta[i];
        }
        double lineLen = Math.sqrt(accum);

        rtnVal[ X ] = lineDelta[0] / lineLen;
        rtnVal[ Y ] = lineDelta[1] / lineLen;
        rtnVal[ Z ] = lineDelta[2] / lineLen;
        
        return rtnVal;
    }
    
    /**
     * This is probably a dead end.  Experimental. Never tested.
     * @param coords
     * @param endPoint
     * @return 
     */
    private double[][] makePolygonAroundPoint( double[] coords, double[] endPoint ) {
        
        // Get the three angles: about X, about Y, about Z.
        double[][] polygon = new double[endPolygonSides][3];
        // Clone the prototype.
        for (int i = 0; i < endPolygonSides; i++) {
            System.arraycopy(prototypeEndPolygon[i], 0, polygon[i], 0, 3);
        }
        double[] deltas = normalize(getLineDelta(coords, endPoint));

        Rotation3d rotation = new Rotation3d();
        rotation.add(new UnitVec3(deltas[X], deltas[Y], deltas[Z]));
        Vec3 f = new Vec3(PROTOTYPE_NORMAL[X], PROTOTYPE_NORMAL[Y], PROTOTYPE_NORMAL[Z]);
        Vec3 u = rotation.times(new Vec3(0,-1,0));
        Vec3 c = f.plus(rotation.times(new Vec3(1,1,1)));
        // Eye, Center, Up
        float[] lookAt = matrixUtils.getLookAt(c, f, u);
        double aboutX = 0;
        double aboutY = 0;
        double aboutZ = 0;
        Matrix transform = matrixUtils.getTransform3D(aboutX, aboutY, aboutZ, coords[X], coords[Y], coords[Z]);
        for (int i = 0; i < polygon.length; i++) {
            polygon[i] = matrixUtils.transform(transform, polygon[i]);
        }
        return polygon;
    }
    
    private List<double[][]> endCapPolygonsHolder = new ArrayList<>();
    private List<double[][]> makeEndPolygons( double[] startCoords, double[] endCoords ) {
        
        endCapPolygonsHolder.clear();
        
        // Get the three angles: about X, about Y, about Z.
        double[] lineUnitVector = normalize(getLineDelta(startCoords, endCoords));
        //double[] crossProtoNormal = getCrossProduct( lineUnitVector, PROTOTYPE_NORMAL );
        //double crossMag = getMagnitude( crossProtoNormal );
        //double theta = Math.asin(crossMag);

        //double[] deltas = getLineDelta(lineUnitVector, PROTOTYPE_NORMAL);
        
        double aboutX = lineUnitVector[2] == 0 ? 0 : Math.atan(lineUnitVector[1] / lineUnitVector[2]);
        double aboutY = lineUnitVector[2] == 0 ? 0 : Math.atan(lineUnitVector[0] / lineUnitVector[2]);
        double aboutZ = lineUnitVector[0] == 0 ? 0 : Math.atan(lineUnitVector[1] / lineUnitVector[0]);
        
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

    protected double[][] producePolygon(Matrix transform) {
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
    
    private double[][] makeEndPolygonForPoint( double[] coords, double[] deltasOverLength ) {
        return makeEndPolygon(deltasOverLength[0], deltasOverLength[1], deltasOverLength[2], coords);        
    }

    protected double[][] makeEndPolygon(
            double deltaYslashLen, double deltaZslashLen, double deltaXslashLen, 
            double[] coords) {

        double[][] polygon = new double[endPolygonSides][3];
        // Clone the prototype.
        for (int i = 0; i < endPolygonSides; i++) {
            System.arraycopy(prototypeEndPolygon[i], 0, polygon[i], 0, 3);
        }

        // Establish the positioning matrix.
        Matrix transformMatrix = matrixUtils.getTransform3D(
                deltaYslashLen, deltaZslashLen, 
                deltaXslashLen, deltaZslashLen,
                deltaYslashLen, deltaXslashLen, 
                coords[X], coords[Y], coords[Z]);
        for ( int i = 0; i < polygon.length; i++ ) {
            polygon[i] = matrixUtils.transform( transformMatrix, polygon[i] );
        }
        return polygon;
    }
    
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
    
    /**
     * 
     * cx = aybz − azby
     * cy = azbx − axbz
     * cz = axby − aybx	
     * @return 
     */
    private double[] getCrossProduct( double[] a, double[] b ) {
        double[] c = new double[ a.length ];
        
        c[ X ] = a[Y] * b[Z] - a[Z] * b[Z];
        c[ Y ] = a[Z] * b[X] - a[X] * b[X];
        c[ Z ] = a[X] * b[Y] - a[Y] * b[Y];
        
        return c;
    }
}
