package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.domain.Neuron;
import org.janelia.it.FlyWorkstation.model.domain.Sample;
import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 8/26/13
 * Time: 11:30 AM
 *
 * This is a means of creating simplistic entities/wrappers for presentation.  Test data only.
 */
public class DummyDataSource {
    /*
        // This impl is to create entities that look normal, out of the input files.
        File loc = new File("/Volumes/jacsData/fosterl/maskFromStack");
        File maskFile = new File( loc, "maskFromStack.mask" );
        File chanFile = new File( loc, "maskFromStack.chan" );
        if ( ! maskFile.canRead()  ||  ! chanFile.canRead() ) {
            logger.error("Check exists/can read {} and {}.", maskFile, chanFile );
            throw new RuntimeException( "Cannot open the mask/chan experimental files." );
        }

        // Establish an ersatz sample.

        MaskChanRenderableData dummyContainerRenderable = getNonRenderingRenderableData(sampleInnerEntity);
        rtnVal.add( dummyContainerRenderable );

        // Now establish an ersatz neuron.
        int count = getRenderableData(
                rtnVal,
                nextTranslatedNum ++,
                maskFile,
                chanFile,
                false
        );
     */

    /**
     * Establishes an ersatz sample.
     *
     * @return sample suitable for representing top of a hierarchy containing a reference.
     */
    public Sample getSample() {
        Sample rtnVal = null;
        try {
            Entity sampleInnerEntity = new Entity();
            sampleInnerEntity.setId( 444444L );
            sampleInnerEntity.setName( "Experimental Containing Sample");
            EntityType type = new EntityType();
            type.setName( EntityConstants.TYPE_SAMPLE );
            type.setId( 333333L );
            sampleInnerEntity.setEntityType( type );
            RootedEntity sampleRootedEntity = new RootedEntity( sampleInnerEntity );
            EntityWrapper dummySampleItemEntity = new Sample( sampleRootedEntity );
            Sample dummySample = (Sample)dummySampleItemEntity;
            rtnVal = dummySample;

            Neuron neuron = getNeuron();
            rtnVal.addChild( neuron );

        } catch ( Exception ex ) {
            ex.printStackTrace();
        }

        return rtnVal;
    }

    private Neuron getNeuron() {
        Neuron rtnVal = null;

        try {
            Entity renderableEntity = new Entity();
            renderableEntity.setId(111111L);
            renderableEntity.setName("Experimental Reference Channel");

            EntityType type = new EntityType();
            type.setName(EntityConstants.TYPE_NEURON_FRAGMENT );
            type.setId( 222222L );
            RootedEntity sampleRootedEntity = new RootedEntity( renderableEntity );

            rtnVal = new Neuron( sampleRootedEntity );
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }

        return rtnVal;
    }
}
