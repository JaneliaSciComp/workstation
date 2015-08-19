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

    List<VoxelViewerEventListener> additionListeners=new ArrayList<>();

    List<VoxelViewerEventListener> removalListeners=new ArrayList<>();

    public void addDataset(Dataset dataset) {
        datasetList.add(dataset);
        for (VoxelViewerEventListener listener : additionListeners) {
            listener.processEvent(dataset);
        }
    }

    public void removeDataset(Dataset dataset) {
        for (VoxelViewerEventListener listener : removalListeners) {
            listener.processEvent(dataset);
        }
        datasetList.remove(dataset);
    }

    public void addAdditionListener(VoxelViewerEventListener listener) {
        additionListeners.add(listener);
    }

    public void addRemovalListener(VoxelViewerEventListener listener) {
        removalListeners.add(listener);
    }

}
