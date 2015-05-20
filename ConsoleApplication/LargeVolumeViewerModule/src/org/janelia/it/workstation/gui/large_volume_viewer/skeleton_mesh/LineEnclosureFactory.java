/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    private static final Logger logger = LoggerFactory.getLogger(LineEnclosureFactory.class);
	public static final double ZERO_TOLERANCE = 0.0001;
	private static final double RADIANS_90 = Math.PI / 2.0;
    private static final int X = 0, Y = 1, Z = 2;
	private static final int PERPENDICULAR_ALIGNMENT = 100;
    private static final int YZ = 0;
    private static final int ZX = 1;
    private static final int XY = 2;
    
    private static final double[] PROTOTYPE_NORMAL = {0, 0, -1};
    private List<VertexInfoBean> vertices = new ArrayList<>();
    private List<Triangle> triangles = new ArrayList<>();
    private ViewMatrixSupport matrixUtils = new ViewMatrixSupport();
    
    private int endPolygonSides = -1;
    private double endPolygonRadius; 
    
    private Map<Integer,double[][]> axisAlignedPrototypePolygons = new HashMap<>();
    private double[][] zAxisAlignedPrototypePolygon;

    private List<double[][]> endCapPolygonsHolder = new ArrayList<>();
    
    public LineEnclosureFactory(int endPolygonSides, double endPolygonRadius) {
        this.endPolygonSides = endPolygonSides;
        this.endPolygonRadius = endPolygonRadius;
        this.zAxisAlignedPrototypePolygon = createZAxisAlignedPrototypeEndPolygon();  
        axisAlignedPrototypePolygons.put(2, this.zAxisAlignedPrototypePolygon);
    }
    
    public int addEnclosure(double[] startingCoords, double[] endingCoords) {
        return addEnclosure(startingCoords, endingCoords, null);
    }
    
    /**
     * Makes a set of triangles (geometry) to make a "pipe" around
     * the pair of starting/ending coords.  May make overhangs for "elbow
     * convenience."
     * 
     * @param startingCoords one end.
     * @param endingCoords other end.
     * @param color applied to each vertex.
     * @return number of coordinates created here.
     */
    public int addEnclosure( double[] startingCoords, double[] endingCoords, float[] color ) {
        if ( startingCoords.length != 3 ) {
            throw new IllegalArgumentException("3-D starting coords only.");
        }
        if (endingCoords.length != 3) {
            throw new IllegalArgumentException("3-D ending coords only.");
        }
        
        List<double[][]> endCaps = makeEndPolygons(startingCoords, endingCoords);
        //List<double[][]> endCaps = makeEndPolygonsNoTrig( startingCoords, endingCoords );
        int coordCount = 0;
        List<VertexInfoBean> startVertices = addVertices(endCaps.get(0), color);
		coordCount += startVertices.size();
        List<VertexInfoBean> endVertices = addVertices(endCaps.get(1), color);
		coordCount += endVertices.size();
        createCCWTriangles(startVertices, endVertices);
        return coordCount;
    }

    //--------------------------------------------IMPLEMENT TriangleSource
    @Override
    public List<VertexInfoBean> getVertices() {
        logger.info("Returning {} vertices.", vertices.size());
        return vertices;
    }

    @Override
    public List<Triangle> getTriangleList() {
        return triangles;
    }

    //--------------------------------------------HELPERS
	/**
	 * Adds vertices to the overall collection, and to the bean collection
	 * that represents this polygon.
	 * 
	 * @param poly simple array of coord triples.
     * @param color dimension 3; applied to every vertex created from polygon.
	 * @return list of coord beans.
	 */
    protected List<VertexInfoBean> addVertices(double[][] poly, float[] color) {
		List<VertexInfoBean> polyBeans = new ArrayList<>();
        for (int i = 0; i < poly.length; i++) {
            VertexInfoBean bean = new VertexInfoBean();
            VertexInfoKey key = new VertexInfoKey();
            key.setPosition(poly[i]);
            bean.setKey(key);
            // Color is optional, depending on application/caller.
            if ( color != null ) {
                bean.setAttribute(
                        VertexInfoBean.KnownAttributes.color.name(), color, 3
                );
            }
            vertices.add(bean);
			polyBeans.add(bean);
            logger.debug("Adding vertex {},{},{}", key.getPosition()[X], key.getPosition()[Y], key.getPosition()[Z]);
        }
        return polyBeans;
    }
	
	private void createCCWTriangles(List<VertexInfoBean> startingVertices, List<VertexInfoBean> endingVertices) {			
		int vertsPerPoly = startingVertices.size();
		for ( int polygonVertex = 0; polygonVertex < vertsPerPoly; polygonVertex++ ) {
			// Add a triangle that demarks leading corner of rectangle, to
			// distant edge of rectangle, and back to starting corner.
			
			// 1xxxxxxxxx3
			// |    xxxxx|
			// |        x|
			// +---------2
			final int nextEdgeOffset = (polygonVertex + 1) % vertsPerPoly;
			Triangle triangle = new Triangle();

            final VertexInfoBean topLeft = startingVertices.get( polygonVertex );
			triangle.addVertex(topLeft);
            
            final VertexInfoBean bottomRight = endingVertices.get(polygonVertex);
            triangle.addVertex( bottomRight );

            final VertexInfoBean topRight = endingVertices.get( nextEdgeOffset);
			triangle.addVertex( topRight );
            
			triangles.add( triangle );
			
			// Add a triangle that demarks the leading edge of the rectangle,
			// to the farthest corner.
			
			// 4---------+
			// |x        |
			// |xxxxx    |
			// 5xxxxxxxxx6
			triangle = new Triangle();
			triangle.addVertex( topLeft );
            
            final VertexInfoBean bottomLeft = startingVertices.get( nextEdgeOffset );
			triangle.addVertex( bottomLeft );
            
			triangle.addVertex( bottomRight );
            
			triangles.add( triangle );
			
		}
		
        // Now treating the end caps.
		// Winding to point close end away from tube.
		for ( int i = 0; i < (vertsPerPoly - 2); i++ ) {
			final Triangle triangle = new Triangle();
			triangle.addVertex( startingVertices.get(0) );			
			triangle.addVertex( startingVertices.get( i + 2 ) );
			triangle.addVertex( startingVertices.get( i + 1 ) );
			triangles.add( triangle );
		}
			
		// Winding to point far end away from tube.
		for ( int i = 0; i < (vertsPerPoly - 2); i++ ) {
			final Triangle triangle = new Triangle();
			triangle.addVertex( endingVertices.get(0) );
			triangle.addVertex( endingVertices.get( i + 1 ) );
			triangle.addVertex( endingVertices.get( i + 2 ) );
			triangles.add( triangle );
		}
	}

    private List<double[][]> makeEndPolygons( double[] startCoords, double[] endCoords ) {
        
        endCapPolygonsHolder.clear();
		final double[] lineDelta = getLineDelta(startCoords, endCoords);
        
        // Get the three angles: about X, about Y, about Z.
        double[] lineUnitVector = normalize(lineDelta);
        
        double aboutX = lineUnitVector[Z] == 0 ? 0 : -Math.atan(lineUnitVector[Y] / lineUnitVector[Z]);
        double aboutY = lineUnitVector[Z] == 0 ? 0 : Math.atan(lineUnitVector[X] / lineUnitVector[Z]);
        double aboutZ = lineUnitVector[X] == 0 ? 0 : Math.atan(lineUnitVector[Y] / lineUnitVector[X]);
				
		logger.debug("Using angles: {}, {}, {}.", Math.toDegrees(aboutX), Math.toDegrees(aboutY), Math.toDegrees(aboutZ));

		int axialAlignment = getAxialAlignmentByLineDelta(lineDelta);
		
		if (axialAlignment == -1) {
			// Now that we have our angles, we make transforms.
			Matrix transform1 = matrixUtils.getTransform3D(
					aboutX, aboutY, 0,
					0, 0, 0);
			final double[][] startEndPolygon = clonePrototypePolygon(zAxisAlignedPrototypePolygon);
			transformPolygon(startEndPolygon, transform1);
			Matrix transform2 = matrixUtils.getTransform3D(
					0, 0, aboutZ,
					startCoords[X], startCoords[Y], startCoords[Z]);
			transformPolygon(startEndPolygon, transform2);
			endCapPolygonsHolder.add(startEndPolygon);

			final double[][] endingEndPolygon = clonePrototypePolygon(zAxisAlignedPrototypePolygon);
			transformPolygon(endingEndPolygon, transform1);
			transform2.set(0, 3, endCoords[X]);
			transform2.set(1, 3, endCoords[Y]);
			transform2.set(2, 3, endCoords[Z]);
			transformPolygon(endingEndPolygon, transform2);
			endCapPolygonsHolder.add(endingEndPolygon);			
		}
		else if (axialAlignment == PERPENDICULAR_ALIGNMENT) {
			// Special case: new normal lies in the xy plane, but not on an axis.
			// Only spin about Z.
			Matrix transform = matrixUtils.getTransform3D(
					0f, 0f, -aboutZ,
					startCoords[X], startCoords[Y], startCoords[Z]
			);
			double[][] prototypePolygon = getPrototypePolygon(Y);
			final double[][] startEndPolygon = clonePrototypePolygon(prototypePolygon);
			transformPolygon(startEndPolygon, transform);		
			endCapPolygonsHolder.add(startEndPolygon);
			
			final double[][] endingEndPolygon = clonePrototypePolygon(prototypePolygon);
			transform.set(0, 3, endCoords[X]);
			transform.set(1, 3, endCoords[Y]);
			transform.set(2, 3, endCoords[Z]);
			transformPolygon(endingEndPolygon, transform);
			endCapPolygonsHolder.add(endingEndPolygon);
		}
		else {
			// Special case: aligned right along some axis.  Trig assumptions won't help.
			Matrix transform = matrixUtils.getTransform3D(
					0f,
					0f,
					0f,
					startCoords[X], startCoords[Y], startCoords[Z]);
			double[][] prototypePolygon = getPrototypePolygon(axialAlignment);
			endCapPolygonsHolder.add(producePolygon(transform, prototypePolygon));
			transform.set(0, 3, endCoords[X]);
			transform.set(1, 3, endCoords[Y]);
			transform.set(2, 3, endCoords[Z]);
			endCapPolygonsHolder.add(producePolygon(transform, prototypePolygon));
		}
        
        return endCapPolygonsHolder;
    }
	
    private List<double[][]> makeEndPolygonsNoTrig(double[] startCoords, double[] endCoords) {

        endCapPolygonsHolder.clear();

        // Establish the positioning matrix.
        double[] lineDelta = getLineDelta(endCoords, startCoords);
        double[] planarProjections = getPlanarProjections( lineDelta );
		int axialAlignment = getAxialAlignment(planarProjections);
        
        Matrix transformMatrix;
        double[][] prototypePolygon;
        if (axialAlignment == -1) {
            transformMatrix = matrixUtils.getTransform3D(
                    lineDelta[Y] / planarProjections[YZ], lineDelta[Z] / planarProjections[YZ],
                    lineDelta[Z] / planarProjections[ZX], lineDelta[X] / planarProjections[ZX],
                    lineDelta[Y] / planarProjections[XY], lineDelta[X] / planarProjections[XY],
                    startCoords[X], startCoords[Y], startCoords[Z]);
            prototypePolygon = zAxisAlignedPrototypePolygon;
        }
        else {
            // Special case: aligned right along some axis.  Trig assumptions won't help.
            if (axisAlignedPrototypePolygons.get(axialAlignment) == null) {
                axisAlignedPrototypePolygons.put(axialAlignment, createAxisAlignedPrototypeEndPolygon(axialAlignment));
            }
            transformMatrix = matrixUtils.getTransform3D(
                    0f,
                    0f,
                    0f,
                    startCoords[X], startCoords[Y], startCoords[Z]);
            prototypePolygon = axisAlignedPrototypePolygons.get(axialAlignment);
        }
        
        endCapPolygonsHolder.add( producePolygon( transformMatrix, prototypePolygon ) );
        transformMatrix.set(0, 3, endCoords[X]);
        transformMatrix.set(1, 3, endCoords[Y]);
        transformMatrix.set(2, 3, endCoords[Z]);
        endCapPolygonsHolder.add( producePolygon( transformMatrix, prototypePolygon ) );
        return endCapPolygonsHolder;
    }

	private int getAxialAlignmentByLineDelta(double[] lineDelta) {
		double deltaX = Math.abs(lineDelta[X]);
		double deltaY = Math.abs(lineDelta[Y]);
		double deltaZ = Math.abs(lineDelta[Z]);
		if (deltaX < ZERO_TOLERANCE  &&  deltaY < ZERO_TOLERANCE) {
			return Z;
		}
		else if (deltaX < ZERO_TOLERANCE && deltaZ < ZERO_TOLERANCE) {
			return Y;
		}
		else if (deltaY < ZERO_TOLERANCE && deltaZ < ZERO_TOLERANCE) {
			return X;
		}
		else if (deltaZ < ZERO_TOLERANCE ) {
			return PERPENDICULAR_ALIGNMENT;
		}
		else {
			return -1;
		}
	}
	
	private int getAxialAlignment(double[] planarProjections) {
		int axialAlignment = -1;
		for (int i = 0; i < planarProjections.length; i++) {
			if (Math.abs(planarProjections[i]) < ZERO_TOLERANCE) {
				axialAlignment = i;
			}
		}
		return axialAlignment;
	}
    
	private double[][] getPrototypePolygon(int axialAlignment) {
		if (axisAlignedPrototypePolygons.get(axialAlignment) == null) {
			axisAlignedPrototypePolygons.put(axialAlignment, createAxisAlignedPrototypeEndPolygon(axialAlignment));
		}
		return axisAlignedPrototypePolygons.get(axialAlignment);
	}

    private double[] getPlanarProjections( double[] lineDelta ) {
        return new double[] {
            Math.sqrt(lineDelta[Y] * lineDelta[Y] + lineDelta[Z] * lineDelta[Z]),
            Math.sqrt(lineDelta[Z] * lineDelta[Z] + lineDelta[X] * lineDelta[X]),
            Math.sqrt(lineDelta[Y] * lineDelta[Y] + lineDelta[X] * lineDelta[X]),
        };
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
    
    protected double[][] clonePrototypePolygon(double[][] prototype) {
        //dumpTransform(transform);
        // Clone the prototype.
        double[][] polygon = new double[endPolygonSides][];
        for (int i = 0; i < endPolygonSides; i++) {
            polygon[i] = Arrays.copyOf(prototype[i], 3);
        }
        return polygon;
    }

    protected double[][] producePolygon(Matrix transform, double[][] prototype) {
        double[][] polygon = clonePrototypePolygon(prototype);
		transformPolygon(polygon, transform);
        return polygon;
    }

	private void transformPolygon(double[][] polygon, Matrix transform) {
		for (int i = 0; i < polygon.length; i++) {
			polygon[i] = matrixUtils.transform(transform, polygon[i]);
		}
	}

    private double[][] createAxisAlignedPrototypeEndPolygon(int axis) {
        double[][] prototypeEndPolygon = new double[endPolygonSides][];
        prototypeEndPolygon[0] = new double[]{-endPolygonRadius, 0f, 0f};
        double fullcircle = Math.PI * 2.0;
        double thetaIncrement = fullcircle / endPolygonSides;
        double theta = Math.PI;  // Position of first polygon point.
        for (int i = 1; i < endPolygonSides; i++) {
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
                prototypeEndPolygon[i][coord] = coord == axis ? 0: calculatedCoords[ calcNum++ ];
            }
        }

        return prototypeEndPolygon;
    }

    private double[][] createZAxisAlignedPrototypeEndPolygon() {
        return createAxisAlignedPrototypeEndPolygon(Z);
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
