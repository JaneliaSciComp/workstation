/* 
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.geometry3d;

import java.util.Arrays;
import org.janelia.geometry3d.camera.ConstRotation;

/**
 * Represents a 3x3 rotation matrix of floats.
 * Many of these methods are adapted from Paul Mitiguy's C++ Rotation 
 * library in the SimTK toolkit http://simtk.org/
 * @author brunsc
 */
public class Rotation implements ConstRotation
{
    private final float[] data;
    
    private static final float EPSILON = 1e-7f;

    public float[] convertRotationToAngleAxis() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public float getMatrixElement(int i) {
        return data[i];
    }

    /**
     * Whether a particular rotation sequence is body-fixed or space-fixed
     */
    static public enum BodyOrSpaceType {
        BodyRotationSequence, SpaceRotationSequence
    }
    
    /**
     * Constructs a new identity Rotation (zero angle)
     */
    public Rotation() {
        data = new float[] {
            1, 0, 0,
            0, 1, 0,
            0, 0, 1};
    }

    /**
     * Constructs a new Rotation with the same values as another rotation
     * (copy constructor)
     * @param rhs 
     */
    public Rotation(Rotation rhs) {
        data = rhs.data.clone();
    }

    /**
     *
     * @return a reference to the internal float array
     */
    public float[] asArray() {
        return data;
    }

    /**
     * Constructs a new Transform with a copy of this Rotation as its
     * rotation component.
     * @return 
     */
    public Matrix4 asTransform() {
        return new Matrix4(
                data[0], data[1], data[2], 0,
                data[3], data[4], data[5], 0,
                data[6], data[7], data[8], 0,
                0, 0, 0, 1);
    }

    /** 
     * Calculate angle for ANY X or Y or Z rotation sequence
     * Adapted from SimTK
     * @param axis1 The axis this Rotation is somehow known to rotate about
     * @return The angle of rotation, in radians
     */
    public float convertOneAxisRotationToOneAngle( CoordinateAxis axis1 )
    {
        // Get proper indices into Rotation matrix
        CoordinateAxis axis2 = axis1.getNextAxis();
        CoordinateAxis axis3 = axis2.getNextAxis();
        int j = axis2.ordinal();
        int k = axis3.ordinal();

        Rotation R = this;
        float sinTheta = ( R.get(k, j) - R.get(j, k) ) / 2.0f;
        float cosTheta = ( R.get(j, j) + R.get(k, k) ) / 2.0f;

        return (float)Math.atan2( sinTheta, cosTheta );   
    }
    
    /**
     * Converts rotation matrix to axis-angle form.
     * @return a four element float array [x, y, z, angle in radians]
     */
    public float[] convertRotationToAxisAngle() {
        return convertRotationToQuaternion().convertQuaternionToAxisAngle();
    }

    /**
     * Converts rotation matrix to a quaternion.
     * @return a Quaternion that represents the same Rotation
     */
    public Quaternion convertRotationToQuaternion() {
        Rotation R = this;

        // Stores the return values [cos(theta/2), lambda1*sin(theta/2), lambda2*sin(theta/2), lambda3*sin(theta/2)]
        float[] q = new float[] {0, 0, 0, 0};

        // Check if the trace is larger than any diagonal
        float tr = R.trace();
        if( tr >= R.get(0,0)  &&  tr >= R.get(1,1)  &&  tr >= R.get(2,2) ) {
            q[0] = 1 + tr;
            q[1] = R.get(2,1) - R.get(1,2);
            q[2] = R.get(0,2) - R.get(2,0);
            q[3] = R.get(1,0) - R.get(0,1);

        // Check if R.get(0,0) is largest along the diagonal
        } else if( R.get(0,0) >= R.get(1,1)  &&  R.get(0,0) >= R.get(2,2)  ) {
            q[0] = R.get(2,1) - R.get(1,2);
            q[1] = 1 - (tr - 2*R.get(0,0));
            q[2] = R.get(0,1)+R.get(1,0);
            q[3] = R.get(0,2)+R.get(2,0);

        // Check if R.get(1,1) is largest along the diagonal
        } else if( R.get(1,1) >= R.get(2,2) ) {
            q[0] = R.get(0,2) - R.get(2,0);
            q[1] = R.get(0,1) + R.get(1,0);
            q[2] = 1 - (tr - 2*R.get(1,1));
            q[3] = R.get(1,2) + R.get(2,1);

        // R.get(2,2) is largest along the diagonal
        } else {
            q[0] = R.get(1,0) - R.get(0,1);
            q[1] = R.get(0,2) + R.get(2,0);
            q[2] = R.get(1,2) + R.get(2,1);
            q[3] = 1 - (tr - 2*R.get(2,2));
        }
        // float scale = q.norm();
        float scale = 0;
        for (int i = 0; i < 4; ++i)
            scale += q[i]*q[i];
        scale = (float)Math.sqrt(scale);
        if( q[0] < 0 )  scale = -scale;   // canonicalize
        // Change element order from SimTK convention to Three.js convention
        return new Quaternion().set(
                q[1]/scale, q[2]/scale, q[3]/scale, q[0]/scale
        ); // prevent re-normalization
    }
    
