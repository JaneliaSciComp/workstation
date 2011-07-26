package org.janelia.it.FlyWorkstation.api.facade.abstract_facade;

import org.janelia.it.FlyWorkstation.api.facade.abstract_facade.fundtype.EntityLoader;
import org.janelia.it.FlyWorkstation.api.stub.data.NoData;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 7/22/11
 * Time: 4:51 PM
 */
public interface OntologyLoader extends EntityLoader {
    Entity[] getOntologies();
}
