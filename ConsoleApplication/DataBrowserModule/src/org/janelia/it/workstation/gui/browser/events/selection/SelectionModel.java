package org.janelia.it.workstation.gui.browser.events.selection;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SelectionModel<T,S> {
    
    private static final Logger log = LoggerFactory.getLogger(DomainObjectNodeSelectionModel.class);
    
    private final List<S> selected = new ArrayList<>();
    private Object source;
    
    public Object getSource() {
        return source;
    }
    
    public void setSource(Object source) {
        this.source = source;
    }
    
    public void select(T object, boolean clearAll) {
        log.debug("select {}, clear={}",object,clearAll);
        S id = getId(object);
        if (clearAll && selected.contains(id) && selected.size()==1) {
            // Already single selected
            return;
        }
        if (!clearAll && selected.contains(id) && selected.size()>1) {
            // Already multiple selected
            return;
        }
        if (clearAll) {
            selected.clear();
        }
        selected.add(id);
        notify(object, id, true, clearAll);
        
    }

    public void deselect(T object) {
        log.debug("deselect {}",object);
        S id = getId(object);
        if (!selected.contains(id)) {
            return;
        }
        selected.remove(id);
        notify(object, id, false, false);
    }

    protected abstract void notify(T object, S id, boolean select, boolean clearAll);
    
    public abstract S getId(T object);
    
    public List<S> getSelectedIds() {
        return selected;
    }
    
    public boolean isSelected(S id) {
        return selected.contains(id);
    }
    
    public S getLastSelectedId() {
        if (selected.isEmpty()) return null;
        return selected.get(selected.size()-1);
    }
}