    public float[] convertThreeAxesBodyFixedRotationToThreeAngles(
            CoordinateAxis axis1, CoordinateAxis axis2, CoordinateAxis axis3) 
    {
        // Ensure this method has proper arguments
        assert( axis1.areAllDifferentAxes(axis2,axis3) );

        int i = axis1.ordinal();
        int j = axis2.ordinal();
        int k = axis3.ordinal();

        // Need to know if using a forward or reverse cyclical
        float plusMinus = 1.0f,  minusPlus = -1.0f;
        if( axis1.isReverseCyclical(axis2) ) { 
            plusMinus = -1.0f;  minusPlus = 1.0f; 
        }

        // Shortcut to the elements of the rotation matrix
        Rotation R = this;

        // Calculate theta2 using lots of information in the rotation matrix
        float Rsum   =  (float)Math.sqrt((square(R.get(i, i)) + square(R.get(i, j)) + square(R.get(j, k)) + square(R.get(k, k))) / 2);
        float theta2 =  (float)Math.atan2( plusMinus*R.get(i, k), Rsum ); // Rsum = abs(cos(theta2)) is inherently positive
        float theta1, theta3;

        // There is a "singularity" when cos(theta2) == 0
        if( Rsum > 4*EPSILON ) {
            theta1 =  (float)Math.atan2( minusPlus*R.get(j, k), R.get(k, k) );
            theta3 =  (float)Math.atan2( minusPlus*R.get(i, j), R.get(i, i) );
        }
        else if( plusMinus*R.get(i, k) > 0 ) {
            float spos = R.get(j, i) + plusMinus*R.get(k, j);  // 2*sin(theta1 + plusMinus*theta3)
            float cpos = R.get(j, j) + minusPlus*R.get(k, i);  // 2*cos(theta1 + plusMinus*theta3)
            float theta1PlusMinusTheta3 = (float)Math.atan2( spos, cpos );
            theta1 = theta1PlusMinusTheta3;  // Arbitrary split
            theta3 = 0;                      // Arbitrary split
        }
        else {
            float sneg = plusMinus*(R.get(k, j) + minusPlus*R.get(j, i));  // 2*sin(theta1 + minusPlus*theta3)
            float cneg = R.get(j, j) + plusMinus*R.get(k, i);              // 2*cos(theta1 + minusPlus*theta3)
            float theta1MinusPlusTheta3 = (float)Math.atan2( sneg, cneg );
            theta1 = theta1MinusPlusTheta3;  // Arbitrary split
            theta3 = 0;                      // Arbitrary split
        }

        // Return values have the following ranges:
        // -pi   <=  theta1  <=  +pi
        // -pi/2 <=  theta2  <=  +pi/2   (Rsum is inherently positive)
        // -pi   <=  theta3  <=  +pi
        return new float[]{ theta1, theta2, theta3 };

    }

