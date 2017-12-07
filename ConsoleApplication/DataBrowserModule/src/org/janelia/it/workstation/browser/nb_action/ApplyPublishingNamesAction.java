package org.janelia.it.workstation.browser.nb_action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.sample.Sample;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * Allows the user to bind the "apply publishing names" action to a key or toolbar button.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Core",
        id = "org.janelia.it.workstation.browser.nb_action.ApplyPublishingNamesAction"
)
@ActionRegistration(
        displayName = "#CTL_ApplyPublishingNamesAction"
)
@ActionReferences({
        @ActionReference(path = "Shortcuts", name = "S-D-P")
})
@Messages("CTL_ApplyPublishingNamesAction=Apply Line Publishing Names")
public class ApplyPublishingNamesAction extends AbstractAction {

    private List<Sample> samples;

    public ApplyPublishingNamesAction() {
        super("Apply Line Publishing Names");
    }
    
    public ApplyPublishingNamesAction(List<Sample> samples) {
        super(getName(samples));
        this.samples = samples;
    }

    private static String getName(List<Sample> samples) {
        if (samples!=null) {
            if (samples.size()>1) {
                return "Apply Line Publishing Names on "+samples.size()+" Samples";
            }
        }
        return "Apply Line Publishing Names";
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Important that we don't set the instance variable, because there is a shared instance of this class
        // that NetBeans uses for all shortcut invocations.
        List<Sample> samples = this.samples;
        try {
            if (samples==null) {
                samples = new ArrayList<>();
                List<Reference> selectedIds = GlobalDomainObjectSelectionModel.getInstance().getSelectedIds();
                List<DomainObject> selected = DomainMgr.getDomainMgr().getModel().getDomainObjects(selectedIds);
                for(DomainObject domainObject : selected) {
                    if (domainObject instanceof Sample) {
                        samples.add((Sample)domainObject);
                    }
                }
            }
            ApplyPublishingNamesActionListener actionListener = new ApplyPublishingNamesActionListener(samples);
            actionListener.actionPerformed(null);
        }
        catch (Exception ex) {
            ConsoleApp.handleException(ex);
        }
    }
}
