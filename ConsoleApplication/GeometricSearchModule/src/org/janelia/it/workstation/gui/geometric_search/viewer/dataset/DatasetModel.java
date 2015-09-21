package org.janelia.it.workstation.gui.geometric_search.viewer.dataset;

import org.janelia.it.workstation.gui.geometric_search.viewer.VoxelViewerEventListener;
import org.janelia.it.workstation.gui.geometric_search.viewer.actor.ActorSharedResource;
import org.janelia.it.workstation.gui.geometric_search.viewer.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        datasetList.remove(dataset); // order is important here
        removeSharedResourcesForDataset(dataset);
        EventManager.sendEvent(this, new DatasetRemovedEvent(dataset));
    }

    public Dataset getSelectedDataset() {
        return selectedDataset;
    }

    public void setSelectedDataset(Dataset dataset) {
        if (selectedDataset!=null) removeSharedResourcesForDataset(selectedDataset);
        this.selectedDataset = dataset;
        EventManager.sendEvent(this, new DatasetSelectedEvent(selectedDataset));
        addSharedResourcesForDataset(selectedDataset);
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

//    Map<String, Integer> createSharedResourceDependencyCount() {
//        Map<String, Integer> resourceCountMap=new HashMap<>();
//        if (selectedDataset==null) return resourceCountMap;
//
//        for (Dataset dataset : datasetList) {
//            List<ActorSharedResource> neededResources=dataset.getNeededActorSharedResources();
//            for (ActorSharedResource sharedResource : neededResources) {
//                Integer count=resourceCountMap.get(sharedResource.getName());
//                if (count==null || count==0) {
//                    resourceCountMap.put(sharedResource.getName(), 1);
//                } else {
//                    resourceCountMap.put(sharedResource.getName(), count + 1);
//                }
//            }
//        }
//        return resourceCountMap;
//    }

    private void addSharedResourcesForDataset(Dataset dataset) {
        List<ActorSharedResource> neededResources=dataset.getNeededActorSharedResources();
        if (neededResources!=null && neededResources.size()>0) {
            for (ActorSharedResource sharedResource : neededResources) {
                EventManager.sendEvent(this, new SharedResourceNeededEvent(sharedResource));
            }
        }
    }

    private void removeSharedResourcesForDataset(Dataset dataset) {
        List<ActorSharedResource> neededResources=dataset.getNeededActorSharedResources();
        if (neededResources!=null && neededResources.size()>0) {
            for (ActorSharedResource sharedResource : neededResources) {
                EventManager.sendEvent(this, new SharedResourceNotNeededEvent(sharedResource.getName()));
            }
        }
    }

}
