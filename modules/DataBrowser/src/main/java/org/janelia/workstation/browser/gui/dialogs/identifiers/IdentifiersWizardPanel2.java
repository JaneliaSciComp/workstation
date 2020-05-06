package org.janelia.workstation.browser.gui.dialogs.identifiers;

import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;

import javax.swing.event.ChangeListener;

public class IdentifiersWizardPanel2 implements WizardDescriptor.ValidatingPanel<WizardDescriptor> {

    private final ChangeSupport changeSupport = new ChangeSupport(this);    
    private WizardDescriptor wiz;
    
    /**
     * The visual component that displays this panel. If you need to access the
     * component from this class, just use getComponent().
     */
    private IdentifiersVisualPanel2 component;

    @Override
    public IdentifiersVisualPanel2 getComponent() {
        if (component == null) {
            component = new IdentifiersVisualPanel2(this);
        }
        return component;
    }

    @Override
    public HelpCtx getHelp() {
        // Show no Help button for this panel:
        return HelpCtx.DEFAULT_HELP;
        // If you have context help:
        // return new HelpCtx("help.key.here");
    }
    
    private boolean isValid = false;
    
    @Override
    public void validate() throws WizardValidationException {
        isValid = !component.isSearching();
        if (isValid) {
            wiz.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);
        }
        else {
            throw new WizardValidationException(null, null, null);
        }
    }
    
    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public void addChangeListener(ChangeListener l) {
        changeSupport.addChangeListener(l);
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
        changeSupport.removeChangeListener(l);
    }
    
    private boolean firstException = true;
    public final void fireChangeEvent() {
        try {
            validate();
        }
        catch (WizardValidationException e) {
            // We don't create an error message when the panel appears, because the user hasn't done anything yet.
            if (!firstException) {
                wiz.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, e.getMessage());
            }
            firstException = false;
        }
        storeSettings(wiz);
        changeSupport.fireChange();
    }
    
    @Override
    public void readSettings(WizardDescriptor wiz) {
        this.wiz = wiz;
        IdentifiersWizardState state = (IdentifiersWizardState)wiz.getProperty(IdentifiersWizardIterator.PROP_WIZARD_STATE);
        getComponent().init(state);
    }

    @Override
    public void storeSettings(WizardDescriptor wiz) {
        ActivityLogHelper.logUserAction("IdentifiersWizard.storeSettings", 1);
        IdentifiersWizardState state = (IdentifiersWizardState)wiz.getProperty(IdentifiersWizardIterator.PROP_WIZARD_STATE);
        state.setResults(getComponent().getResults());
    }

}
