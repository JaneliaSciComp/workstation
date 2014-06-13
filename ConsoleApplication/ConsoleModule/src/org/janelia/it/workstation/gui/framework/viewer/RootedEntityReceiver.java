package org.janelia.it.workstation.gui.framework.viewer;

import org.janelia.it.workstation.gui.framework.viewer.search.SolrResultsMetaData;
import org.janelia.it.workstation.model.entity.RootedEntity;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 12/16/13
 * Time: 1:49 PM
 *
 * Can implement this with anything that can accept rooted entities, and hence remove implementation detail from
 * client of this interface.
 */
public interface RootedEntityReceiver {
    void setRootedEntities( List<RootedEntity> rootedEntities, SolrResultsMetaData solrResultsMetaData );
}
