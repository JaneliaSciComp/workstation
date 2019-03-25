package org.janelia.it.workstation.browser.events.selection;

import java.util.Arrays;

import org.janelia.it.workstation.browser.nodes.FilterNode;
import org.janelia.model.domain.gui.search.Filter;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FilterSelectionEvent extends DomainObjectSelectionEvent {

    public FilterSelectionEvent(Object source, Filter filter, boolean select, boolean clearAll, boolean isUserDriven) {
        super(source, Arrays.asList(filter), select, clearAll, isUserDriven);
    }
    
    public FilterSelectionEvent(Object source, FilterNode filter, boolean select, boolean clearAll, boolean isUserDriven) {
        super(source, filter, select, clearAll, isUserDriven);
    }
    
    public FilterNode getFilterNode() {
        return (FilterNode)getDomainObjectNode();
    }
    
    public Filter getFilter() {
        return (Filter)getObjectIfSingle();
    }
}
