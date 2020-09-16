package org.janelia.horta.actions;

/**
 *
 * @author schauderd
 */

import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Viewport;
import org.janelia.horta.NeuronTracerTopComponent;
import org.janelia.scenewindow.SceneWindow;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionID(
        category = "Horta",
        id = "ZSliceDownAction"
)
@ActionRegistration(
        displayName = "Move Z Slice Down (3D)",
        lazy = true
)
@ActionReferences({
        @ActionReference(path = "Shortcuts", name = "S-Z")
})
public class ZSliceDownAction extends AbstractAction {

    private static final Logger log = LoggerFactory.getLogger(ZSliceDownAction.class);
    public ZSliceDownAction() {
        super("Move Z Slice Down (3D)");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
       NeuronTracerTopComponent nttc = NeuronTracerTopComponent.findThisComponent();
       SceneWindow sc = nttc.getSceneWindow();
       Vantage vantage = sc.getVantage();
       Viewport viewport = sc.getCamera().getViewport();
       viewport.setzNearRelative(.999f);
       viewport.setzFarRelative(1.001f);
       viewport.getChangeObservable().notifyObservers();
       vantage.setFocus(vantage.getFocus()[0],vantage.getFocus()[1],vantage.getFocus()[2]-2);
       vantage.notifyObservers();
    }
    
    @Override
    public void setEnabled(boolean newValue) {
        throw new IllegalStateException("Calling setEnabled directly is not supported");
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
