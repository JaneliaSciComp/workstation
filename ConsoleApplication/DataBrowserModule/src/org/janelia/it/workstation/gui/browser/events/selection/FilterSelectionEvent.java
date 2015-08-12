package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.workstation.gui.browser.model.DomainObjectId;
import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.workstation.gui.browser.nodes.FilterNode;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FilterSelectionEvent extends DomainObjectSelectionEvent {

    public FilterSelectionEvent(Object source, DomainObjectId identifier, boolean select, Filter filter) {
        super(source, identifier, filter, select, true);
    }
    
    public FilterSelectionEvent(Object source, DomainObjectId identifier, boolean select, FilterNode filter) {
        super(source, identifier, filter, select, true);
    }
    
    public FilterNode getFilterNode() {
        return (FilterNode)getDomainObjectNode();
    }
    
    public Filter getFilter() {
        return (Filter)getDomainObject();
    }
}