    /**
     * Calculate angles for ANY three-angle ijk rotation sequence where (i,j,k = X,Y,Z).
     * Adapted from SimTK
     * 
     * @param bodyOrSpace Whether rotations are body-fixed or space-fixed
     * @param axis1 First body-fixed coordinate axis
     * @param axis2 Second body-fixed coordinate axis
     * @param axis3 Third body-fixed coordinate axis
     * @return Array of three angles, in radians
     */
    public float[] convertThreeAxesRotationToThreeAngles(
            BodyOrSpaceType bodyOrSpace,
            CoordinateAxis axis1, 
            CoordinateAxis axis2, 
            CoordinateAxis axis3) 
    {
        // If all axes are same, efficiently calculate with a one-axis, one-angle method
        if( axis1.areAllSameAxes(axis2,axis3) ) { 
            float theta = convertOneAxisRotationToOneAngle(axis1) / 3.0f;
            return new float[] {theta,theta,theta}; 
        }

        // If axis2 is same as axis1, efficiently calculate with a two-angle, two-axis rotation
        if( axis2.isSameAxis(axis1) ) {
            float[] xz = convertTwoAxesRotationToTwoAngles(bodyOrSpace,axis1,axis3);
            float theta = xz[0] / 2.0f;
            return new float[]{ theta, theta, xz[1] };
        }

        // If axis2 is same as axis3, efficiently calculate with a two-angle, two-axis rotation
        if( axis2.isSameAxis(axis3) ) {
            float[] xz = convertTwoAxesRotationToTwoAngles(bodyOrSpace,axis1,axis3);
            float theta = xz[1] / 2.0f;
            return new float[]{ xz[0], theta, theta };
        }

        // If using a SpaceRotationSequence, switch the order of the axes (later switch the angles)
        if( bodyOrSpace == BodyOrSpaceType.SpaceRotationSequence )  {
            // std::swap(axis1,axis3);
            // swap
            CoordinateAxis tmp = axis1;
            axis1 = axis3;
            axis3 = tmp;

        }

        // All calculations are based on a body-fixed rotation sequence.
        // Determine whether this is a BodyXYX or BodyXYZ type of rotation sequence.
        float[] ans = axis1.isSameAxis(axis3) 
                    ? convertTwoAxesBodyFixedRotationToThreeAngles(axis1, axis2)
                    : convertThreeAxesBodyFixedRotationToThreeAngles(axis1, axis2, axis3);

        // If using a SpaceRotationSequence, switch the angles now.
        if( bodyOrSpace == BodyOrSpaceType.SpaceRotationSequence ) {
            // std::swap(ans[0], ans[2]);
            ans = new float[] {ans[2], ans[1], ans[0]};
        }

        return ans;
    }

    /**
     * Calculate angles ONLY for a three-angle, two-axes, body-fixed, 
     * iji rotation sequence where i != j.
     * @param axis1
     * @param axis2
     * @return 
     */
    public float[] convertTwoAxesBodyFixedRotationToThreeAngles(
            CoordinateAxis axis1, CoordinateAxis axis2) 
    {
        // Ensure this method has proper arguments
        assert( axis1.isDifferentAxis( axis2 ) );

        CoordinateAxis axis3 = axis1.getThirdAxis( axis2 );
        int i = axis1.ordinal();
        int j = axis2.ordinal();
        int k = axis3.ordinal();

        // Need to know if using a forward or reverse cyclical
        float plusMinus = 1.0f,  minusPlus = -1.0f;
        if( axis1.isReverseCyclical(axis2) ) { 
            plusMinus = -1.0f;  minusPlus = 1.0f; 
        }

        // Shortcut to the elements of the rotation matrix
        Rotation R = this;

        // Calculate theta2 using lots of information in the rotation matrix
        float Rsum   = (float)Math.sqrt( (square(R.get(i, j)) + square(R.get(i, k)) + square(R.get(j, i)) + square(R.get(k, i))) / 2.0f );  
        float theta2 = (float)Math.atan2( Rsum, R.get(i, i) );  // Rsum = abs(sin(theta2)) is inherently positive
        float theta1, theta3;

        // There is a "singularity" when sin(theta2) == 0
        if( Rsum > 4*EPSILON ) {
            theta1  =  (float)Math.atan2( R.get(j, i), minusPlus*R.get(k, i) );
            theta3  =  (float)Math.atan2( R.get(i, j), plusMinus*R.get(i, k) );
        }
        else if( R.get(i, i) > 0 ) {
            float spos = plusMinus*R.get(k, j) + minusPlus*R.get(j, k);  // 2*sin(theta1 + theta3)
            float cpos = R.get(j, j) + R.get(k, k);                      // 2*cos(theta1 + theta3)
            float theta1PlusTheta3 = (float)Math.atan2( spos, cpos );
            theta1 = theta1PlusTheta3;  // Arbitrary split
            theta3 = 0;                 // Arbitrary split
        }
        else {
            float sneg = plusMinus*R.get(k, j) + plusMinus*R.get(j, k);  // 2*sin(theta1 - theta3)
            float cneg = R.get(j, j) - R.get(k, k);                      // 2*cos(theta1 - theta3)
            float theta1MinusTheta3 = (float)Math.atan2( sneg, cneg );
            theta1 = theta1MinusTheta3;  // Arbitrary split
            theta3 = 0;                  // Arbitrary split
        }

        // Return values have the following ranges:
        // -pi   <=  theta1  <=  +pi
        //   0   <=  theta2  <=  +pi    (Rsum is inherently positive)
        // -pi   <=  theta3  <=  +pi
        return new float[]{ theta1, theta2, theta3 };
    }

