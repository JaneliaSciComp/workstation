package org.janelia.jacs2.model.domain.sample;

public class TileLsmPair {
    private String tileName;
    private LSMSampleImage firstLsm;
    private LSMSampleImage secondLsm;

    public String getTileName() {
        return tileName;
    }

    public void setTileName(String tileName) {
        this.tileName = tileName;
    }

    public LSMSampleImage getFirstLsm() {
        return firstLsm;
    }

    public void setFirstLsm(LSMSampleImage firstLsm) {
        this.firstLsm = firstLsm;
    }

    public LSMSampleImage getSecondLsm() {
        return secondLsm;
    }

    public void setSecondLsm(LSMSampleImage secondLsm) {
        this.secondLsm = secondLsm;
    }
}
