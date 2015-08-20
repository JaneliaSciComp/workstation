package org.janelia.it.workstation.gui.geometric_search.viewer.dataset;

import org.janelia.it.workstation.gui.geometric_search.viewer.event.DatasetRemovedEvent;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.EventManager;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.DatasetSelectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by murphys on 8/6/2015.
 */
public class DatasetModel {

    private static final Logger logger = LoggerFactory.getLogger(DatasetModel.class);

    List<Dataset> datasetList = new ArrayList<>();

    Dataset selectedDataset=null;

    public void addDataset(Dataset dataset) {
        datasetList.add(dataset);
        EventManager.sendEvent(this, new DatasetSelectedEvent(dataset));
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

}
