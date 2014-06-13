package org.janelia.it.workstation.gui.alignment_board_viewer.masking;

import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.janelia.it.workstation.gui.viewer3d.masking.RenderMappingI;

import static org.junit.Assert.*;

/**
 * This checks that the values seeded in the mapping give expected results.  It checks that values in future runs
 * of this test match those yielded at time of writing.
 *
 * Created by fosterl on 1/31/14.
 */
@Category(TestCategories.FastTests.class)
public class ConfigurableColorMappingTest {

    public static final int TRANSLATED_NUM_FOR_COLOR_WHEEL_A = 2;
    public static final int TRANSLATED_NUM_FOR_COLOR_WHEEL_B = 4;
    public static final int TRANSLATED_NUM_FOR_COLOR_AVG = 5;
    public static final long ID_NUM_FOR_COLOR_AVG = 5;

    public static final byte[] RGB_FOR_NF1 = new byte[]{15, 75, 82, RenderMappingI.FRAGMENT_RENDERING};
    public static final byte[] RGB_FOR_COMP1 = new byte[]{25, 35, 12, RenderMappingI.COMPARTMENT_RENDERING};
    public static final int TRANSLATED_NUM_COMP1 = 3;
    public static final int TRANSLATED_NUM_NF1 = 1;
    public static final double[] CHANNEL_AVERAGES = new double[]{0.5, 0.25, 0.125};
    public static final byte[] RGB_FOR_CHANNEL_AVG = new byte[]{(byte)128, 64, 32};

    private static final Logger logger = LoggerFactory.getLogger( ConfigurableColorMappingTest.class );

