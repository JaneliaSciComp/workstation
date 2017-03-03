package org.janelia.it.workstation.browser.gui.dialogs.download;

import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadWizardPanel2 implements WizardDescriptor.ValidatingPanel<WizardDescriptor> {

    private WizardDescriptor wiz;
    
    /**
     * The visual component that displays this panel. If you need to access the
     * component from this class, just use getComponent().
     */
    private DownloadVisualPanel2 component;

    @Override
    public DownloadVisualPanel2 getComponent() {
        if (component == null) {
            component = new DownloadVisualPanel2(this);
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
        isValid = false;
        for (ArtifactDescriptor artifactDescriptor : component.getArtifactDescriptors()) {
            if (!artifactDescriptor.getFileTypes().isEmpty()) {
                isValid = true;
                break;
            }
        }
        if (isValid) {
            wiz.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);
        }
        else {
            throw new WizardValidationException(null, "At least one result type must be selected", null);
        }
    }
    
    @Override
    public boolean isValid() {
        return isValid;
    }

    private ChangeSupport changeSupport = new ChangeSupport(this);
    
    @Override
    public void addChangeListener(ChangeListener l) {
        changeSupport.addChangeListener(l);
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
        changeSupport.removeChangeListener(l);
    }
    
    public final void fireChangeEvent() {
        try {
            validate();
        }
        catch (WizardValidationException e) {
            wiz.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, e.getMessage());
        }
        storeSettings(wiz);
        changeSupport.fireChange();
    }
    
    @Override
    public void readSettings(WizardDescriptor wiz) {
        this.wiz = wiz;
        DownloadWizardState state = (DownloadWizardState)wiz.getProperty(DownloadWizardIterator.PROP_WIZARD_STATE);
        getComponent().init(state);
    }

    @Override
    public void storeSettings(WizardDescriptor wiz) {
        DownloadWizardState state = (DownloadWizardState)wiz.getProperty(DownloadWizardIterator.PROP_WIZARD_STATE);
        state.setArtifactDescriptors(getComponent().getArtifactDescriptors());
        //NbPreferences.forModule(DownloadWizardState.class).putBoolean("has3d", state.has3d());
    }

}