    /**
     * Calculate angles ONLY for a two-angle, two-axes, body-fixed, ij rotation 
     * sequence where i != j.
     * @param axis1 First rotation axis
     * @param axis2 Second rotation axis
     * @return Array of two sequential rotation angles in radians
     */
    public float[] convertTwoAxesBodyFixedRotationToTwoAngles(
            CoordinateAxis axis1, CoordinateAxis axis2)
    {
        // Ensure this method has proper arguments
        assert( axis1.isDifferentAxis( axis2 ) );

        CoordinateAxis axis3 = axis1.getThirdAxis(axis2);
        int i = axis1.ordinal();
        int j = axis2.ordinal();
        int k = axis3.ordinal();
        Rotation R = this;

        // Can use either direct method (fast) or all matrix elements with the 
        // overhead of two additional square roots (possibly more accurate).
        float sinTheta1Direct = R.get(k, j);
        float signSinTheta1 = sinTheta1Direct > 0 ? 1.0f : -1.0f;
        float sinTheta1Alternate = signSinTheta1 * (float)Math.sqrt( square(R.get(j, i)) + square(R.get(j, k)) );
        float sinTheta1 = ( sinTheta1Direct + sinTheta1Alternate ) / 2;

        float cosTheta1Direct = R.get(j, j);
        float signCosTheta1 = cosTheta1Direct > 0 ? 1.0f : -1.0f;
        float cosTheta1Alternate = signCosTheta1 * (float)Math.sqrt( square(R.get(k, i)) + square(R.get(k, k)) );
        float cosTheta1 = ( cosTheta1Direct + cosTheta1Alternate ) / 2;

        float theta1 = (float)Math.atan2( sinTheta1, cosTheta1 );

        // Repeat for theta2
        float sinTheta2Direct = R.get(i , k);
        float signSinTheta2 = sinTheta2Direct > 0 ? 1.0f : -1.0f;
        float sinTheta2Alternate = signSinTheta2 * (float)Math.sqrt( square(R.get(j, i)) + square(R.get(k, i)) );
        float sinTheta2 = ( sinTheta2Direct + sinTheta2Alternate ) / 2;

        float cosTheta2Direct = R.get(i , i);
        float signCosTheta2 = cosTheta2Direct > 0 ? 1.0f : -1.0f;
        float cosTheta2Alternate = signCosTheta2 * (float)Math.sqrt( square(R.get(j, k)) + square(R.get(k, k)) );
        float cosTheta2 = ( cosTheta2Direct + cosTheta2Alternate ) / 2.0f;

        float theta2 = (float)Math.atan2( sinTheta2, cosTheta2 );

        // If using a reverse cyclical, negate the signs of the angles
        if( axis1.isReverseCyclical(axis2) )  { 
            theta1 = -theta1;  
            theta2 = -theta2; 
        }

        // Return values have the following ranges:
        // -pi   <=  theta1  <=  +pi
        // -pi   <=  theta2  <=  +pi
        return new float[]{ theta1, theta2 };
        
    }
    
