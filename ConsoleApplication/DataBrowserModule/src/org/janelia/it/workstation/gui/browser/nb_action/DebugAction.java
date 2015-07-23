package org.janelia.it.workstation.gui.browser.nb_action;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.janelia.it.workstation.gui.browser.events.selection.DomainObjectNodeSelectionModel;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DebugAction extends AbstractAction implements ContextAwareAction {
    
    private Logger log = LoggerFactory.getLogger(DebugAction.class);
    
    private Lookup context;
    DomainObjectNodeSelectionModel selectionModel;
 
    public DebugAction() {
        this(Utilities.actionsGlobalContext());
    }
 
    public DebugAction(Lookup context) {
        putValue(Action.NAME, "Debug Action");
        this.context = context;
    }
 
    void init() {
        if (selectionModel != null) {
            return;
        }
        
        assert SwingUtilities.isEventDispatchThread() 
               : "this shall be called just from AWT thread";
 
        //The thing we want to listen for the presence or absence of
        //on the global selection
        selectionModel = context.lookup(DomainObjectNodeSelectionModel.class);
    }
    
 
    @Override
    public boolean isEnabled() {
        init();
        setEnabled(selectionModel.getSelectedIds().size()==1);
        putValue(Action.NAME, "Debug "+selectionModel.getSelectedIds().size()+" Instances, "+selectionModel.getSelectedIds());
        
        return super.isEnabled();
    }
 
    @Override
    public void actionPerformed(ActionEvent e) {
        init();
        log.info("DEBUG THIS: {}", selectionModel.getSelectedIds());
    }
 
    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new DebugAction(context);
    }
}
