package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.swing.event.ChangeListener;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;

public class DownloadWizardPanel3 implements WizardDescriptor.ValidatingPanel<WizardDescriptor> {
    
    private final ChangeSupport changeSupport = new ChangeSupport(this);
    private WizardDescriptor wiz;
    
    /**
     * The visual component that displays this panel. If you need to access the
     * component from this class, just use getComponent().
     */
    private DownloadVisualPanel3 component;

    @Override
    public DownloadVisualPanel3 getComponent() {
        if (component == null) {
            component = new DownloadVisualPanel3(this);
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
        // Check if all paths are unique
        boolean pathsUnique = true;
        Set<String> uniquePaths = new HashSet<>();
        for (DownloadFileItem downloadItem : component.getDownloadItems()) {
            File targetFile = downloadItem.getTargetFile();
            if (targetFile!=null) {
                String path = targetFile.getAbsolutePath();
                if (uniquePaths.contains(path)) {
                    pathsUnique = false;
                    break;
                }
                uniquePaths.add(path);
            }
        }
        if (!pathsUnique) {
            isValid = false;
            throw new WizardValidationException(null, "Some output paths are duplicated. Try adding a unique identifier like {GUID} to your file naming template.", null);
        }
        // Check if there are any files to download
        if (uniquePaths.isEmpty()) {
            isValid = false;
            throw new WizardValidationException(null, "No files were identified for download.", null);
        }
        // Everything checks out
        isValid = true;
        wiz.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);
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
        ActivityLogHelper.logUserAction("DownloadWizard.storeSettings", 3);
        DownloadWizardState state = (DownloadWizardState) wiz.getProperty(DownloadWizardIterator.PROP_WIZARD_STATE);
        state.setFlattenStructure(getComponent().isFlattenStructure());
        state.setFilenamePattern(getComponent().getFilenamePattern());
        state.setDownloadItems(getComponent().getDownloadItems());

        // Updated serialized state
        FrameworkImplProvider.setLocalPreferenceValue(DownloadWizardState.class, "flattenStructure", state.isFlattenStructure());
        FrameworkImplProvider.setLocalPreferenceValue(DownloadWizardState.class, "filenamePattern", state.getFilenamePattern());

        String filePattern = getComponent().getFilenamePattern();
        boolean found = false;
        for (String pattern : DownloadVisualPanel3.STANDARD_FILE_PATTERNS) {
            if (pattern.equals(filePattern)) {
                found = true;
                break;
            }
        }
        if (!found) {
            FrameworkImplProvider.setModelProperty(DownloadVisualPanel3.FILE_PATTERN_PROP_NAME, filePattern);
        }
    }
}
