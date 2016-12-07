package org.janelia.jacs2.model.domain.sample;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.janelia.jacs2.model.domain.DataFile;
import org.janelia.jacs2.model.domain.Reference;

import java.util.ArrayList;
import java.util.List;

/**
 * A sample tile consists of a set of LSMs with the same objective,
 * and in the same anatomical area.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleTile {

    private String name;
    private String anatomicalArea;
    private List<Reference> lsmReferences;
    private List<DataFile> dataFiles;
    @JsonIgnore
    private SampleObjective parent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnatomicalArea() {
        return anatomicalArea;
    }

    public void setAnatomicalArea(String anatomicalArea) {
        this.anatomicalArea = anatomicalArea;
    }

    public List<Reference> getLsmReferences() {
        return lsmReferences;
    }

    public void setLsmReferences(List<Reference> lsmReferences) {
        this.lsmReferences = lsmReferences;
    }

    public void addLsmReference(Reference ref) {
        if (lsmReferences == null) {
            lsmReferences = new ArrayList<>();
        }
        lsmReferences.add(ref);
    }

    public List<DataFile> getDataFiles() {
        return dataFiles;
    }

    public void setDataFiles(List<DataFile> dataFiles) {
        this.dataFiles = dataFiles;
    }

    public void addDataFile(DataFile dataFile) {
        if (dataFiles == null) {
            dataFiles = new ArrayList<>();
        }
        dataFiles.add(dataFile);
    }

    public SampleObjective getParent() {
        return parent;
    }

    public void setParent(SampleObjective parent) {
        this.parent = parent;
    }

    public Reference getLsmReferenceAt(int index) {
        return lsmReferences != null && index < lsmReferences.size() ? lsmReferences.get(index) : null;
    }
}
