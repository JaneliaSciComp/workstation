package org.janelia.workstation.core.api.lifecycle;

import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.netbeans.api.autoupdate.*;
import org.netbeans.api.autoupdate.InstallSupport.Installer;
import org.netbeans.api.autoupdate.InstallSupport.Validator;
import org.netbeans.api.autoupdate.OperationContainer.OperationInfo;
import org.netbeans.api.autoupdate.OperationSupport.Restarter;
import org.netbeans.api.progress.ProgressHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

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
    
    private static final ResourceBundle rb = ResourceBundle.getBundle("org.janelia.workstation.core.Bundle");
    private static final String UPDATE_CENTER_KEY = "org_janelia_workstation_update_center";

    static String getUpdateCenterURL() {
        return rb.getString(AutoUpdater.UPDATE_CENTER_KEY);
    }

    private String updateCenterLabel;
    private String updateCenterUrl;
    
    private OperationContainer<InstallSupport> containerForUpdate;
    private Restarter restarter;
    private boolean restarting = false;

    protected AutoUpdater() {
        
        try {
            updateCenterLabel = rb.getString("Services/AutoupdateType/"+UPDATE_CENTER_KEY+".instance");
            log.info("Update center name: {}", updateCenterLabel);
        }
        catch (MissingResourceException e) {
            log.error("Missing update center label property in browser bundle!");
        }
        
        try {
            updateCenterUrl = getUpdateCenterURL();
            log.info("Update center URL: {}", updateCenterUrl);
        }
        catch (MissingResourceException e) {
            log.warn("Missing update center label property. Running in dev?");
        }
    }
    
    @Override
    protected void doStuff() throws Exception {

        if (SystemInfo.isDev || SystemInfo.isTest) {
            log.info("Skipping updates on non-production build");
            return;
        }

        ProgressHandle handle = ProgressHandle.createHandle("Checking for updates...");
        
        try {
            handle.start();

            upgradeToNewUpdateCenter();
            
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

    private void upgradeToNewUpdateCenter() {

        log.info("Verifying update center providers");
        
        if (StringUtils.isBlank(updateCenterLabel)) {
            log.trace("Empty update center label, aborting update center check");
            return;
        }

        if (StringUtils.isBlank(updateCenterUrl)) {
            log.trace("Empty update center URL, aborting update center check");
            return;
        }
        
        try {
            List<UpdateUnitProvider> updateUnitProviders = UpdateUnitProviderFactory.getDefault().getUpdateUnitProviders(true);
            
            if (updateUnitProviders.isEmpty()) {
                log.warn("No providers found");
                createUpdateCenter();
            }
            else {
                log.info("Verifying {} providers", updateUnitProviders.size());
                for (UpdateUnitProvider provider : updateUnitProviders) {
                    
                    if (provider == null || provider.getProviderURL() == null) {
                        continue; 
                    }
                    
                    log.info("Verifying {} (displayName={}) (url={})", provider.getName(), provider.getDisplayName(), provider.getProviderURL());
                    if (updateCenterLabel.equals(provider.getName()) || updateCenterLabel.equals(provider.getDisplayName())) {
                        
                        if (!provider.getProviderURL().toString().equals(updateCenterUrl)) {
                            provider.setProviderURL(new URL(updateCenterUrl));
                            log.warn("Updated URL for {}", provider.getName(), provider.getProviderURL());
                            break;
                        }
                        
                    }
                }
            }
        }
        catch (Exception ex) {
            log.error("Error updating to new update center", ex);
        }
    }
        
    private void createUpdateCenter() throws MalformedURLException {
        UpdateUnitProvider newProvider = UpdateUnitProviderFactory.getDefault().create(updateCenterLabel, updateCenterLabel, new URL(updateCenterUrl));
        newProvider.setEnable(true);
        log.warn("Created update center {} ({})", newProvider.getName(), newProvider.getProviderURL());
    }

    @Override
    protected void hadSuccess() {
        
        log.info("Finished");
        
        if (containerForUpdate==null || restarter==null) {
            log.info("No updates were installed");
            return;
        }
        
        InstallSupport support = containerForUpdate.getSupport();
        
        try {
            String html = "<html><body>"
                    + "<p>Updates have been downloaded are ready to install.</p>"
                    + "</body></html>";

            String[] buttons = { "Restart and Update", "Later" };
            int selectedOption = JOptionPane.showOptionDialog(FrameworkImplProvider.getMainFrame(), html,
                    "Updates Ready", JOptionPane.YES_NO_OPTION, 0, null, buttons, buttons[0]);

            if (selectedOption == 0) {
                log.info("Restarting now to complete installation.");
                support.doRestart(restarter, null);
                this.restarting = true;
            }
            else {
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
                log.info("Checking provider {}", provider.getDisplayName());
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
        
        // The following is unfortunate. The autoupdate Utilities class, which is the only way to access the user's install dir preference, 
        // is part of org.netbeans.modules.autoupdate.ui, which is not a public module API. We could use implementation version here, but it 
        // can lead to a lot of problems with auto update. For now, we'll assume global installation, and let the user do a manual upgrade if
        // things don't work.
        Boolean global = true;//Utilities.isGlobalInstallation();
        boolean userDirAsFallback = true;
        
        try {
            String installDir = SystemInfo.getInstallDir();
            log.info("Install directory: "+installDir);
            
            if (installDir.startsWith("/misc/local/workstation")) {
                log.warn("Shared Linux installation detected. Forcing global installation.");
                // if using the shared Linux installation disallow user dir upgrades so that all users stay in sync
                global = true;
                userDirAsFallback = false;
            }
        }
        catch (RuntimeException e) {
            // The above step isn't critical, so we handle any exceptions to allow install to continue. 
            FrameworkImplProvider.handleException(e);
        }
        
        log.info("Downloading updates with flags: global={}, userDirAsFallback={}", global, userDirAsFallback);
        return installSupport.doDownload(null, global, userDirAsFallback);
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
