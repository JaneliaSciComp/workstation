package org.janelia.it.FlyWorkstation.geom;

/**
 * This class encapsulates a 3D parametrized line; given two (x, y, z) points and corresponding
 * t values, it calculates the location of a point along the line at the input t parameter
 *
 * or to put it another way, given x(t1), y(t1), z(t1) and x(t2), y(t2), z(t2),
 * this class will give you  x(t), y(t), z(t) for any t
 *
 * if the two points or parameters are equal or nearly equal, we displace the second slightly
 * rather than throwing an exception
 *
 * another pair of methods let you convert between the arbitrary parameter t and
 * the path length along the line measured from t=0
 *
 * User: olbrisd
 * Date: 9/24/13
 * Time: 11:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class ParametrizedLine {

    private Vec3 point1;
    private Vec3 point2;
    private Double t1;
    private Double t2;

    public ParametrizedLine(Vec3 point1, Vec3 point2, Double t1, Double t2) {
        // checks: nulls?  use predetermined values
        if (t1 == null) {
            t1 = 0.0;
        }
        if (t2 == null) {
            t2 = 1.0;
        }
        if (point1 == null) {
            point1 = new Vec3(0.0, 0.0, 0.0);
        }
        if (point2 == null) {
            point2 = new Vec3(1.0, 0.0, 0.0);
        }

        // not clear what exactly which constant we should use here
        if (Math.abs(t1 - t2) < Double.MIN_NORMAL) {
            t2 = t1 + 10 * Double.MIN_NORMAL;
        }

        // points can't be too close, either:
        if (point1.minus(point2).norm() < Double.MIN_NORMAL) {
            point2 = point1.plus(new Vec3(10 * Double.MIN_NORMAL, 0.0, 0.0));
        }

        this.point1 = point1;
        this.point2 = point2;
        this.t1 = t1;
        this.t2 = t2;
    }

    public ParametrizedLine(Vec3 point1, Vec3 point2) {
        this(point1, point2, 0.0, 1.0);
    }

    public ParametrizedLine() {
        // the parameterless constructor essentially gives you the x-axis
        this(new Vec3(0.0, 0.0, 0.0), new Vec3(1.0, 0.0, 0.0));
    }

    /**
     * returns the point at the given parameter
     *
     * @param t = arbitrary parameter
     * @return x, y, z point on line
     */
    public Vec3 getPoint(Double t) {
        // Java makes this formula kind of ugly; the underlying equation is:
        // p(t) = p1 + (p2 - p1) * (t - t1) / (t2 - t1)

        Vec3 point = point2.minus(point1);
        point.multEquals((t - t1) / (t2 - t1));
        return point.plus(point1);
    }

    /** given a parameter value, find the path length in pixels along
     * the line from t=0
     *
     * @param t = arbitrary parameter
     * @return path length measured from t=0
     */
    public Double pathLengthFromParameter(Double t) {
        // this is a little sketchy, but should work for our
        //  purposes:
        return point2.minus(point1).norm() / (t2 - t1) * t;
    }

    /** given a path length (in pixels) from t=0,
     * return the parameter value that gets you that location
     *
     * @param s = path length measured from t=0
     * @return parameter corresponding to input path length
     */
    public Double parameterFromPathLength(Double s) {
       return (t2 - t1) / point2.minus(point1).norm() * s;
    }

}

