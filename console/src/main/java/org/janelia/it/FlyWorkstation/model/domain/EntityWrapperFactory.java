package org.janelia.it.FlyWorkstation.model.domain;

import org.janelia.it.FlyWorkstation.model.entity.RootedEntity;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Wraps Entity objects in domain-specific EntityWrapper objects.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityWrapperFactory {

    public static EntityWrapper wrap(RootedEntity rootedEntity) {
        String type = rootedEntity.getEntity().getEntityType().getName();
        if (EntityConstants.TYPE_FOLDER.equals(type)) {
            return new Folder(rootedEntity);
        }
        else if (EntityConstants.TYPE_SAMPLE.equals(type)) {
            return new Sample(rootedEntity);
        }
        else if (EntityConstants.TYPE_NEURON_FRAGMENT.equals(type)) {
            return new Neuron(rootedEntity);
        }
        else if (EntityConstants.TYPE_ALIGNED_ITEM.equals(type)) {
            return new AlignedItem(rootedEntity);
        }
        else if (EntityConstants.TYPE_COMPARTMENT_SET.equals(type)) {
            return new CompartmentSet(rootedEntity);
        }
        else if (EntityConstants.TYPE_COMPARTMENT.equals(type)) {
            return new Compartment(rootedEntity);
        }
         throw new IllegalArgumentException("Cannot wrap entity type: "+type);
    }
}
