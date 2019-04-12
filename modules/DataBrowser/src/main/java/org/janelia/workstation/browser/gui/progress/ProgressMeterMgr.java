package org.janelia.workstation.browser.gui.progress;

import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.eventbus.Subscribe;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.workers.WorkerChangedEvent;
import org.janelia.workstation.core.events.workers.WorkerEndedEvent;
import org.janelia.workstation.core.events.workers.WorkerStartedEvent;
import org.janelia.workstation.browser.gui.components.ProgressTopComponent;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listen for worker events and ensure that the progress panel is shown to the user.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ProgressMeterMgr {

    private static final Logger log = LoggerFactory.getLogger(ProgressMeterMgr.class);

    // Singleton
    private static ProgressMeterMgr instance;
    public static synchronized ProgressMeterMgr getProgressMeterMgr() {
        if (instance==null) {
            instance = new ProgressMeterMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    private Set<BackgroundWorker> workerSet = new LinkedHashSet<>();

    @Subscribe
    public void processEvent(WorkerStartedEvent e) {
        ProgressTopComponent tc = ProgressTopComponent.ensureActive();
        tc.workerStarted(e.getWorker());
        workerSet.add(e.getWorker());
    }

    @Subscribe
    public void processEvent(WorkerChangedEvent e) {
        ProgressTopComponent tc = ProgressTopComponent.ensureActive();
        tc.workerChanged(e.getWorker());
    }

    @Subscribe
    public void processEvent(WorkerEndedEvent e) {
        ProgressTopComponent tc = ProgressTopComponent.ensureActive();
        tc.workerEnded(e.getWorker());
        workerSet.remove(e.getWorker());
    }

    public Set<BackgroundWorker> getActiveWorkers() {
        return workerSet;
    }
}
