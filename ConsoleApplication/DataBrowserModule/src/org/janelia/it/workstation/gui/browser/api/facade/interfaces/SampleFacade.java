package org.janelia.it.workstation.gui.browser.api.facade.interfaces;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LineRelease;

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
    public Collection<DataSet> getDataSets() throws Exception;

    /**
     * Create a new data set.
     * @param dataSet the data set to create, with null GUID
     * @return the saved data set
     * @throws Exception
     */
    public DataSet create(DataSet dataSet) throws Exception;

    /**
     * Update and return the given data set.
     * @param dataSet the data set to create, with null GUID
     * @return the saved data set
     * @throws Exception
     */
    public DataSet update(DataSet dataSet) throws Exception;
    
    /**
     * Remove the given domainobject.
     * @param dataSet domainobject to remove
     * @throws Exception something went wrong
     */
    public void remove(DataSet dataSet) throws Exception;
    
    /**
     * Returns all of the LSM images for a given sample. 
     * @return list of LSM images
     */
    public Collection<LSMImage> getLsmsForSample(Long sampleId) throws Exception;

    /**
     * Returns all the line releases.
     * @return
     */
    public List<LineRelease> getLineReleases() throws Exception;

    /**
     * Creates a new line release.
     * @param name name of the new release
     * @param releaseDate date of release
     * @param lagTimeMonths lag time for what samples should be included in the release (may be null)
     * @param dataSets the data sets that can be pulled from for the release
     * @return the saved line release
     * @throws Exception
     */
    public LineRelease createLineRelease(String name, Date releaseDate, Integer lagTimeMonths, List<String> dataSets) throws Exception;

    /**
     * Update and return the given line release.
     * @param release the new line release object, with GUID
     * @return the saved line release
     * @throws Exception
     */
    public LineRelease update(LineRelease release) throws Exception;

    /**
     * Remove the given line release.
     * @param release a line release object with GUID populated
     * @throws Exception
     */
    public void remove(LineRelease release) throws Exception;
    
}
