/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.framework.session_mgr;

/**
 *
 * @author fosterl
 */
import java.util.Map;
import org.janelia.it.jacs.integration.framework.session_mgr.SessionSupport;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ToolString;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = SessionSupport.class, path=SessionSupport.LOOKUP_PATH)
public class SessionMgrSessionSupport implements SessionSupport {
    @Override
    public int addExternalClient(String newClientName) {
        return SessionMgr.getSessionMgr().addExternalClient(newClientName);
    }

    @Override
    public void clearFileCache() {
        SessionMgr.getSessionMgr().clearFileCache();
    }

    @Override
    public String getApplicationName() {
        return SessionMgr.getSessionMgr().getApplicationName();
    }

    @Override
    public String getApplicationOutputDirectory() {
        return SessionMgr.getSessionMgr().getApplicationOutputDirectory();
    }

    @Override
    public String getApplicationVersion() {
        return SessionMgr.getSessionMgr().getApplicationVersion();
    }

    @Override
    public Long getCurrentSessionId() {
        return SessionMgr.getSessionMgr().getCurrentSessionId();
    }

    @Override
    public double getFileCacheGigabyteUsage() {
        return SessionMgr.getSessionMgr().getFileCacheGigabyteCapacity();
    }

    @Override
    public Object getModelProperty(Object key) {
        return SessionMgr.getSessionMgr().getModelProperty(key);
    }

    @Override
    public void handleException(Throwable throwable) {
        SessionMgr.getSessionMgr().handleException(throwable);
    }

    @Override
    public boolean isDarkLook() {
        return SessionMgr.getSessionMgr().isDarkLook();
    }

    @Override
    public boolean isFileCacheAvailable() {
        return SessionMgr.getSessionMgr().isFileCacheAvailable();
    }

    @Override
    public boolean isLoggedIn() {
        return SessionMgr.getSessionMgr().isLoggedIn();
    }

    @Override
    public boolean isUnloadImages() {
        return SessionMgr.getSessionMgr().isUnloadImages();
    }

    @Override
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action, long timestamp, double elapsedMs, double thresholdMs) {
        SessionMgr.getSessionMgr().logToolEvent(toolName, category, action, timestamp, elapsedMs, thresholdMs);
    }

    @Override
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action) {
        SessionMgr.getSessionMgr().logToolEvent(toolName, category, action);
    }

    @Override
    public void logToolEvent(ToolString toolName, CategoryString category, ActionString action, double elapsedMs, double thresholdMs) {
        SessionMgr.getSessionMgr().logToolEvent(toolName, category, action, elapsedMs, thresholdMs);
    }

    @Override
    public void logToolThresholdEvent(ToolString toolName, CategoryString category, ActionString action, long timestamp, double elapsedMs, double thresholdMs) {
        SessionMgr.getSessionMgr().logToolThresholdEvent(toolName, category, action, timestamp, elapsedMs, thresholdMs);
    }

    @Override
    public boolean loginSubject(String username, String password) {
        return SessionMgr.getSessionMgr().loginSubject(username, password);
    }

    @Override
    public void logoutUser() {
        SessionMgr.getSessionMgr().logoutUser();
    }

    @Override
    public void registerPreferenceInterface(Object interfaceKey, Class interfaceClass) throws Exception {
        SessionMgr.getSessionMgr().registerPreferenceInterface(interfaceKey, interfaceClass);
    }

    @Override
    public void removeExternalClientByPort(int targetPort) {
        SessionMgr.getSessionMgr().removeExternalClientByPort(targetPort);
    }

    @Override
    public void saveUserSettings() {
        SessionMgr.getSessionMgr().saveUserSettings();
    }

    @Override
    public void sendMessageToExternalClients(String operationName, Map<String, Object> parameters) {
        SessionMgr.getSessionMgr().sendMessageToExternalClients(operationName, parameters);
    }

    @Override
    public void setApplicationName(String name) {
        SessionMgr.getSessionMgr().setApplicationName(name);
    }

    @Override
    public void setApplicationVersion(String version) {
        SessionMgr.getSessionMgr().setApplicationVersion(version);
    }

    @Override
    public void setCurrentSessionId(Long currentSessionId) {
        SessionMgr.getSessionMgr().setCurrentSessionId(currentSessionId);
    }

    @Override
    public void setFileCacheDisabled(boolean isDisabled) {
        SessionMgr.getSessionMgr().setFileCacheDisabled(isDisabled);
    }

    @Override
    public void setFileCacheGigabyteCapacity(Integer gigabyteCapacity) {
        SessionMgr.getSessionMgr().setFileCacheGigabyteCapacity(gigabyteCapacity);
    }

    @Override
    public Object setModelProperty(Object key, Object value) {
        return SessionMgr.getSessionMgr().setModelProperty(key, value);
    }

    @Override
    public boolean setRunAsUser(String runAsUser) {
        return SessionMgr.getSessionMgr().setRunAsUser(runAsUser);
    }
    
}
