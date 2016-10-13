package org.janelia.it.workstation.gui.browser.api.facade.interfaces;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.RawFileInfo;
import org.janelia.it.jacs.shared.annotation.DataDescriptor;
import org.janelia.it.jacs.shared.annotation.DataFilter;
import org.janelia.it.jacs.shared.annotation.FilterResult;

public interface LegacyFacade {

    // Tasks
    
    public Task saveOrUpdateTask(Task task) throws Exception;

    public Task getTaskById(Long taskId) throws Exception;
    
    public void deleteTaskById(Long taskId) throws Exception;
    
    public void cancelTaskById(Long taskId) throws Exception;

    public void submitJob(String processDefName, Long taskId) throws Exception;

    public void dispatchJob(String processDefName, Long taskId) throws Exception;

    public List<Task> getUserTasks() throws Exception;

    public List<Task> getUserParentTasks() throws Exception;
    
    public List<Task> getUserTasksByType(String taskName) throws Exception;
    
    // Pattern annotation
    
    public Object[] getPatternAnnotationQuantifierMapsFromSummary() throws Exception;

    public Object[] getMaskQuantifierMapsFromSummary(String maskFolderName) throws Exception;

    public List<DataDescriptor> patternSearchGetDataDescriptors(String type) throws Exception;

    public int patternSearchGetState() throws Exception;

    public List<String> patternSearchGetCompartmentList(String type) throws Exception;

    public FilterResult patternSearchGetFilteredResults(String type, Map<String, Set<DataFilter>> filterMap) throws Exception;

    // MouseLight

    public boolean isServerPathAvailable(String serverPath, boolean directoryOnly);

    public CoordinateToRawTransform getLvvCoordToRawTransform( String basePath ) throws Exception;

    public Map<Integer,byte[]> getTextureBytes( String basePath, int[] viewerCoord, int[] dimensions ) throws Exception;

    public RawFileInfo getNearestChannelFiles(String basePath, int[] viewerCoord) throws Exception;
    
}
