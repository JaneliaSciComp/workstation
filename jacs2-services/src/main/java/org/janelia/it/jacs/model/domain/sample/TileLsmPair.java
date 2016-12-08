package org.janelia.it.jacs.model.domain.sample;

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
}
