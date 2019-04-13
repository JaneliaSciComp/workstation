package org.janelia.workstation.browser.nb_action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;

import org.janelia.workstation.integration.FrameworkImplProvider;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.events.selection.GlobalDomainObjectSelectionModel;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.sample.Sample;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * Allows the user to bind the "set publishing name" action to a key or toolbar button.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ActionID(
        category = "Core",
        id = "org.janelia.workstation.browser.nb_action.SetPublishingNameAction"
)
@ActionRegistration(
        displayName = "#CTL_SetPublishingNameAction"
)
@ActionReferences({
        @ActionReference(path = "Shortcuts", name = "D-P")
})
@Messages("CTL_SetPublishingNameAction=Choose Line Publishing Name")
public class SetPublishingNameAction extends AbstractAction {

    private List<Sample> samples;

    public SetPublishingNameAction() {
        super("Choose Line Publishing Name");
    }
    
    public SetPublishingNameAction(List<Sample> samples) {
        super(getName(samples));
        this.samples = samples;
    }

    private static String getName(List<Sample> samples) {
        if (samples!=null) {
            if (samples.size()>1) {
                return "Choose Line Publishing Name on "+samples.size()+" Samples";
            }
        }
        return "Choose Line Publishing Name";
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
            SetPublishingNameActionListener actionListener = new SetPublishingNameActionListener(samples);
            actionListener.actionPerformed(null);
        }
        catch (Exception ex) {
            FrameworkImplProvider.handleException(ex);
        }
    }
}
