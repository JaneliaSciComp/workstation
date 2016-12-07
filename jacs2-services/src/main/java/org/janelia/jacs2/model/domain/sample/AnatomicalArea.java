package org.janelia.jacs2.model.domain.sample;

import com.sun.javafx.scene.control.skin.VirtualFlow;

import java.util.ArrayList;
import java.util.List;

public class AnatomicalArea {

    private Number sampleId;
    private String objective;
    private String name;
    private List<TileLsmPair> tileLsmPairs = new ArrayList<>();

    public Number getSampleId() {
        return sampleId;
    }

    public void setSampleId(Number sampleId) {
        this.sampleId = sampleId;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TileLsmPair> getTileLsmPairs() {
        return tileLsmPairs;
    }

    public void setTileLsmPairs(List<TileLsmPair> tileLsmPairs) {
        this.tileLsmPairs = tileLsmPairs;
    }

    public void addLsmPair(TileLsmPair tileLsmPair) {
        tileLsmPairs.add(tileLsmPair);
    }
}
