/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.List;

/**
 * Helps setup prototypes (copy-able zero-based instances) of enclosures about
 * points.
 *
 * @author fosterl
 */
public class PointPrototypeHelper {
    private final List<Matrix> prototypePoints;

    public PointPrototypeHelper(int numSides, double radius) {
        prototypePoints = new ArrayList<>();
        setup(numSides, radius);
    }
    
    public List<Matrix> getPrototypePoints() {
        return prototypePoints;
    }
    
    private void setup(int numSides, double radius) {
        // Make a lot of polygons: as many as requested number of sides.
        double hypot = radius;
        final int ringCount = (int) (double) (numSides / 2.0);
        double angleOffset = Math.PI / 20.0;
        double angularIteration
                = ((Math.PI / 2.0) - 2 * angleOffset) / ringCount;
        double angle = angleOffset;
        // Growing forward, to midline.
        for (int i = 0; i < ringCount; i++) {
            double y = Math.sin(angle) * hypot;
            double z = -Math.cos(angle) * hypot;
            PolygonSource polygonSource = new PolygonSource(numSides, y);
            double[][] polygon = polygonSource.createZAxisAlignedPrototypeEndPolygon();
            Matrix transform = createPrototypeTransform(hypot, y, z);
            addResult(polygon, transform, prototypePoints);

            angle += angularIteration;
        }

        angle -= angularIteration; // Push back in other direction.

        // Growing beyond midline.        
        for (int i = 0; i < ringCount; i++) {
            double y = Math.sin(angle) * hypot;
            double z = Math.cos(angle) * hypot;
            PolygonSource polygonSource = new PolygonSource(numSides, y);
            double[][] polygon = polygonSource.createZAxisAlignedPrototypeEndPolygon();
            Matrix transform = createPrototypeTransform(hypot, y, z);
            addResult(polygon, transform, prototypePoints);
            angle -= angularIteration;
        }
    }

    private void addResult(double[][] polygon, Matrix transform, List<Matrix> points) {
        for (double[] polygonPoint : polygon) {
            Matrix pm = new Matrix(4, 1);
            for (int i = 0; i < polygonPoint.length; i++) {
                pm.set(i, 0, polygonPoint[i]);
            }
            pm.set(3, 0, 1.0); //Ensures transforms have something to work with.
            Matrix result = transform.times(pm);
            points.add(result);
        }
    }

    private Matrix createPrototypeTransform(double hypot, double y, double z) {
        Matrix transform = Matrix.identity(4, 4);
        transform.set(1, 3, hypot - y);
        transform.set(2, 3, z);
        return transform;
    }

    @SuppressWarnings("unused")
    private void dumpPointMatrix(Matrix point) {
        point.print(10, 4);
    }


}
