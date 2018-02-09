package org.janelia.it.workstation.browser.api.lifecycle;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.gui.support.WindowLocator;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.netbeans.api.autoupdate.InstallSupport;
import org.netbeans.api.autoupdate.InstallSupport.Installer;
import org.netbeans.api.autoupdate.InstallSupport.Validator;
import org.netbeans.api.autoupdate.OperationContainer;
import org.netbeans.api.autoupdate.OperationContainer.OperationInfo;
import org.netbeans.api.autoupdate.OperationException;
import org.netbeans.api.autoupdate.OperationSupport.Restarter;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.netbeans.api.autoupdate.UpdateUnitProvider;
import org.netbeans.api.autoupdate.UpdateUnitProviderFactory;
import org.netbeans.api.progress.ProgressHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Download updates automatically on startup and ask the user to restart, or install the next time 
 * the application is started.
 * 
 * Most of the update code comes from this guide: https://blogs.oracle.com/rechtacek/entry/how_to_update_netbeans_platform
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AutoUpdater extends SimpleWorker {

    private static final Logger log = LoggerFactory.getLogger(AutoUpdater.class);
    
    private OperationContainer<InstallSupport> containerForUpdate;
    private Restarter restarter;
    private boolean restarting = false;

    @Override
    protected void doStuff() throws Exception {
        ProgressHandle handle = ProgressHandle.createHandle("Checking for updates...");
        
        try {
            handle.start();
            this.containerForUpdate = getContainerForUpdate(doRealCheck(handle));

            if (containerForUpdate.getSupport()==null) {
                log.info("Found no updates to install");
                return;
            }
            
            log.info("Checking licenses");
            if (!allLicensesApproved(containerForUpdate)) {
                log.warn("Licenses are not approved. Aborting update.");
                return;
            }
            
            log.info("Downloading updates...");
            handle.progress("Downloading updates...");
            Validator validator = doDownload(containerForUpdate);
            
            log.info("Installing updates...");
            handle.progress("Installing updates...");
            this.restarter = doInstall(containerForUpdate.getSupport(), validator);
        }
        finally {
            handle.finish();
        }
    }

    @Override
    protected void hadSuccess() {
        if (containerForUpdate==null || restarter==null) return;
        InstallSupport support = containerForUpdate.getSupport();
        
        try {
            String html = "<html><body>"
                    + "<p>Updates have been downloaded are ready to install.</p>"
                    + "</body></html>";

            String[] buttons = { "Restart and Install Updates Now", "Later" };
            int selectedOption = JOptionPane.showOptionDialog(WindowLocator.getMainFrame(), html, 
                    "Updates Ready", JOptionPane.INFORMATION_MESSAGE, 0, null, buttons, buttons[0]);

            if (selectedOption == 0) {
                log.info("Restarting now to complete installation.");
                support.doRestart(restarter, null);
                this.restarting = true;
            }
            else if (selectedOption == 1) {
                log.info("Will install updates the next time the application is started.");
                support.doRestartLater(restarter);
            }
        }
        catch (Throwable ex) {
            hadError(ex);
        }
    }

    @Override
    protected void hadError(Throwable ex) {
        FrameworkImplProvider.handleException("Error running auto-update check", ex);
    }
        
    private Collection<UpdateElement> doRealCheck(ProgressHandle handle) {    
        
        List<UpdateUnitProvider> updateUnitProviders = UpdateUnitProviderFactory.getDefault().getUpdateUnitProviders(true);
        for (UpdateUnitProvider provider : updateUnitProviders) {
            try {
                // the second parameter forces update from server when true
                log.info("Checking provider '{}'", provider.getDisplayName());
                handle.progress("Checking "+ provider.getDisplayName());
                provider.refresh(handle, true);
            }
            catch (Exception ex) {
                log.error("Error refreshing " + provider.getDisplayName(), ex);
            }
        }
        
        Collection<UpdateElement> elements4update = new HashSet<>();
        List<UpdateUnit> updateUnits = UpdateManager.getDefault().getUpdateUnits();
        for (UpdateUnit unit : updateUnits) {
            handle.progress("Finding updates for "+unit.getCodeName());
            // plugin already installed?
            if (unit.getInstalled() != null) {
                // has updates ?
                if (!unit.getAvailableUpdates().isEmpty()) {
                    log.info("Found updates for: {}", unit.getCodeName());
                    // add plugin with highest version
                    elements4update.add(unit.getAvailableUpdates().get(0));
                }
                else {
                    log.debug("No updates for: {}", unit.getCodeName());
                }
            }
        }
        return elements4update;
    }

    private OperationContainer<InstallSupport> getContainerForUpdate(Collection<UpdateElement> elements4update) {
        OperationContainer<InstallSupport> container = OperationContainer.createForUpdate();
        for (UpdateElement element : elements4update) {
            log.info("Checking update element: {}", element.getDisplayName());
            
            if (container.canBeAdded(element.getUpdateUnit(), element)) {
                log.info("Adding update element: {}", element.getDisplayName());
                
                OperationInfo<InstallSupport> operationInfo = container.add(element);
                if (operationInfo == null) {
                    continue;
                }

                if (!operationInfo.getBrokenDependencies().isEmpty()) {
                    log.info("Found broken dependencies for: {}", element.getDisplayName());
                    continue;
                }
                
                log.info("Adding required elements for: {}", element.getDisplayName());
                container.add(operationInfo.getRequiredElements());
            }
        }
        
        return container;
    }

    private boolean allLicensesApproved(OperationContainer<InstallSupport> container) {
        if (!container.listInvalid().isEmpty()) {
            return false;
        }
        for (OperationInfo<InstallSupport> info : container.listAll()) {
            String license = info.getUpdateElement().getLicence();
            if (!isLicenseApproved(license)) {
                return false;
            }
        }
        return true;
    }

    private boolean isLicenseApproved(String license) {
        // TODO: check this for real, and pop-up licenses if they need approval 
        // (this seems like something the framework should do for us...)
        return true;
    }

    private Validator doDownload(OperationContainer<InstallSupport> container) throws OperationException {
        InstallSupport installSupport = container.getSupport();
        return installSupport.doDownload(null, false, true);
    }

    private Restarter doInstall(InstallSupport support, Validator validator) throws OperationException {
        // validates all plugins are correctly downloaded
        Installer installer = support.doValidate(validator, null);
        return support.doInstall(installer, null);
    }

    public boolean isRestarting() {
        return restarting;
    }
}
