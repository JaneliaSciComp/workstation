package org.janelia.it.FlyWorkstation.loader.renderable;

import org.janelia.it.jacs.model.TestCategories;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.loader.renderable.InvertingComparator;
import org.janelia.it.jacs.shared.loader.renderable.RBComparator;
import org.janelia.it.jacs.shared.loader.renderable.RenderableBean;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests the {@link RBComparator} class.
 *
 * @author Les Foster
 */
@Category(TestCategories.FastTests.class)
public class RBComparatorTest {
    private static enum Phase {
        NF(0), REFERENCE(1), COMPARTMENT(2), SAMPLE(3), COMPARTMENT_COLLECTION(4);

        private int rank;
        Phase( int rank ) {
            this.rank = rank;
        }

        public int getRank() { return rank; }
    }

    private Logger logger;
    public RBComparatorTest() {
        logger = LoggerFactory.getLogger( RBComparatorTest.class );
    }

    @Test
    public void testCompare() throws Exception {
        RBComparator comparator = new RBComparator();
        List<RenderableBean> beanList = new RenderableBeanCollection().createCollection();

        Collections.sort( beanList, new InvertingComparator( comparator ) );

        // Here's the tough part: checking that the order is as expected.
        checkCollationConformation(beanList);
    }

    public void checkCollationConformation(List<RenderableBean> beanList) {
        Phase phase = Phase.NF;
        Phase previousPhase = Phase.NF;
        long minVoxelCount = Long.MAX_VALUE;
        for ( RenderableBean bean: beanList ) {
            assertFalse(
                    "Should not encounter Neuron Fragments at this phase.",
                    phase.getRank() > Phase.NF.getRank() && bean.getType().equals(EntityConstants.TYPE_NEURON_FRAGMENT)
            );
            assertFalse(
                    "Should not encounter Neuron Fragments at this phase.",
                    phase.getRank() > Phase.REFERENCE.getRank() && bean.getType().equals("Reference")
            );
            assertFalse(
                    "Should not encounter Compartments at this phase.",
                    phase.getRank() > Phase.COMPARTMENT.getRank() && bean.getType().equals(EntityConstants.TYPE_COMPARTMENT)
            );

            if ( bean.getType().equals( "Reference" ) ) {
                phase = Phase.REFERENCE;
            }
            else if ( bean.getType().equals( EntityConstants.TYPE_SAMPLE ) ) {
                phase = Phase.SAMPLE;
            }
            else if ( bean.getType().equals( EntityConstants.TYPE_NEURON_FRAGMENT ) ) {
                phase = Phase.NF;
            }
            else if ( bean.getType().equals( EntityConstants.TYPE_COMPARTMENT ) ) {
                phase = Phase.COMPARTMENT;
            }
            else if ( bean.getType().equals( EntityConstants.TYPE_COMPARTMENT_SET ) ) {
                phase = Phase.COMPARTMENT_COLLECTION;
            }

            if ( phase != previousPhase ) {
                minVoxelCount = Long.MAX_VALUE;
                previousPhase = phase;
            }

            logger.debug("Bean " + bean.getRenderableEntity().getName() + " " + bean.getType() + " size=" + bean.getVoxelCount() + ", type=" + bean.getType());
            assertTrue(
                    "Rule of sort by descending voxel count violated. " + bean.getVoxelCount() + " v " + minVoxelCount,
                    bean.getVoxelCount() <= minVoxelCount
            );

            minVoxelCount = bean.getVoxelCount();
        }
    }
}
