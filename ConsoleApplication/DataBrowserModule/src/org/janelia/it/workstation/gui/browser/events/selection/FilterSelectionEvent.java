package org.janelia.it.workstation.gui.browser.events.selection;

import java.util.Arrays;

import org.janelia.it.jacs.model.domain.gui.search.Filter;
import org.janelia.it.workstation.gui.browser.nodes.FilterNode;

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
