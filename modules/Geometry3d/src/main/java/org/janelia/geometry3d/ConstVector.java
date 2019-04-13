package org.janelia.geometry3d;

/**
 *
 * @author Christopher Bruns
 */
public interface ConstVector {

    float distance(ConstVector rhs);

    float distanceSquared(ConstVector rhs);

    float dot(ConstVector rhs);

    float get(int i);

    int size();
    
    float[] toNewArray();

}
