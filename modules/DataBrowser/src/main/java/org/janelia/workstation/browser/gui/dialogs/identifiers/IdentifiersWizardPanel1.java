package org.janelia.workstation.browser.gui.dialogs.identifiers;

import javax.swing.event.ChangeListener;

import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;

public class IdentifiersWizardPanel1 implements WizardDescriptor.ValidatingPanel<WizardDescriptor> {

    private final ChangeSupport changeSupport = new ChangeSupport(this);    
    private WizardDescriptor wiz;
    
    /**
     * The visual component that displays this panel. If you need to access the
     * component from this class, just use getComponent().
     */
    private IdentifiersVisualPanel1 component;

    @Override
    public IdentifiersVisualPanel1 getComponent() {
        if (component == null) {
            component = new IdentifiersVisualPanel1(this);
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
        isValid = !component.getText().isEmpty();
        if (isValid) {
            wiz.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);
        }
        else {
            throw new WizardValidationException(null, "Enter at least one identifier to search for", null);
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
        state.setText(getComponent().getText());
    }

}
