package org.janelia.it.jacs.model.domain.sample;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class TileLsmPair {
    private String tileName;
    private LSMImage firstLsm;
    private LSMImage secondLsm;

    public String getTileName() {
        return tileName;
    }

    public void setTileName(String tileName) {
        this.tileName = tileName;
    }

    public LSMImage getFirstLsm() {
        return firstLsm;
    }

    public void setFirstLsm(LSMImage firstLsm) {
        this.firstLsm = firstLsm;
    }

    public LSMImage getSecondLsm() {
        return secondLsm;
    }

    public void setSecondLsm(LSMImage secondLsm) {
        this.secondLsm = secondLsm;
    }

    @JsonIgnore
    public List<LSMImage> getLsmFiles() {
        ImmutableList.Builder<LSMImage> lsmFileListBuilder = ImmutableList.<LSMImage>builder().add(firstLsm);
        if (secondLsm != null) {
            lsmFileListBuilder.add(secondLsm);
        }
        return lsmFileListBuilder.build();
    }
}
