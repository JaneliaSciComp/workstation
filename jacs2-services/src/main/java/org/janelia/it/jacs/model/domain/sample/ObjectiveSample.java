package org.janelia.it.jacs.model.domain.sample;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * A set of LSMs in a Sample with a common objective.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectiveSample {

    private String objective;
    private String chanSpec;
    private List<SampleTile> tiles = new ArrayList<>();
    @JsonIgnore
    private transient Sample parent;

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getChanSpec() {
        return chanSpec;
    }

    public void setChanSpec(String chanSpec) {
        this.chanSpec = chanSpec;
    }

    public List<SampleTile> getTiles() {
        return tiles;
    }

    public void setTiles(List<SampleTile> tiles) {
        Preconditions.checkArgument(tiles != null, "The tile list for a sample objective cannot be null");
        this.tiles = tiles;
    }

    public void addTiles(SampleTile... tiles) {
        for (SampleTile t : tiles) {
            t.setParent(this);
            this.tiles.add(t);
        }
    }

    public Sample getParent() {
        return parent;
    }

    public void setParent(Sample parent) {
        this.parent = parent;
    }
}
