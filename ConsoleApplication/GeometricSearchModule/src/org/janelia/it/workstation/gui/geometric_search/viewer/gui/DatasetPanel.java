package org.janelia.it.workstation.gui.geometric_search.viewer.gui;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.dataset.Dataset;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.DatasetAddedEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.DatasetSelectedEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.VoxelViewerEvent;

/**
 * Created by murphys on 8/20/2015.
 */
public class DatasetPanel extends ScrollableRowPanel implements VoxelViewerEventListener {

    @Override
    public void processEvent(VoxelViewerEvent event) {
        if (event instanceof DatasetAddedEvent) {
            DatasetAddedEvent datasetAddedEvent=(DatasetAddedEvent)event;
            Dataset dataset=datasetAddedEvent.getDataset();
            addEntry(dataset.getName());
        } else if (event instanceof DatasetSelectedEvent) {
            DatasetSelectedEvent datasetSelectedEvent=(DatasetSelectedEvent)event;
            Dataset dataset=datasetSelectedEvent.getDataset();
            setSelectedRowByName(dataset.getName());
        }
    }

}
