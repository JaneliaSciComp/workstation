package org.janelia.it.workstation.browser.api.facade.impl.ejb;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.janelia.it.jacs.compute.api.TiledMicroscopeBeanRemote;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.UserToolEvent;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.RawFileInfo;
import org.janelia.it.jacs.shared.annotation.DataDescriptor;
import org.janelia.it.jacs.shared.annotation.DataFilter;
import org.janelia.it.jacs.shared.annotation.FilterResult;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.facade.interfaces.LegacyFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegacyFacadeImpl implements LegacyFacade {

    public static final String COMM_FAILURE_MSG_TMB = "Communication failure.";
    private static final Logger LOG = LoggerFactory.getLogger(LegacyFacadeImpl.class);
    private static final int RETRY_MAX_ATTEMPTS_RTMB = 5;
    private static final int RETRY_INTERIM_MULTIPLIER_RTMB = 500;
    
    @Override
    public Task saveOrUpdateTask(Task task) throws Exception {
        if (task == null) throw new IllegalArgumentException("Task may not be null");
        return EJBFactory.getRemoteComputeBean().saveOrUpdateTask(task);
    }

    @Override
    public Task getTaskById(Long taskId) throws Exception {
        if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        return EJBFactory.getRemoteComputeBean().getTaskById(taskId);
    }

    @Override
    public void cancelTaskById(Long taskId) throws Exception {
        if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        EJBFactory.getRemoteComputeBean().cancelTaskById(taskId);
    }

    @Override
    public void deleteTaskById(Long taskId) throws Exception {
        if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        EJBFactory.getRemoteComputeBean().deleteTaskById(taskId);
    }

    @Override
    public void submitJob(String processDefName, Long taskId) throws Exception {
        if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        EJBFactory.getRemoteComputeBean(true).submitJob(processDefName, taskId);
    }

    @Override
    public void dispatchJob(String processDefName, Long taskId) throws Exception {
        if (taskId == null) throw new IllegalArgumentException("Task id may not be null");
        EJBFactory.getRemoteComputeBean(true).dispatchJob(processDefName, taskId);
    }

    @Override
    public List<Task> getUserTasks() throws Exception {
        return EJBFactory.getRemoteComputeBean().getUserTasks(AccessManager.getSubjectKey());
    }

    @Override
    public List<Task> getUserParentTasks() throws Exception {
        return EJBFactory.getRemoteComputeBean().getRecentUserParentTasks(AccessManager.getSubjectKey());
    }
    
    @Override
    public List<Task> getUserTasksByType(String taskName) throws Exception {
        return EJBFactory.getRemoteComputeBean().getUserTasksByType(taskName, AccessManager.getSubjectKey());
    }
    
    @Override
    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getPatternAnnotationQuantifierMapsFromSummary();
    }

    @Override
    public Object[] getMaskQuantifierMapsFromSummary(String maskFolderName) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().getMaskQuantifierMapsFromSummary(maskFolderName);
    }

    @Override
    public List<DataDescriptor> patternSearchGetDataDescriptors(String type) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().patternSearchGetDataDescriptors(type);
    }

    @Override
    public int patternSearchGetState() throws Exception {
        return EJBFactory.getRemoteAnnotationBean().patternSearchGetState();
    }

    @Override
    public List<String> patternSearchGetCompartmentList(String type) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().patternSearchGetCompartmentList(type);
    }

    @Override
    public FilterResult patternSearchGetFilteredResults(String type, Map<String, Set<DataFilter>> filterMap) throws Exception {
        return EJBFactory.getRemoteAnnotationBean().patternSearchGetFilteredResults(type, filterMap);
    }
    
    @Override
    public boolean isServerPathAvailable( String serverPath, boolean directoryOnly ) {
        return EJBFactory.getRemoteComputeBean().isServerPathAvailable(serverPath, directoryOnly);
    }
    
    @Override
    public Map<Integer,byte[]> getTextureBytes( String basePath, int[] viewerCoord, int[] dimensions ) throws Exception {
        Map<Integer,byte[]> rtnVal = null;
        final TiledMicroscopeBeanRemote remoteTiledMicroscopeBean = getRemoteTMBWithRetries();
        if ( remoteTiledMicroscopeBean != null ) {
            rtnVal = remoteTiledMicroscopeBean.getTextureBytes( basePath, viewerCoord, dimensions );
        }
        return rtnVal;
    }
    
    @Override
    public RawFileInfo getNearestChannelFiles( String basePath, int[] viewerCoord ) throws Exception {
        RawFileInfo rtnVal = null;
        final TiledMicroscopeBeanRemote remoteTiledMicroscopeBean = getRemoteTMBWithRetries();
        if ( remoteTiledMicroscopeBean != null ) {
            rtnVal = remoteTiledMicroscopeBean.getNearestChannelFiles( basePath, viewerCoord );
        }
        return rtnVal;
    }

    @Override
    public CoordinateToRawTransform getLvvCoordToRawTransform( String basePath ) throws Exception {
        CoordinateToRawTransform rtnVal = null;
        final TiledMicroscopeBeanRemote remoteTiledMicroscopeBean = getRemoteTMBWithRetries();
        if ( remoteTiledMicroscopeBean != null ) {
            rtnVal = remoteTiledMicroscopeBean.getTransform(basePath);
        }
        return rtnVal;
    }
    
    public static TiledMicroscopeBeanRemote getRemoteTMBWithRetries() {
        TiledMicroscopeBeanRemote bean = null;
        for (int i = 0; i < RETRY_MAX_ATTEMPTS_RTMB; i++) {
            bean = EJBFactory.getRemoteTiledMicroscopeBean();
            if (bean != null) {
                if (i > 0) {
                    // At least one retry failed.
                    try {
                        UserToolEvent ute = new UserToolEvent();
                        ute.setAction(COMM_FAILURE_MSG_TMB);
                        ute.setCategory(TiledMicroscopeBeanRemote.class.getSimpleName());
                        ute.setToolName("EJB");
                        ute.setSessionId(0L);
                        ute.setTimestamp(new java.util.Date());
                        ute.setUserLogin("Unknown");
                        EJBFactory.getRemoteComputeBean().addEventToSessionAsync(null);
                    } catch (Exception ex) {
                        LOG.warn("Error instantiating the remote bean", ex);
                    }
                }
                break;
            }
            else {
                try {
                    Thread.sleep(RETRY_INTERIM_MULTIPLIER_RTMB * (i + 1));
                } catch (InterruptedException ie) {
                    LOG.warn("Interrupt exception", ie);
                }
            }
        };
        return bean;
    }
    
    public static class EJBLookupException extends Exception {
        public EJBLookupException(String message) {
            super(message);
        }
    }
}