    /**
     * Calculate angles for ANY two-angle ij rotation sequence (i,j = X,Y,Z)
     * @param bodyOrSpace Whether rotations are body-fixed or space-fixed
     * @param axis1 First rotation axis
     * @param axis2 Second rotation axis
     * @return Array of two sequential rotation angles, in radians
     */
    public float[] convertTwoAxesRotationToTwoAngles(
            BodyOrSpaceType bodyOrSpace, 
            CoordinateAxis axis1, 
            CoordinateAxis axis2)
    {
        // If axis2 is same as axis1, efficiently calculate with a one-axis, one-angle method
        if( axis1.isSameAxis(axis2) ) 
        {   
            float theta = convertOneAxisRotationToOneAngle(axis1) / 2.0f; 
            return new float[]{ theta, theta }; 
        }

        // If using a SpaceRotationSequence, switch the order of the axes (later switch the angles)
        if( bodyOrSpace == BodyOrSpaceType.SpaceRotationSequence )  {
            // swap
            CoordinateAxis tmp = axis1;
            axis1 = axis2;
            axis2 = tmp;
        }

        // All calculations are based on a body-fixed rotation sequence
        float[] ans = convertTwoAxesBodyFixedRotationToTwoAngles( axis1, axis2 );

        // If using a SpaceRotationSequence, switch the angles now.
        if( bodyOrSpace == BodyOrSpaceType.SpaceRotationSequence ) {
            ans = new float[] { ans[1], ans[0] }; // swap
        }
        
        return ans;
        
    }
    
    public float get(int i, int j) {
        return data[3*i + j];
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Rotation other = (Rotation) obj;
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Arrays.hashCode(this.data);
        return hash;
    }

