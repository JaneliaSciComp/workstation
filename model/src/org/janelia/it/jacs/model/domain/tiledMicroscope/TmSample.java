package org.janelia.it.jacs.model.domain.tiledMicroscope;

import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchType;

/**
 * Tiled microscope sample.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@SearchType(key="tmSample",label="Tiled Microscope Sample")
@MongoMapped(collectionName="tmSample",label="Tiled Microscope Sample")
public class TmSample extends AbstractDomainObject implements HasFilepath {

    @SearchAttribute(key="filepath_txt",label="File Path")
    private String filepath;
    
    @SearchAttribute(key="micron_to_vox_txt",label="Micron to Voxel Matrix")
    private String micronToVoxMatrix;
    
    @SearchAttribute(key="vox_to_micron_txt",label="Voxel to Micron Matrix")
    private String voxToMicronMatrix;

    private List<Integer> origin;
    private List<Double> scaling;
    private Long numImageryLevels;

    public TmSample() {
    }

    public TmSample(Long id, String name) {
        setId(id);
        setName(name);
    }

    public TmSample(Long id, String name, Date creationDate, String filepath) {
        this(id, name);
        setCreationDate(creationDate);
        this.filepath = filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    @Override
    public String getFilepath() {
        return filepath;
    }

    public String getVoxToMicronMatrix() {
        return voxToMicronMatrix;
    }

    public void setVoxToMicronMatrix(String voxToMicronMatrix) {
        this.voxToMicronMatrix = voxToMicronMatrix;
    }

    public String getMicronToVoxMatrix() {
        return micronToVoxMatrix;
    }

    public void setMicronToVoxMatrix(String micronToVoxMatrix) {
        this.micronToVoxMatrix = micronToVoxMatrix;
    }


    public Long getNumImageryLevels() {
        return numImageryLevels;
    }

    public void setNumImageryLevels(Long numImageryLevels) {
        this.numImageryLevels = numImageryLevels;
    }

    public List<Double> getScaling() {
        return scaling;
    }

    public void setScaling(List<Double> scaling) {
        this.scaling = scaling;
    }

    public List<Integer> getOrigin() {
        return origin;
    }

    public void setOrigin(List<Integer> origin) {
        this.origin = origin;
    }

}
