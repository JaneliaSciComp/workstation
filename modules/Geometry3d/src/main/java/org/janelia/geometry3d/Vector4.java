package org.janelia.geometry3d;

/**
 *
 * @author Christopher Bruns
 */
public class Vector4 extends BasicVector {
    public Vector4(float x, float y, float z, float w) {
        super(4);
        data[0] = x;
        data[1] = y;
        data[2] = z;
        data[3] = w;
    }
}
