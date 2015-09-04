package org.janelia.it.workstation.gui.geometric_search.viewer.dataset;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 8/6/2015.
 */
public class DatasetModel implements VoxelViewerEventListener {

    private static final Logger logger = LoggerFactory.getLogger(DatasetModel.class);

    List<Dataset> datasetList = new ArrayList<>();

    Dataset selectedDataset=null;

    public void addDataset(Dataset dataset) {
        datasetList.add(dataset);
        EventManager.sendEvent(this, new DatasetAddedEvent(dataset));
        setSelectedDataset(dataset);
    }

    public void removeDataset(Dataset dataset) {
        datasetList.remove(dataset);
        EventManager.sendEvent(this, new DatasetRemovedEvent(dataset));
    }

    public Dataset getSelectedDataset() {
        return selectedDataset;
    }

    public void setSelectedDataset(Dataset selectedDataset) {
        this.selectedDataset = selectedDataset;
        EventManager.sendEvent(this, new DatasetSelectedEvent(selectedDataset));
    }

    public void processEvent(VoxelViewerEvent event) {
        if (event instanceof RowSelectedEvent) {
            RowSelectedEvent rowSelectedEvent=(RowSelectedEvent)event;
            Component component=rowSelectedEvent.getComponent();
            for (Dataset dataset : datasetList) {
                if (dataset.getName().equals(component.getName())) {
                    setSelectedDataset(dataset);
                }
            }
        }
    }


}
