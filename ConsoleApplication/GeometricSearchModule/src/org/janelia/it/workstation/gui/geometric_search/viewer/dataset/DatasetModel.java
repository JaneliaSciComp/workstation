package org.janelia.it.workstation.gui.geometric_search.viewer.dataset;

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

    public void addDataset(Dataset dataset) {
        datasetList.add(dataset);
    }

    public void removeDataset(Dataset dataset) {
        datasetList.remove(dataset);
    }


}
