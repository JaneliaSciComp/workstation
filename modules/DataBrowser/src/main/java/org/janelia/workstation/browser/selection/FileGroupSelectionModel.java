package org.janelia.workstation.browser.selection;

import java.util.List;

import org.janelia.model.domain.sample.FileGroup;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A selection model implementation which tracks the selection of file groups.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileGroupSelectionModel extends ChildSelectionModel<FileGroup,String> {

    private static final Logger log = LoggerFactory.getLogger(FileGroupSelectionModel.class);

    @Override
    protected void selectionChanged(List<FileGroup> domainObjects, boolean select, boolean clearAll, boolean isUserDriven) {
        log.debug("selectionChanged(objects.size={}, select={}, clearAll={}, isUserDriven={})",domainObjects.size(),select,clearAll,isUserDriven);
        Events.getInstance().postOnEventBus(new FileGroupSelectionEvent(getSource(), domainObjects, select, clearAll, isUserDriven));
    }
    
    @Override
    public String getId(FileGroup fileGroup) {
        return fileGroup.getKey();
    }
}
