
package org.janelia.geometry3d;

/**
 * Bounding Box
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class Box3 {
    public final Vector3 min;
    public final Vector3 max;
    
    public Box3() {
        // "empty" box
        min = new Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        max = new Vector3(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
    }
    
    public Box3(Vector3 min, Vector3 max) {
        this.min = min;
        this.max = max;
    }

    public Vector3 getCentroid() {
        return (new Vector3(min).add(max)).multiplyScalar(0.5f);
    }
    
    public void include(Vector3 v) {
        for (int i = 0; i < 3; ++i) {
            if (v.get(i) < min.get(i))
                min.set(i, v.get(i));
            if (v.get(i) > max.get(i))
                max.set(i, v.get(i));
        }
    }
    
    public void include(Box3 other) {
        include(other.min);
        include(other.max);
    }

    void clear() {
        min.set(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        max.set(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
    }
}
