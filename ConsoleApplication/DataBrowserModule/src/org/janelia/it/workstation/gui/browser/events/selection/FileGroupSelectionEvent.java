package org.janelia.it.workstation.gui.browser.events.selection;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.sample.FileGroup;

/**
 * Event indicating that a file groups's selection has changed.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileGroupSelectionEvent {

    private final Object source;
    private final List<FileGroup> fileGroups;
    private final boolean select;
    private final boolean clearAll;
    private final boolean isUserDriven;

    public FileGroupSelectionEvent(Object source, List<? extends FileGroup> fileGroups, boolean select, boolean clearAll, boolean isUserDriven) {
        this.source = source;
        this.fileGroups = new ArrayList<>(fileGroups);
        this.select = select;
        this.clearAll = clearAll;
        this.isUserDriven = isUserDriven;
    }

    public Object getSource() {
        return source;
    }

    public FileGroup getObjectIfSingle() {
        return fileGroups.size()==1 ? fileGroups.get(0) : null;
    }
    
    public List<FileGroup> getDomainObjects() {
        return fileGroups;
    }

    public boolean isSelect() {
        return select;
    }

    public boolean isClearAll() {
        return clearAll;
    }

    public boolean isUserDriven() {
        return isUserDriven;
    }

    @Override
    public String toString() {
        return "FileGroupSelectionEvent [source=" + source + ", fileGroups=" + fileGroups
                + ", select=" + select + ", clearAll=" + clearAll + ", isUserDriven=" + isUserDriven + "]";
    }
}
