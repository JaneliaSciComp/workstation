package org.janelia.it.workstation.gui.browser.components;

import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.browser.events.selection.PipelineResultSelectionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * Manages the life cycle of domain list viewers based on user generated selected events. This manager
 * either reuses existing viewers, or creates them as needed and docks them in the appropriate place.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleResultViewerManager implements ViewerManager<SampleResultViewerTopComponent> {

    private final static Logger log = LoggerFactory.getLogger(SampleResultViewerManager.class);
    
    public static SampleResultViewerManager instance;
    
    private SampleResultViewerManager() {
    }
    
    public static SampleResultViewerManager getInstance() {
        if (instance==null) {
            instance = new SampleResultViewerManager();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    /* Manage the active instance of this top component */
    
    private SampleResultViewerTopComponent activeInstance;
    void activate(SampleResultViewerTopComponent instance) {
        activeInstance = instance;
    }
    boolean isActive(SampleResultViewerTopComponent instance) {
        return activeInstance == instance;
    }
    @Override
    public SampleResultViewerTopComponent getActiveViewer() {
        return activeInstance;
    }
    
    @Override
    public String getViewerName() {
        return "DomainListViewTopComponent";
    }

    @Override
    public Class<SampleResultViewerTopComponent> getViewerClass() {
        return SampleResultViewerTopComponent.class;
    }

    @Subscribe
    public void sampleResultSelected(PipelineResultSelectionEvent event) {
        
        log.info("sampleResultSelected({})",event.getPipelineResult());

        SampleResultViewerTopComponent viewer = SampleResultViewerManager.getInstance().getActiveViewer();
        
        if (viewer!=null) {   
            viewer.loadSampleResult(event.getPipelineResult(), event.isUserDriven(), null);
        }
    }
}
