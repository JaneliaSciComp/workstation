/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.geometry3d;

import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author cmbruns
 */
public class Vector3Test {
    
    public Vector3Test() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of applyRotation method, of class Vector3.
     */
    @Test
    public void testApplyRotation() {
        System.out.println("applyRotation");
        Rotation r = new Rotation().setFromAxisAngle(new Vector3(0, 1, 0), (float)(3*Math.PI/2));
        Vector3 instance = new Vector3(2, 1, 3);
        Vector3 expResult = new Vector3(-3, 1, 2);
        Vector3 result = instance.applyRotation(r);
        assertArrayEquals(expResult.toArray(), result.toArray(), 0.01f);
    }

    /**
     * Test of toArray method, of class Vector3.
     */
    @Test
    public void testAsArray() {
        System.out.println("asArray");
        Vector3 instance = new Vector3(1, 2, 3);
        float[] expResult = new float[] {1, 2, 3};
        float[] result = instance.toArray();
        assertArrayEquals(expResult, result, 0.01f);
    }

    /**
     * Test of copy method, of class Vector3.
     */
    @Test
    public void testCopy() {
        System.out.println("copy");
        Vector3 rhs = new Vector3(1, 2, 3.2f);
        Vector3 instance = new Vector3(0, 0, 1);
        instance.copy(rhs);
        assertArrayEquals(instance.toArray(), rhs.toArray(), 0.1f);
    }

    /**
     * Test of dot method, of class Vector3.
     */
    @Test
    public void testDot() {
        System.out.println("dot");
        Vector3 rhs = new Vector3(-1, 1, 2);
        Vector3 instance = new Vector3(1, 1, -1);
        float expResult = -2.0F;
        float result = instance.dot(rhs);
        assertEquals(expResult, result, 0.01);
    }

    /**
     * Test of add method, of class Vector3.
     */
    @Test
    public void testAdd() {
        System.out.println("add");
        Vector3 rhs = new Vector3(6, 7, 8);
        Vector3 instance = new Vector3(5, 4, 2);
        Vector3 expResult = new Vector3(11, 11, 10);
        Vector3 result = instance.add(rhs);
        assertEquals(expResult, result);
    }

    /**
     * Test of equals method, of class Vector3.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        Object obj = new Vector3(1, 2, 3);
        Object obj2 = new Vector3(1, 2, 4);
        Vector3 instance = new Vector3(1, 2, 3);
        boolean expResult = true;
        boolean result = instance.equals(obj);
        assertEquals(expResult, result);
        expResult = false;
        assertEquals(expResult, instance.equals(obj2));
        assertEquals(obj, instance);
    }

    /**
     * Test of getX method, of class Vector3.
     */
    @Test
    public void testGetX() {
        System.out.println("getX");
        Vector3 instance = new Vector3(1, 2, 3);
        float expResult = 1.0F;
        float result = instance.getX();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getY method, of class Vector3.
     */
    @Test
    public void testGetY() {
        System.out.println("getY");
        Vector3 instance = new Vector3(1, 2, 3);
        float expResult = 2.0F;
        float result = instance.getY();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getZ method, of class Vector3.
     */
    @Test
    public void testGetZ() {
        System.out.println("getZ");
        Vector3 instance = new Vector3(1, 2, 3);
        float expResult = 3.0F;
        float result = instance.getZ();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of hashCode method, of class Vector3.
     */
    @Test
    public void testHashCode() {
        System.out.println("hashCode");
        Vector3 instance = new Vector3(1, 2, 3);
        int expResult = Arrays.hashCode(new float[] {1, 2, 3});
        int result = instance.hashCode();
        assertEquals(expResult, result);
    }

    /**
     * Test of negate method, of class Vector3.
     */
    @Test
    public void testNegate() {
        System.out.println("negate");
        Vector3 instance = new Vector3(1, 2, 3);
        Vector3 expResult = new Vector3(-1, -2, -3);
        Vector3 result = instance.negate();
        assertEquals(expResult, result);
    }

    /**
     * Test of norm method, of class Vector3.
     */
    @Test
    public void testNorm() {
        System.out.println("norm");
        Vector3 instance = new Vector3(1, 2, 3);
        float expResult = 3.742F;
        float result = instance.length();
        assertEquals(expResult, result, 0.1);
    }

    /**
     * Test of normalize method, of class Vector3.
     */
    @Test
    public void testNormalize() {
        System.out.println("normalize");
        Vector3 instance = new Vector3(1,2,3);
        float expResult = 1.0f;
        float result = instance.normalize().length();
        assertEquals(expResult, result, 0.1f);
    }

    /**
     * Test of normSquared method, of class Vector3.
     */
    @Test
    public void testNormSquared() {
        System.out.println("normSquared");
        Vector3 instance = new Vector3(1,2,3);
        float expResult = 14.0F;
        float result = instance.lengthSquared();
        assertEquals(expResult, result, 0.1f);
    }

    /**
     * Test of multiplyScalar method, of class Vector3.
     */
    @Test
    public void testMultiplyScalar() {
        System.out.println("multiplyScalar");
        float s = 2.5F;
        Vector3 instance = new Vector3(1, 2, 3);
        Vector3 expResult = new Vector3(2.5f, 5f, 7.5f);
        Vector3 result = instance.multiplyScalar(s);
        assertEquals(expResult, result);
    }

    /**
     * Test of asTransform method, of class Vector3.
     */
    @Test
    public void testAsTransform() {
        System.out.println("asTransform");
        Vector3 instance = new Vector3(1, 2, 3);
        Matrix4 expResult = new Matrix4(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                1, 2, 3, 1
        );
        Matrix4 result = instance.asTransform();
        assertEquals(expResult, result);
    }

    /**
     * Test of setX method, of class Vector3.
     */
    @Test
    public void testSetX() {
        System.out.println("setX");
        float x = -2.5F;
        Vector3 instance = new Vector3(1, 2, 3);
        Vector3 expResult = new Vector3(-2.5f, 2, 3);
        Vector3 result = instance.setX(x);
        assertEquals(expResult, result);
    }

    /**
     * Test of setY method, of class Vector3.
     */
    @Test
    public void testSetY() {
        System.out.println("setY");
        float y = -2.5f;
        Vector3 instance = new Vector3(1, 2, 3);
        Vector3 expResult = new Vector3(1, -2.5f, 3);
        Vector3 result = instance.setY(y);
        assertEquals(expResult, result);
    }

    /**
     * Test of setZ method, of class Vector3.
     */
    @Test
    public void testSetZ() {
        System.out.println("setZ");
        float z = 2.5F;
        Vector3 instance = new Vector3(1, 2, 3);
        Vector3 expResult = new Vector3(1, 2, 2.5f);
        Vector3 result = instance.setZ(z);
        assertArrayEquals(expResult.toArray(), result.toArray(), 0.1f);
    }

    /**
     * Test of toString method, of class Vector3.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        Vector3 instance = new Vector3(1, 2, 3);
        String expResult = "[1.0, 2.0, 3.0]";
        String result = instance.toString();
        assertEquals(expResult, result);
    }
    
}
