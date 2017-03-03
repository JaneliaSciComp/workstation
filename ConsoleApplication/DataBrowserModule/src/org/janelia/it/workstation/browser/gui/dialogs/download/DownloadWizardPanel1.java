package org.janelia.it.workstation.browser.gui.dialogs.download;

import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public class DownloadWizardPanel1 implements WizardDescriptor.Panel<WizardDescriptor> {

    /**
     * The visual component that displays this panel. If you need to access the
     * component from this class, just use getComponent().
     */
    private DownloadVisualPanel1 component;

    @Override
    public DownloadVisualPanel1 getComponent() {
        if (component == null) {
            component = new DownloadVisualPanel1();
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

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addChangeListener(ChangeListener l) {
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
    }

    @Override
    public void readSettings(WizardDescriptor wiz) {
        DownloadWizardState state = (DownloadWizardState)wiz.getProperty(DownloadWizardIterator.PROP_WIZARD_STATE);
        getComponent().init(state);
    }

    @Override
    public void storeSettings(WizardDescriptor wiz) {
        DownloadWizardState state = (DownloadWizardState)wiz.getProperty(DownloadWizardIterator.PROP_WIZARD_STATE);
        // These only need to be calculated once. We don't override settings if the user goes back in the wizard.
        if (state.getDownloadObjects()==null) {
            state.setDownloadObjects(getComponent().getDownloadObjects());
        }
        if (state.getArtifactCounts()==null) {
            state.setArtifactCounts(getComponent().getArtifactCounts());
        }
    }

}
