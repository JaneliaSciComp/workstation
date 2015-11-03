/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.shared.util;

import org.janelia.it.jacs.shared.swc.SWCData;
import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.experimental.categories.Category;

/**
 *
 * @author fosterl
 */
@Category(TestCategories.FastTests.class)
public class SWCDataTest {
    
    private static final String ROOT_LINE = "27      5       -485.884525     1337.089427     114.742765      1.000000        -1";
    private static final String REORDER_LINE = "28      5       -482.881719     1343.394478     114.742765      1.000000        27";
    private static final String POST_REPORDER_LINE = "2      5       -482.881719     1343.394478     114.742765      1.000000        1";
    
    public SWCDataTest() {
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
    public void testReorder() {
        SWCData swcdata = new SWCData();
        final int firstNonDigitPosition = StringUtils.findFirstNonDigitPosition(ROOT_LINE);
        assertEquals("First non-digit-pos incorrect.", 2, firstNonDigitPosition);
        final String rootOffsetStr = ROOT_LINE.substring(0, firstNonDigitPosition);
        assertEquals("Root offset string incorrect.", "27", rootOffsetStr);
        int rootOffset = Integer.parseInt( rootOffsetStr ) - 1;
        assertEquals("Root offset.", 26, rootOffset);
        String reordered = swcdata.reorder(REORDER_LINE, rootOffset);  
        assertEquals("Reordered line is flawed.", POST_REPORDER_LINE, reordered);
        //System.out.println(reordered);
    }
}
