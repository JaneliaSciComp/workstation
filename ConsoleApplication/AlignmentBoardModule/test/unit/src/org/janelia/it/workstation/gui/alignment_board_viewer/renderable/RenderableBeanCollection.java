package org.janelia.it.workstation.gui.alignment_board_viewer.renderable;

import org.janelia.it.jacs.model.domain.compartments.Compartment;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.viewer3d.renderable.RenderableBean;
import org.janelia.it.workstation.gui.alignment_board.util.ABReferenceChannel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * This makes a collection of renderable beans which can be used by tests.
 * Created by fosterl on 1/29/14.
 */
public class RenderableBeanCollection {
    public List<RenderableBean> createCollection() {
        List<RenderableBean> rtnVal = new ArrayList<>();
        int nextLabelNum = 22;
        String[] validTypes = {
                NeuronFragment.class.getSimpleName(),
                Compartment.class.getSimpleName(),
                Sample.class.getSimpleName(),
                ABReferenceChannel.REF_CHANNEL_TYPE_NAME,
                CompartmentSet.class.getSimpleName()
        };
        Random random = new Random( new Date().getTime() );
        String nextEntityName = "AnEntity__";
        for ( int i = 0; i < 50; i++ ) {
            RenderableBean rb = new RenderableBean();
            rb.setInvertedY(false);
            rb.setLabelFileNum(nextLabelNum++);
            String validType = validTypes[random.nextInt(validTypes.length)];
            rb.setName(nextEntityName + i);
            rb.setId(random.nextLong());
            rb.setRgb(new byte[]{(byte) 128, 0, 0});
            rb.setType(validType);
            rb.setVoxelCount( (long)random.nextInt( 500 ) );
            rtnVal.add( rb );
        }
        return rtnVal;
    }
}
