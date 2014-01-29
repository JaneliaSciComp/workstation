package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.renderable;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

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
        List<RenderableBean> rtnVal = new ArrayList<RenderableBean>();
        int nextItemId = 11111111;
        int nextLabelNum = 22;
        String[] validTypes = {
                EntityConstants.TYPE_NEURON_FRAGMENT,
                EntityConstants.TYPE_COMPARTMENT,
                EntityConstants.TYPE_SAMPLE,
                "Reference",
                EntityConstants.TYPE_COMPARTMENT_SET
        };
        Random random = new Random( new Date().getTime() );
        String nextEntityName = "AnEntity__";
        for ( int i = 0; i < 50; i++ ) {
            RenderableBean rb = new RenderableBean();
            rb.setAlignedItemId( nextItemId );
            rb.setInvertedY(false);
            rb.setLabelFileNum(nextLabelNum++);
            Entity entity = new Entity();
            entity.setName( nextEntityName + i );
            String validType = validTypes[random.nextInt(validTypes.length)];
            entity.setEntityTypeName( validType );
            entity.setId( random.nextLong() );
            rb.setRenderableEntity(entity);
            rb.setAlignedItemId(nextItemId++);
            rb.setRgb(new byte[]{(byte) 128, 0, 0});
            rb.setType(validType);
            rb.setVoxelCount( (long)random.nextInt( 500 ) );
            rtnVal.add( rb );
        }
        return rtnVal;
    }
}
