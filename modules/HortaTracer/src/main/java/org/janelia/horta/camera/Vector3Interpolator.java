package org.janelia.horta.camera;

import org.janelia.horta.camera.PrimitiveInterpolator;
import org.janelia.horta.camera.InterpolatorKernel;
import org.janelia.geometry3d.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brunsc
 */
public class Vector3Interpolator implements Interpolator<Vector3> 
{
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PrimitiveInterpolator interpolator;

    public Vector3Interpolator(InterpolatorKernel kernel) {
        interpolator = new PrimitiveInterpolator(kernel);
    }

    @Override
    public Vector3 interpolate_equidistant(double ofTheWay, Vector3 p0, Vector3 p1, Vector3 p2, Vector3 p3) {
        float x = interpolator.interpolate_equidistant(ofTheWay, 
                p0.getX(), p1.getX(), p2.getX(), p3.getX());
        float y = interpolator.interpolate_equidistant(ofTheWay, 
                p0.getY(), p1.getY(), p2.getY(), p3.getY());
        float z = interpolator.interpolate_equidistant(ofTheWay, 
                p0.getZ(), p1.getZ(), p2.getZ(), p3.getZ());
        
        return new Vector3(x, y, z);
    }

    @Override
    public Vector3 interpolate(
            double ofTheWay, 
            Vector3 p0, Vector3 p1, Vector3 p2, Vector3 p3, 
            double t0, double t1, double t2, double t3) 
    {
        float x = interpolator.interpolate(ofTheWay, 
                p0.getX(), p1.getX(), p2.getX(), p3.getX(), 
                t0, t1, t2, t3);
        float y = interpolator.interpolate(ofTheWay, 
                p0.getY(), p1.getY(), p2.getY(), p3.getY(), 
                t0, t1, t2, t3);
        float z = interpolator.interpolate(ofTheWay, 
                p0.getZ(), p1.getZ(), p2.getZ(), p3.getZ(), 
                t0, t1, t2, t3);
        
        return new Vector3(x, y, z);
    }
    
}
