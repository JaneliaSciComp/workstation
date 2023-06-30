package org.janelia.horta.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.janelia.horta.NeuronTracerTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ActionID(
        category = "Horta",
        id = "org.janelia.horta.actions.LoadHortaTileAtFocusAction"
)
@ActionRegistration(
        iconBase = "org/janelia/horta/images/neuronTracerCubic16.png",
        displayName = "#CTL_LoadHortaTileAtFocusAction",
        key = "loadTileAtFocus"
)
@Messages("CTL_LoadHortaTileAtFocusAction=Load Horta Tile At Focus")

public final class LoadHortaTileAtFocusAction implements ActionListener {
    private NeuronTracerTopComponent context;
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    public LoadHortaTileAtFocusAction(NeuronTracerTopComponent horta) {
        context = horta;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            context.loadPersistentTileAtFocus();
        } catch (IOException ex) {
            LOG.info("Tile load failed", ex);
        }
    }
}

