package org.janelia.workstation.browser.selection;

import java.util.List;

import org.janelia.model.domain.sample.FileGroup;
import org.janelia.workstation.core.events.selection.ObjectSelectionEvent;

/**
 * Event indicating that a file groups's selection has changed.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileGroupSelectionEvent extends ObjectSelectionEvent<FileGroup> {

    public FileGroupSelectionEvent(Object source, List<? extends FileGroup> fileGroups, boolean select, boolean clearAll, boolean isUserDriven) {
        super(source, fileGroups, select, clearAll, isUserDriven);
    }
}
