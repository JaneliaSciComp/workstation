package org.janelia.it.workstation.gui.browser.events.selection;

import java.util.List;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SelectionModel<T,S> {
    
    public void select(T object, boolean clearAll);

    public void deselect(T object);

    public S getId(T object);
    
    public List<S> getSelectedIds();

    public boolean isSelected(S identifier);
}
