package org.janelia.workstation.core.api.facade.interfaces;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.model.entity.json.JsonTask;
import org.janelia.model.domain.dto.SampleReprocessingRequest;
import org.janelia.model.domain.gui.cdmip.ColorDepthLibrary;
import org.janelia.model.domain.sample.DataSet;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.LineRelease;

/**
 * Implementations provide access to Samples, Data Sets, and related concepts. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SampleFacade {

    /**
     * Returns all of the datas ets that the current user owns.
     * @return list of data sets
     */
    Collection<DataSet> getDataSets() throws Exception;

    /**
     * Create a new data set.
     * @param dataSet the data set to create, with null GUID
     * @return the saved data set
     * @throws Exception
     */
    DataSet create(DataSet dataSet) throws Exception;

    /**
     * Update and return the given data set.
     * @param dataSet the data set to create, with null GUID
     * @return the saved data set
     * @throws Exception
     */
    DataSet update(DataSet dataSet) throws Exception;
    
    /**
     * Remove the given domainobject.
     * @param dataSet domainobject to remove
     * @throws Exception something went wrong
     */
    void remove(DataSet dataSet) throws Exception;
    
    /**
     * Returns all of the LSM images for a given sample. 
     * @return list of LSM images
     */
    Collection<LSMImage> getLsmsForSample(Long sampleId) throws Exception;

    /**
     * Returns all the line releases.
     * @return
     */
    List<LineRelease> getLineReleases() throws Exception;

    /**
     * Creates a new line release.
     * @param name name of the new release
     * @return the saved line release
     * @throws Exception
     */
    LineRelease createLineRelease(String name) throws Exception;

    /**
     * Update and return the given line release.
     * @param release the new line release object, with GUID
     * @return the saved line release
     * @throws Exception
     */
    LineRelease update(LineRelease release) throws Exception;

    /**
     * Remove the given line release.
     * @param release a line release object with GUID populated
     * @throws Exception
     */
    void remove(LineRelease release) throws Exception;

    /**
     * Dispatches the given samples for processing.
     * @throws Exception
     */
    String dispatchSamples(SampleReprocessingRequest request) throws Exception;
    
    /**
     * Dispatches the given task using legacy JACSv1.
     * @param task
     * @param processName
     * @return
     * @throws Exception
     */
    Long dispatchTask(JsonTask task, String processName) throws Exception;

    /**
     * Returns the list of libraries available for color depth search in a given alignment space.
     * @return
     * @throws Exception
     */
    Collection<ColorDepthLibrary> getColorDepthLibraries(String alignmentSpace) throws Exception;
}
