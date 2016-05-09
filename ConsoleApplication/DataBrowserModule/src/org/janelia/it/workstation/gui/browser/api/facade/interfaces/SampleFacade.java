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
     * Returns all of the datasets that the current user owns.
     * @return list of datasets
     */
    public Collection<DataSet> getDataSets();

    /**
     * Create a new dataset set.
     * @param dataSet the data set to create, with null GUID
     * @return the saved data set
     * @throws Exception
     */
    public DataSet create(DataSet dataSet) throws Exception;

    /**
     * Update and return the given dataset set.
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
    public Collection<LSMImage> getLsmsForSample(Long sampleId);

    public List<LineRelease> getLineReleases();

    public LineRelease createLineRelease(String name, Date releaseDate, Integer lagTimeMonths, List<String> dataSets) throws Exception;
    
    public LineRelease update(LineRelease release) throws Exception;

    public void remove(LineRelease release) throws Exception;
    
}
