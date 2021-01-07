package org.janelia.horta.actions;

/**
 *
 * @author schauderd
 */

import java.awt.event.ActionEvent;
import java.util.Set;

import javax.swing.AbstractAction;
import org.janelia.horta.NeuronTracerTopComponent;
import org.janelia.workstation.controller.eventbus.ViewerEvent;
import org.janelia.workstation.controller.model.TmModelManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Horta",
        id = "StopPlayReviewAction"
)
@ActionRegistration(
        displayName = "Stop Playback",
        lazy = true
)
public class StopPlayReviewAction extends AbstractAction {
    private NeuronTracerTopComponent context;
    private static final Logger log = LoggerFactory.getLogger(StopPlayReviewAction.class);
    public StopPlayReviewAction(NeuronTracerTopComponent horta) {
        super("Stop Play Review");
        context = horta;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Set<ViewerEvent.VIEWER> openViewers = TmModelManager.getInstance().getCurrentView().getViewerSet();
        if (!openViewers.contains(ViewerEvent.VIEWER.HORTA))
            return;
        context.stopPlaybackReview();
    }
    
    @Override
    public void setEnabled(boolean newValue) {
        throw new IllegalStateException("Calling setEnabled directly is not supported");
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    public void fireEnabledChangeEvent() {
        boolean enabled = isEnabled();
        firePropertyChange("enabled", Boolean.valueOf(!enabled), Boolean.valueOf(enabled));
    }
}
