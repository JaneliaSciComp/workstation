/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.viewer3d.volume_builder;

import org.janelia.it.jacs.model.TestCategories;
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
public class PiecewiseVolumeDataBeanTest {
    
    private static PiecewiseVolumeDataBean volumeDataBean;
    private static StringBuilder allData;
    
    public static final String DATA3 = new String("Four score and seven years ago our fathers brought forth on this continent, a new nation, conceived in Liberty, and dedicated to the proposition that all men are created equal.\n"
            + "\n"
            + "Now we are engaged in a great civil war, testing whether that nation, or any nation so conceived and so dedicated, can long endure. We are met on a great battle-field of that war. We have come to dedicate a portion of that field, as a final resting place for those who here gave their lives that that nation might live. It is altogether fitting and proper that we should do this.\n"
            + "\n"
            + "But, in a larger sense, we can not dedicate -- we can not consecrate -- we can not hallow -- this ground. The brave men, living and dead, who struggled here, have consecrated it, far above our poor power to add or detract. The world will little note, nor long remember what we say here, but it can never forget what they did here. It is for us the living, rather, to be dedicated here to the unfinished work which they who fought here have thus far so nobly advanced. It is rather for us to be here dedicated to the great task remaining before us -- that from these honored dead we take increased devotion to that cause for which they gave the last full measure of devotion -- that we here highly resolve that these dead shall not have died in vain -- that this nation, under God, shall have a new birth of freedom -- and that government of the people, by the people, for the people, shall not perish from the earth.\n"
            + "\n"
            + "Abraham Lincoln\n"
            + "November 19, 1863");
    public static final String DATA2 = "ZYX_ZYX_ZYX_ZYX_";
    public static final String DATA1 = "0123456789ABCDEF";

    public PiecewiseVolumeDataBeanTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
        volumeDataBean = null;
        allData = null;
    }
    
    @Before
    public void setUp() {
        if (volumeDataBean == null)
            volumeDataBean = new PiecewiseVolumeDataBean(16, 16, 4, 2, 1);
        if (allData == null)
            allData = new StringBuilder();
    }
    
    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
    @Test
    public void addData() {
        System.out.println("Running addData");
        volumeDataBean.addData(
                DATA1.getBytes()
        );
        allData.append(DATA1);
        volumeDataBean.addData(
                DATA2.getBytes()
        );
        allData.append(DATA2);
        volumeDataBean.addData(
                DATA3.getBytes()
        );
        allData.append(DATA3);
    }
    
    @Test
    public void examineData() {        
        System.out.println("Running examineData");
        StringBuilder builder = new StringBuilder();
        for (long l = 0; l < volumeDataBean.length(); l++) {
            byte val = volumeDataBean.getValueAt(l);
            if (val > 0) {
                builder.append((char)val);
            }
        }
        //System.out.println("-------------------------------- Found content");
        //System.out.println(builder);
        assertEquals("Data added is not identical to data extracted.", builder.toString(), allData.toString());
    }
}
