package org.janelia.workstation.browser.gui.dialogs.download;

import javax.swing.event.ChangeListener;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadWizardPanel2 implements WizardDescriptor.ValidatingPanel<WizardDescriptor> {

    private static final Logger log = LoggerFactory.getLogger(DownloadWizardPanel2.class);
    
    private final ChangeSupport changeSupport = new ChangeSupport(this);
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
        this.isValid = getComponent().isLoaded();
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
        ActivityLogHelper.logUserAction("DownloadWizard.storeSettings", 2);
        DownloadWizardState state = (DownloadWizardState)wiz.getProperty(DownloadWizardIterator.PROP_WIZARD_STATE);
        boolean splitChannels = getComponent().isSplitChannels();
        state.setSplitChannels(splitChannels);
        state.setOutputExtensions(getComponent().getOutputExtensions());
        // Updated serialized state
        FrameworkAccess.setLocalPreferenceValue(DownloadWizardState.class, "outputExtensions", state.getOutputExtensionString());
        FrameworkAccess.setLocalPreferenceValue(DownloadWizardState.class, "splitChannels", state.isSplitChannels());
    }

}
