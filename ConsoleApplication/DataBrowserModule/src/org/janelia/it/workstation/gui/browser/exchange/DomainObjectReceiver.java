package org.janelia.it.workstation.gui.browser.exchange;

import org.janelia.it.workstation.gui.framework.viewer.search.SolrResultsMetaData;
import org.janelia.it.jacs.model.domain.DomainObject;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 06/07/16
 * Time: 11:11 AM
 *
 * Can implement this with anything that can accept domain objects, and hence remove implementation detail from
 * client of this interface.
 */
public interface DomainObjectReceiver {
    void setDomainObjects( List<DomainObject> domainObjects, SolrResultsMetaData solrResultsMetaData );
}
