package org.janelia.horta.actions;

/**
 *
 * @author schauderd
 */

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.horta.NeuronTracerTopComponent;
import org.janelia.horta.PlayReviewManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Horta",
        id = "DecreaseSpeedNeuronCamAction"
)
@ActionRegistration(
        displayName = "Decrease Speed Of NeuronCam",
        lazy = true
)

public class DecreaseSpeedNeuronCamAction extends AbstractAction {

    private NeuronTracerTopComponent context;
    private static final Logger log = LoggerFactory.getLogger(DecreaseSpeedNeuronCamAction.class);
    public DecreaseSpeedNeuronCamAction(NeuronTracerTopComponent horta) {
        super("Decrease Speed Of NeuronCam");
        context = horta;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        context.updatePlaybackSpeed(false);
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
