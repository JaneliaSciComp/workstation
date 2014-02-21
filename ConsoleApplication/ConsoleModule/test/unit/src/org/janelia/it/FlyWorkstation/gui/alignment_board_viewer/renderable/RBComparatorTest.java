package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable;

import org.janelia.it.jacs.model.entity.EntityConstants;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by fosterl on 1/29/14.
 */
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
            Assert.assertFalse(
                    "Should not encounter Neuron Fragments at this phase.",
                    phase.getRank() > Phase.NF.getRank() && bean.getType().equals(EntityConstants.TYPE_NEURON_FRAGMENT)
            );
            Assert.assertFalse(
                    "Should not encounter Neuron Fragments at this phase.",
                    phase.getRank() > Phase.REFERENCE.getRank() && bean.getType().equals("Reference")
            );
            Assert.assertFalse(
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
                phase = phase.COMPARTMENT;
            }
            else if ( bean.getType().equals( EntityConstants.TYPE_COMPARTMENT_SET ) ) {
                phase = Phase.COMPARTMENT_COLLECTION;
            }

            if ( phase != previousPhase ) {
                minVoxelCount = Long.MAX_VALUE;
                previousPhase = phase;
            }

            logger.debug("Bean " + bean.getRenderableEntity().getName() + " " + bean.getType() + " size=" + bean.getVoxelCount() + ", type=" + bean.getType());
            Assert.assertTrue(
                    "Rule of sort by descending voxel count violated. " + bean.getVoxelCount() + " v " + minVoxelCount,
                    bean.getVoxelCount() <= minVoxelCount
            );

            minVoxelCount = bean.getVoxelCount();
        }
    }
}
