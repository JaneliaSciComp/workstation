package org.janelia.it.workstation.gui.geometric_search.viewer.dataset;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
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

    List<VoxelViewerEventListener> addDatasetListeners=new ArrayList<>();

    List<VoxelViewerEventListener> removeDatasetListeners=new ArrayList<>();

    public void addDataset(Dataset dataset) {
        datasetList.add(dataset);
        for (VoxelViewerEventListener listener : addDatasetListeners) {
            listener.processEvent(dataset);
        }
    }

    public void removeDataset(Dataset dataset) {
        for (VoxelViewerEventListener listener : removeDatasetListeners) {
            listener.processEvent(dataset);
        }
        datasetList.remove(dataset);
    }

    public void addAddDatasetListener(VoxelViewerEventListener listener) {
        addDatasetListeners.add(listener);
    }

    public void addRemoveDatasetListener(VoxelViewerEventListener listener) {
        removeDatasetListeners.add(listener);
    }

}
