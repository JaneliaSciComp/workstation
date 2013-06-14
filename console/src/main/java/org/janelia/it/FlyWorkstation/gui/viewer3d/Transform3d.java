package org.janelia.it.FlyWorkstation.gui.viewer3d;

/**
 * This class represents the rotate-and-shift transform which gives the
 * location and orientation of a new frame F in a base (reference) frame
 * B. A frame is an orthogonal, right-handed set of three axes, and an
 * origin point. A transform X from frame B to F consists of 3 perpendicular
 * unit vectors defining F's axes as viewed from B (that is, as expressed in
 * the basis formed by B's axes), and a vector from B's origin point OB to F's
 * origin point OF. Note that the meaning of "B" comes from the context in
 * which the transform is used. We use the phrase "frame F is in frame B" to
 * describe the above relationship, that is, "in" means both measured from
 * and expressed in.
 *
 * The axis vectors constitute a Rotation. They are ordered 1-2-3 or x-y-z
 * as you prefer, with z = x X y, making a right-handed set. These axes are
 * arranged as columns of a 3x3 rotation matrix R_BF = [ x y z ] which is a
 * direction cosine (rotation) matrix useful for conversions between frame
 * B and F. (The columns of R_BF are F's coordinate axes, expressed in B.) For
 * example, given a vector vF expressed in the F frame, that same vector
 * re-expressed in B is given by vB = R_BF*vF. F's origin point OF is
 * stored as the translation vector p_BF=(OF-OB) and expressed in B.
 *
 * Transform is designed to behave as much as possible like the computer
 * graphics 4x4 transform X which would be arranged like this:
 * <pre>
 *
 *         [       |   ]
 *     X = [   R   | p ]    R is a 3x3 orthogonal rotation matrix
 *         [.......|...]    p os a 3x1 translation vector
 *         [ 0 0 0   1 ]
 * </pre>
 *
 * These can be composed directly by matrix multiplication, but more
 * importantly they have a particularly simple inverse:
 * <pre>
 *
 *    -1   [       |    ]
 *   X   = [  ~R   | p* ]   ~R is R transpose, p* = ~R(-p).
 *         [.......|....]
 *         [ 0 0 0   1  ]
 * </pre>
 *
 */
public class Transform3d {
    private Rotation rotation = new Rotation();
    private Vec3 translation = new Vec3();

    public Transform3d() {
        rotation = new Rotation();
        translation = new Vec3();
    }
    
    public Transform3d(Rotation rotation, Vec3 translation) {
        this.rotation = rotation;
        this.translation = translation;
    }
    
    public Transform3d times(Transform3d rhs) {
        return new Transform3d(
                rotation.times(rhs.rotation),
                this.times(rhs.translation));
    }
    
    public Vec3 times(Vec3 rhs) {
        return translation.plus(rotation.times(rhs));
    }
}
