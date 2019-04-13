package org.janelia.geometry3d;

/**
 *
 * @author Christopher Bruns
 * 
 * Const interface for Vector3
 * 
 * Vector3 is based on Three.js implementation, which relies
 * on in-place modifications, for better memory allocation efficiency.
 * But this can be confusing for those accustomed to immutable types. So
 * this ConstVector3 interface is available for greater data safety.
 */
public interface ConstVector3 
extends ConstVector
{

    /**
     * Creates a Matrix4, representing this Vector3 as a translation.
     * @return
     */
    Matrix4 asTransform();

    ConstVector3 cross(ConstVector3 rhs);

    float getX();

    float getY();

    float getZ();

    float length();

    float lengthSquared();
    
    ConstVector3 minus(ConstVector3 rhs);
    ConstVector3 plus(ConstVector3 rhs);
    
}
