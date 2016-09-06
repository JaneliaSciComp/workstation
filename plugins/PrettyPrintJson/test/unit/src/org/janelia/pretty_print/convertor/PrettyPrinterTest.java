/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.pretty_print.convertor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test the pretty printer.
 *
 * @author fosterl
 */
public class PrettyPrinterTest {
    
    public PrettyPrinterTest() {
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

    @Test
    public void testPretty() {
        PrettyPrinter pr = new PrettyPrinter();
        String converted = pr.convert("{ \"name\":\"George Smith\", \"address\":{\"street\":\"22 Whatever Dr\", \"city\":\"Nondescript Village\",\"state\":\"OH\", \"zip\":\"11101\" } }");
        System.out.println(converted);
    }

}
