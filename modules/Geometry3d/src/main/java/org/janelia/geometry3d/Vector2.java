package org.janelia.geometry3d;

/**
 *
 * @author Christopher Bruns
 */
public class Vector2 extends BasicVector {
    public Vector2(float x, float y) {
        super(2);
        data[0] = x;
        data[1] = y;
    }
    
}