    @Test
    public void testColorAssignments() {
        MultiMaskTracker testingTracker = MultiMaskTrackerTest.createMultiMaskTracker();
        FileStats fileStats = new FileStats();
        testingTracker.setFileStats( fileStats );
        ConfigurableColorMapping colorMapping = new ConfigurableColorMapping(
                testingTracker, fileStats
        );

        Collection<RenderableBean> renderablBeanCollection = new ArrayList<RenderableBean>();
        RenderableBean neuron1 = new RenderableBean();
        neuron1.setType(EntityConstants.TYPE_NEURON_FRAGMENT );
        neuron1.setRgb( RGB_FOR_NF1 );
        neuron1.setVoxelCount(3000L);
        neuron1.setInvertedY(false);
        neuron1.setLabelFileNum(10);
        neuron1.setTranslatedNum( TRANSLATED_NUM_NF1 );
        neuron1.setRenderableEntity(makeEntity( 10L, EntityConstants.TYPE_NEURON_FRAGMENT ));

        RenderableBean neuron2 = new RenderableBean();
        neuron2.setType(EntityConstants.TYPE_NEURON_FRAGMENT );
        // Leaving as null.  Allow calc to happen.
        // neuron2.setRgb(new byte[]{25, 35, 12, RenderMappingI.FRAGMENT_RENDERING});
        neuron2.setVoxelCount(2000L);
        neuron2.setInvertedY(false);
        neuron2.setLabelFileNum(11);
        neuron2.setTranslatedNum(TRANSLATED_NUM_FOR_COLOR_WHEEL_A);
        neuron2.setRenderableEntity( makeEntity(11L, EntityConstants.TYPE_NEURON_FRAGMENT) );

        RenderableBean neuron3 = new RenderableBean();
        neuron3.setType(EntityConstants.TYPE_NEURON_FRAGMENT );
        // Leaving as null.  Allow calc to happen.
        // neuron2.setRgb(new byte[]{25, 35, 12, RenderMappingI.FRAGMENT_RENDERING});
        neuron3.setVoxelCount(2500L);
        neuron3.setInvertedY(false);
        neuron3.setLabelFileNum(21);
        neuron3.setTranslatedNum( TRANSLATED_NUM_FOR_COLOR_AVG );
        neuron3.setRenderableEntity( makeEntity(ID_NUM_FOR_COLOR_AVG, EntityConstants.TYPE_NEURON_FRAGMENT) );
        // We will establish "average colors" for this one.
        fileStats.recordChannelAverages( ID_NUM_FOR_COLOR_AVG, CHANNEL_AVERAGES );

        RenderableBean compartment1 = new RenderableBean();
        compartment1.setType(EntityConstants.TYPE_COMPARTMENT);
        compartment1.setRgb( RGB_FOR_COMP1 );
        compartment1.setVoxelCount(18000L);
        compartment1.setInvertedY(false);
        compartment1.setLabelFileNum(12);
        compartment1.setTranslatedNum( TRANSLATED_NUM_COMP1 );
        compartment1.setRenderableEntity(makeEntity(12L, EntityConstants.TYPE_COMPARTMENT));

        RenderableBean compartment2 = new RenderableBean();
        compartment2.setType(EntityConstants.TYPE_COMPARTMENT);
        // NULL rgb.
        //compartment2.setRgb(new byte[]{11, 92, 51, RenderMappingI.COMPARTMENT_RENDERING});
        compartment2.setVoxelCount(17000L);
        compartment2.setInvertedY(false);
        compartment2.setLabelFileNum(13);
        compartment2.setTranslatedNum(TRANSLATED_NUM_FOR_COLOR_WHEEL_B);
        // NULL entity.
        //compartment2.setRenderableEntity(new Entity(13L));

        renderablBeanCollection.add( neuron1 );
        renderablBeanCollection.add( neuron2 );
        renderablBeanCollection.add( neuron3 );
        renderablBeanCollection.add( compartment1 );
        renderablBeanCollection.add( compartment2 );

        colorMapping.setRenderables( renderablBeanCollection );
        Map<Integer,byte[]> mapping = colorMapping.getMapping();

        // Check auto-generated stuff.

        // Here, the all-null / fall-through causes the color for this entity to be set to compartment rendering.
        // If RGB was null prior to getting the mapping, and there is an entity set in the bean, we'll see compartment rendering.
        byte[] rgbColorWheelA = mapping.get( TRANSLATED_NUM_FOR_COLOR_WHEEL_A );
        assertEquals("Not compartment rendering", rgbColorWheelA[3], RenderMappingI.COMPARTMENT_RENDERING);

        // No entity in this case: getting a translated color.
        byte[] rgbColorWheelB = mapping.get( TRANSLATED_NUM_FOR_COLOR_WHEEL_B );
        byte[] colorWheelEntryB = ConfigurableColorMapping.COLOR_WHEEL[ TRANSLATED_NUM_FOR_COLOR_WHEEL_B ];
        checkMatch(rgbColorWheelB, colorWheelEntryB);

        // Check explicitly-set stuff.
        checkMatch( mapping.get( TRANSLATED_NUM_NF1 ), RGB_FOR_NF1 );
        checkMatch( mapping.get( TRANSLATED_NUM_COMP1 ), RGB_FOR_COMP1 );

        // Check things set via averaging.
        checkMatch( mapping.get( TRANSLATED_NUM_FOR_COLOR_AVG ), RGB_FOR_CHANNEL_AVG );

        if ( logger.isDebugEnabled() ) {
            for ( Integer key: mapping.keySet() ) {
                logger.debug(
                        "For " + key +
                                " got [" + mapping.get(key)[0] + "," +
                                mapping.get(key)[1] + "," + mapping.get(key)[2] +
                                "," + mapping.get(key)[3] +
                                "]"
                );
            }
        }

        // Check one multi-mask.  Should fall back to neuron 1.
        checkMatch( mapping.get( 56 ), new byte[] { 15, 75,82,73 } );

    }

    private void checkMatch(byte[] rgbColorWheelA, byte[] colorWheelEntryA) {
        for ( int i = 0; i < 3; i++ ) {
            assertEquals("color mismatch for index " + i, rgbColorWheelA[i], colorWheelEntryA[i]);
        }
    }

    private Entity makeEntity(Long id, String type) {
        Entity rtnVal = new Entity(id);
        rtnVal.setEntityTypeName( type );
        rtnVal.setName( "Entity " + id );
        return rtnVal;
    }
}