    public Rotation multiply(Rotation rhs) {
        float[] lhs = data.clone(); // copy before in-place replacement
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                data[3*i+j] = 0;
                for (int k = 0; k < 3; ++k) {
                    data[3*i+j] += lhs[3*i+k] * rhs.data[3*k+j];
                }
            }
        }
        return this;
    }
    
    public Vector3 multiply(Vector3 rhs) {
        Vector3 result = new Vector3(0,0,0);
        for (int i = 0; i < 3; ++i) {
            float r = 0;
            for (int j = 0; j < 3; ++j) {
                r += rhs.get(j) * data[3*i + j];
            }
            result.set(i, r);
        }
        return result;
    }
    
    protected void set(int i, int j, float value) {
        data[3*i +j] = value;
    }
    
    public void copy(ConstRotation other) {
        for (int i = 0; i < 9; ++i) {
            data[i] = other.getMatrixElement(i);
        }
    }

    public final void set(
            float i0, 
            float i1, 
            float i2, 
            float i3, 
            float i4, 
            float i5, 
            float i6, 
            float i7,
            float i8) 
    {
        data[0] = i0;
        data[1] = i1;
        data[2] = i2;
        data[3] = i3;
        data[4] = i4;
        data[5] = i5;
        data[6] = i6;
        data[7] = i7;
        data[8] = i8;
    }

    public Rotation setFromAxisAngle(ConstVector3 axis, float theta) {
        Quaternion q;
        q = new Quaternion().setFromAxisAngle(axis, theta);
        Rotation result = setFromQuaternion(q);
        if (Float.isNaN(result.get(0, 0)))
            System.err.println("Rotation.setFromAxisAngle: " + result + ", " + axis + ", " + theta);
        return result;
    }
    
    public Rotation setFromQuaternion(Quaternion quaternion) {
        // This method lifted from SimTK
        // Rearrange indices to match SimTK convention
        float[] qXyzw = quaternion.asArray();
        float[] q = {qXyzw[3], qXyzw[0], qXyzw[1], qXyzw[2]};
        float q00=q[0]*q[0], q11=q[1]*q[1], q22=q[2]*q[2], q33=q[3]*q[3];
        float q01=q[0]*q[1], q02=q[0]*q[2], q03=q[0]*q[3];
        float q12=q[1]*q[2], q13=q[1]*q[3], q23=q[2]*q[3];
        // 
        set(q00+q11-q22-q33,    2*(q12-q03)  ,   2*(q13+q02),
            2*(q12+q03)  ,  q00-q11+q22-q33,   2*(q23-q01),
            2*(q13-q02)  ,    2*(q23+q01)  , q00-q11-q22+q33);
        return this;
    }
    
    public Rotation setRotationFromAngleAboutAxis(float angle, CoordinateAxis axis) {
        return axis.isXAxis() 
                ? setRotationFromAngleAboutX(angle) : 
                (axis.isYAxis() ? setRotationFromAngleAboutY(angle) : 
                setRotationFromAngleAboutZ(angle) );
    }

    public Rotation setRotationFromAngleAboutX(float angle) {
        return setRotationFromAngleAboutX( (float)Math.cos(angle), (float)Math.sin(angle) );
    }

    public Rotation setRotationFromAngleAboutX(float cosAngle, float sinAngle) {
        Rotation R = this;
        R.set( 0, 0, 1 );   
        R.set( 0, 1, 0 );
        R.set( 0, 2, 0 );
        R.set( 1, 0, 0 );
        R.set( 2, 0, 0 );   
        R.set( 1, 1, cosAngle );
        R.set( 2, 2, cosAngle );  
        R.set( 1, 2, -sinAngle );
        R.set( 2, 1, sinAngle );  
        return this;
    }

    public Rotation setRotationFromAngleAboutY(float angle) {
        return setRotationFromAngleAboutY( (float)Math.cos(angle), (float)Math.sin(angle) );
    }

    public Rotation setRotationFromAngleAboutY(float cosAngle, float sinAngle) {
        Rotation R = this;
        R.set( 1, 1, 1 );   
        R.set( 0, 1, 0 );
        R.set( 1, 0, 0 );
        R.set( 1, 2, 0 );
        R.set( 2, 1, 0 );   
        R.set( 0, 0, cosAngle );
        R.set( 2, 2, cosAngle );  
        R.set( 2, 0, -sinAngle );
        R.set( 0, 2, sinAngle );  
        return this;
    }
    
    public Rotation setRotationFromAngleAboutZ(float angle) {
        return setRotationFromAngleAboutZ( (float)Math.cos(angle), (float)Math.sin(angle) );
    }
    
    public Rotation setRotationFromAngleAboutZ(float cosAngle, float sinAngle) {
        Rotation R = this;
        R.set( 2, 2, 1 );   
        R.set( 0, 2, 0 );
        R.set( 1, 2, 0 );
        R.set( 2, 0, 0 );
        R.set( 2, 1, 0 );   
        R.set( 0, 0, cosAngle );
        R.set( 1, 1, cosAngle );  
        R.set( 0, 1, -sinAngle );
        R.set( 1, 0, sinAngle );  
        return this;
    }


    /**
     * Set Rotation for ANY three-angle ijk rotation sequence where (i,j,k = X,Y,Z)
     * @param bodyOrSpace
     * @param angle1 Rotation angle in radians
     * @param axis1 
     * @param angle2  Rotation angle in radians
     * @param axis2 
     * @param angle3  Rotation angle in radians
     * @param axis3 
     * @return  reference to this Rotation
     */
    public Rotation setRotationFromThreeAnglesThreeAxes(
            BodyOrSpaceType bodyOrSpace, 
            float angle1, CoordinateAxis axis1, 
            float angle2, CoordinateAxis axis2, 
            float angle3, CoordinateAxis axis3) 
    {
        // If axis2 is same as axis1 or axis3, efficiently calculate with a two-angle,
        // two-axis rotation.
        if( axis2.isSameAxis(axis1) ) 
            return setRotationFromTwoAnglesTwoAxes(bodyOrSpace, angle1+angle2, axis1,  angle3,axis3);
        if( axis2.isSameAxis(axis3) ) 
            return setRotationFromTwoAnglesTwoAxes(bodyOrSpace, angle1,axis1,  angle2+angle3, axis3);

        // If using a SpaceRotationSequence, switch the order of the axes and the angles.
        if( bodyOrSpace == BodyOrSpaceType.SpaceRotationSequence )  {
            // std::swap(angle1,angle3);  std::swap(axis1,axis3); 
            float tmpAng = angle1; angle1 = angle3; angle3 = tmpAng;
            CoordinateAxis tmpAx = axis1; axis1 = axis3; axis3 = tmpAx;
        }

        // If using a reverse cyclical, negate the signs of the angles.
        if( axis1.isReverseCyclical(axis2) )  { 
            angle1 = -angle1;   angle2 = -angle2;   angle3 = -angle3; 
        }

        // Calculate the sines and cosines (some hardware can do this more 
        // efficiently as one Taylor series).
        float c1 = (float)Math.cos( angle1 ),  s1 = (float)Math.sin( angle1 );
        float c2 = (float)Math.cos( angle2 ),  s2 = (float)Math.sin( angle2 );
        float c3 = (float)Math.cos( angle3 ),  s3 = (float)Math.sin( angle3 );

        // All calculations are based on a body-fixed rotation sequence.
        // Determine whether this is a BodyXYX or BodyXYZ type of rotation sequence.
        if( axis1.isSameAxis(axis3) )  
            setThreeAngleTwoAxesBodyFixedForwardCyclicalRotation(
                    c1,s1,axis1, c2,s2,axis2, c3,s3);
        else                           
            setThreeAngleThreeAxesBodyFixedForwardCyclicalRotation(
                    c1,s1,axis1, c2,s2,axis2, c3,s3,axis3);

        return this;
    }

    /**
     * Set Rotation for ANY two-angle ij rotation sequence (i,j = X,Y,Z)
     * @param bodyOrSpace
     * @param angle1
     * @param axis1
     * @param angle2
     * @param axis2
     * @return 
     */
    public Rotation setRotationFromTwoAnglesTwoAxes(
            BodyOrSpaceType bodyOrSpace, 
            float angle1, CoordinateAxis axis1, 
            float angle2, CoordinateAxis axis2) 
    {
        // If axis2 is same as axis1, efficiently calculate with a one-angle, one-axis rotation
        if( axis1.isSameAxis(axis2) ) { 
            return setRotationFromAngleAboutAxis( angle1+angle2, axis1 ); }

        // If using a SpaceRotationSequence, switch the order of the axes and the angles
        if( bodyOrSpace == BodyOrSpaceType.SpaceRotationSequence )  {
            // std::swap(angle1,angle2);  std::swap(axis1,axis2);
            float tmpAng = angle1; angle1 = angle2; angle2 = tmpAng;
            CoordinateAxis tmpAx = axis1; axis1 = axis2; axis2 = tmpAx;
        }

        // If using a reverse cyclical, negate the signs of the angles
        if( axis1.isReverseCyclical(axis2) )  { 
            angle1 = -angle1;   angle2 = -angle2; 
        }

        // Calculate the sines and cosines (some hardware can do this more efficiently as one Taylor series)
        float c1 = (float)Math.cos( angle1 ),  s1 = (float)Math.sin( angle1 );
        float c2 = (float)Math.cos( angle2 ),  s2 = (float)Math.sin( angle2 );

        // All calculations are based on a body-fixed forward-cyclical rotation sequence
        return setTwoAngleTwoAxesBodyFixedForwardCyclicalRotation( 
                c1,s1,axis1,  c2,s2,axis2 );
    }

    /**
     * Set Rotation ONLY for three-angle, three-axes, body-fixed, ijk rotation 
     * sequence where i != j != k.
     * @param rotX Rotation angle in radians
     * @param rotY Rotation angle in radians
     * @param rotZ Rotation angle in radians
     * @return 
     */
    public Rotation setRotationToBodyFixedXYZ(float rotX, float rotY, float rotZ) {
        return setRotationFromThreeAnglesThreeAxes( 
                BodyOrSpaceType.BodyRotationSequence, 
                rotX, CoordinateAxis.X, 
                rotY, CoordinateAxis.Y, 
                rotZ, CoordinateAxis.Z );
    }
    
    private Rotation setThreeAngleThreeAxesBodyFixedForwardCyclicalRotation(
            float cosAngle1, float sinAngle1, CoordinateAxis axis1, 
            float cosAngle2, float sinAngle2, CoordinateAxis axis2, 
            float cosAngle3, float sinAngle3, CoordinateAxis axis3) 
    {
        // Ensure this method has proper arguments
        assert( axis1.areAllDifferentAxes(axis2,axis3) );

        // Repeated calculations (for efficiency)
        float s1c3 = sinAngle1 * cosAngle3;
        float s3c1 = sinAngle3 * cosAngle1;
        float s1s3 = sinAngle1 * sinAngle3;
        float c1c3 = cosAngle1 * cosAngle3;

        int i = axis1.ordinal();
        int j = axis2.ordinal();
        int k = axis3.ordinal();

        Rotation R = this;
        R.set( i, i,  cosAngle2 * cosAngle3 );
        R.set( i, j, -sinAngle3 * cosAngle2 );
        R.set( i, k,  sinAngle2 );
        R.set( j, i,  s3c1 + sinAngle2 * s1c3 );
        R.set( j, j,  c1c3 - sinAngle2 * s1s3 );
        R.set( j, k, -sinAngle1 * cosAngle2 );
        R.set( k, i,  s1s3 - sinAngle2 * c1c3 );
        R.set( k, j,  s1c3 + sinAngle2 * s3c1 );
        R.set( k, k,  cosAngle1 * cosAngle2 );
        
        return this;
    }


    /**
     * Set Rotation ONLY for three-angle, two-axes, body-fixed, iji rotation 
       sequence where i != j.
     * @param cosAngle1
     * @param sinAngle1
     * @param axis1
     * @param cosAngle2
     * @param sinAngle2
     * @param axis2
     * @param cosAngle3
     * @param sinAngle3
     * @return 
     */
    private Rotation setThreeAngleTwoAxesBodyFixedForwardCyclicalRotation(
            float cosAngle1, float sinAngle1, CoordinateAxis axis1, 
            float cosAngle2, float sinAngle2, CoordinateAxis axis2, 
            float cosAngle3, float sinAngle3) 
    {
        // Ensure this method has proper arguments
        assert( axis1.isDifferentAxis( axis2 ) );

        // Repeated calculations (for efficiency)
        float s1c3 = sinAngle1 * cosAngle3;
        float s3c1 = sinAngle3 * cosAngle1;
        float s1s3 = sinAngle1 * sinAngle3;
        float c1c3 = cosAngle1 * cosAngle3;

        CoordinateAxis axis3 = axis1.getThirdAxis( axis2 );
        int i = axis1.ordinal();
        int j = axis2.ordinal();
        int k = axis3.ordinal();

        Rotation R =  this;
        R.set( i, i,  cosAngle2);
        R.set( i, j,  sinAngle2 * sinAngle3);
        R.set( i, k,  sinAngle2 * cosAngle3);
        R.set( j, i,  sinAngle1 * sinAngle2);
        R.set( j, j,  c1c3 - cosAngle2 * s1s3);
        R.set( j, k, -s3c1 - cosAngle2 * s1c3);
        R.set( k, i, -sinAngle2 * cosAngle1);
        R.set( k, j,  s1c3 + cosAngle2 * s3c1);
        R.set( k, k, -s1s3 + cosAngle2 * c1c3);
        
        return this;
    }

    /**
     * 
     * @param rotX Rotation angle in radians
     * @param rotY Rotation angle in radians
     * @param rotZ Rotation angle in radians
     * @return 
     */
    public Rotation setToBodyFixed123(float rotX, float rotY, float rotZ) {
        return setRotationToBodyFixedXYZ(rotX, rotY, rotZ);
    }

    /**
     * Set Rotation ONLY for two-angle, two-axes, body-fixed, ij rotation sequence 
     * where i != j.
     * @param cosAngle1
     * @param sinAngle1
     * @param axis1
     * @param cosAngle2
     * @param sinAngle2
     * @param axis2
     * @return 
     */
    public Rotation setTwoAngleTwoAxesBodyFixedForwardCyclicalRotation(
            float cosAngle1, float sinAngle1, CoordinateAxis axis1, 
            float cosAngle2, float sinAngle2, CoordinateAxis axis2) 
    {
        // Ensure this method has proper arguments
        assert( axis1.isDifferentAxis( axis2 ) );

        CoordinateAxis axis3 = axis1.getThirdAxis( axis2 );
        int i = axis1.ordinal();
        int j = axis2.ordinal();
        int k = axis3.ordinal();

        Rotation R = this;
        R.set(i, i,  cosAngle2);
        R.set(i, j,  0);
        R.set(i, k,  sinAngle2);
        R.set(j, i,  sinAngle2 * sinAngle1);
        R.set(j, j,  cosAngle1);
        R.set(j, k, -sinAngle1 * cosAngle2);
        R.set(k, i, -sinAngle2 * cosAngle1);
        R.set(k, j,  sinAngle1);
        R.set(k, k,  cosAngle1 * cosAngle2);
        
        return this;
    }


    private float square(float a) {return a*a;}
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 9; ++i) {
            sb.append(data[i]);
            if (i < 8) sb.append(", ");
            if ( (i == 2) || (i == 5) )
                sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
    
    public float trace() {
        return data[0] + data[4] + data[8];
    }

    public Rotation transpose() {
        set(data[0], data[3], data[6],
            data[1], data[4], data[7],
            data[2], data[5], data[8]);
        return this;
    }

}
