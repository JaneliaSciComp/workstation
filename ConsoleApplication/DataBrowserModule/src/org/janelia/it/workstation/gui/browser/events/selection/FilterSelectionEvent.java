package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.workstation.gui.browser.nodes.FilterNode;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FilterSelectionEvent extends DomainObjectSelectionEvent {

    public FilterSelectionEvent(Object source, boolean select, Filter filter, boolean isUserDriven) {
        super(source, filter, select, true, isUserDriven);
    }
    
    public FilterSelectionEvent(Object source, boolean select, FilterNode filter, boolean isUserDriven) {
        super(source, filter, select, true, isUserDriven);
    }
    
    public FilterNode getFilterNode() {
        return (FilterNode)getDomainObjectNode();
    }
    
    public Filter getFilter() {
        return (Filter)getDomainObject();
    }
}
